package org.apache.maven.extension;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.ArtifactFilterManager;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.model.Extension;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.project.MavenProject;
import org.apache.maven.wagon.manager.WagonManager;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.ContextException;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Used to locate extensions.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @author Jason van Zyl
 * @version $Id$
 */
public class DefaultExtensionManager
    extends AbstractLogEnabled
    implements ExtensionManager, Contextualizable
{

    private ArtifactFactory artifactFactory;

    private ArtifactResolver artifactResolver;

    private ArtifactMetadataSource artifactMetadataSource;

    private PlexusContainer container;

    private ArtifactFilterManager artifactFilterManager;
    
    private WagonManager wagonManager;

    public void addExtension( Extension extension,
                              Model originatingModel,
                              List remoteRepositories,
                              ArtifactRepository localRepository )
        throws ArtifactResolutionException, PlexusContainerException, ArtifactNotFoundException
    {
        Artifact extensionArtifact = artifactFactory.createBuildArtifact( extension.getGroupId(),
                                                                          extension.getArtifactId(),
                                                                          extension.getVersion(), "jar" );

        Parent originatingParent = originatingModel.getParent();

        String groupId = originatingModel.getGroupId();
        if ( groupId == null && originatingParent != null )
        {
            groupId = originatingParent.getGroupId();
        }

        String artifactId = originatingModel.getArtifactId();

        String version = originatingModel.getVersion();
        if ( version == null && originatingParent != null )
        {
            version = originatingParent.getVersion();
        }

        Artifact projectArtifact = artifactFactory.createProjectArtifact( groupId, artifactId, version );

        addExtension( extensionArtifact, projectArtifact, remoteRepositories, localRepository, null );
    }

    public void addExtension( Extension extension,
                              MavenProject project,
                              ArtifactRepository localRepository )
        throws ArtifactResolutionException, PlexusContainerException, ArtifactNotFoundException
    {
        String extensionId = ArtifactUtils.versionlessKey( extension.getGroupId(), extension.getArtifactId() );

        getLogger().debug( "Initialising extension: " + extensionId );

        Artifact artifact = (Artifact) project.getExtensionArtifactMap().get( extensionId );

        addExtension( artifact, project.getArtifact(), project.getRemoteArtifactRepositories(), localRepository,
                      new ActiveArtifactResolver( project ) );
    }

    private void addExtension( Artifact extensionArtifact,
                               Artifact projectArtifact,
                               List remoteRepositories,
                               ArtifactRepository localRepository,
                               ActiveArtifactResolver activeArtifactResolver )
        throws ArtifactResolutionException, PlexusContainerException, ArtifactNotFoundException
    {
        getLogger().debug( "Starting extension-addition process for: " + extensionArtifact );
        
        if ( extensionArtifact != null )
        {
            ArtifactFilter filter =
                new ProjectArtifactExceptionFilter( artifactFilterManager.getArtifactFilter(), projectArtifact );

            ArtifactResolutionResult result = artifactResolver.resolveTransitively(
                Collections.singleton( extensionArtifact ), projectArtifact, localRepository, remoteRepositories,
                artifactMetadataSource, filter );

            for ( Iterator i = result.getArtifacts().iterator(); i.hasNext(); )
            {
                Artifact a = (Artifact) i.next();

                if ( activeArtifactResolver != null )
                {
                    a = activeArtifactResolver.replaceWithActiveArtifact( a );
                }

                getLogger().debug( "Adding to extension classpath: " + a.getFile() + " in classRealm: " + container.getContainerRealm().getId() );

                container.addJarResource( a.getFile() );
                
                artifactFilterManager.excludeArtifact( a.getArtifactId() );
            }
        }
    }

    public void registerWagons()
    {
        try
        {
            wagonManager.registerExtensionContainer( container );
        }
        catch ( ComponentLookupException e )
        {
            // no wagons found in the extension
        }
    }

    public void contextualize( Context context )
        throws ContextException
    {
        this.container = (PlexusContainer) context.get( PlexusConstants.PLEXUS_KEY );
    }

    private static final class ActiveArtifactResolver
    {
        private MavenProject project;

        ActiveArtifactResolver( MavenProject project )
        {
            this.project = project;
        }

        Artifact replaceWithActiveArtifact( Artifact artifact )
        {
            return project.replaceWithActiveArtifact( artifact );
        }
    }

    private static final class ProjectArtifactExceptionFilter
        implements ArtifactFilter
    {
        private ArtifactFilter passThroughFilter;

        private String projectDependencyConflictId;

        ProjectArtifactExceptionFilter( ArtifactFilter passThroughFilter,
                                        Artifact projectArtifact )
        {
            this.passThroughFilter = passThroughFilter;
            this.projectDependencyConflictId = projectArtifact.getDependencyConflictId();
        }

        public boolean include( Artifact artifact )
        {
            String depConflictId = artifact.getDependencyConflictId();

            return projectDependencyConflictId.equals( depConflictId ) || passThroughFilter.include( artifact );
        }
    }

}
