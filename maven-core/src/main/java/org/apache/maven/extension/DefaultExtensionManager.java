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
import org.apache.maven.plugin.DefaultPluginManager;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.manager.WagonManager;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.metadata.ResolutionGroup;
import org.apache.maven.artifact.metadata.ArtifactMetadataRetrievalException;
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
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.ContextException;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

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
        if ( ( groupId == null ) && ( originatingParent != null ) )
        {
            groupId = originatingParent.getGroupId();
        }

        String artifactId = originatingModel.getArtifactId();

        String version = originatingModel.getVersion();
        if ( ( version == null ) && ( originatingParent != null ) )
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

        addExtension( artifact, project.getArtifact(), project.getRemoteArtifactRepositories(),
                      localRepository, new ActiveArtifactResolver( project ) );
    }

    private void addExtension( Artifact extensionArtifact,
                               Artifact projectArtifact,
                               List remoteRepositories,
                               ArtifactRepository localRepository, ActiveArtifactResolver activeArtifactResolver )
        throws ArtifactResolutionException, PlexusContainerException, ArtifactNotFoundException
    {
        getLogger().debug( "Starting extension-addition process for: " + extensionArtifact );

        if ( extensionArtifact != null )
        {
            ArtifactFilter filter =
                new ProjectArtifactExceptionFilter( artifactFilterManager.getArtifactFilter(), projectArtifact );


            ResolutionGroup resolutionGroup;

            try
            {
                resolutionGroup = artifactMetadataSource.retrieve( extensionArtifact, localRepository, remoteRepositories );
            }
            catch ( ArtifactMetadataRetrievalException e )
            {
                throw new ArtifactResolutionException( "Unable to download metadata from repository for plugin '" +
                    extensionArtifact.getId() + "': " + e.getMessage(), extensionArtifact, e );
            }

            // We use the same hack here to make sure that plexus 1.1 is available for extensions that do
            // not declare plexus-utils but need it. MNG-2900
            DefaultPluginManager.checkPlexusUtils( resolutionGroup, artifactFactory );

            Set dependencies = new HashSet( resolutionGroup.getArtifacts() );

            dependencies.add( extensionArtifact );

            // TODO: Make this work with managed dependencies, or an analogous management section in the POM.
            ArtifactResolutionResult result =
                artifactResolver.resolveTransitively( dependencies, projectArtifact,
                                                      Collections.EMPTY_MAP, localRepository, remoteRepositories,
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
        wagonManager.findAndRegisterWagons( container );
    }

    public void contextualize( Context context )
        throws ContextException
    {
        container = (PlexusContainer) context.get( PlexusConstants.PLEXUS_KEY );
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
            projectDependencyConflictId = projectArtifact.getDependencyConflictId();
        }

        public boolean include( Artifact artifact )
        {
            String depConflictId = artifact.getDependencyConflictId();

            return projectDependencyConflictId.equals( depConflictId ) || passThroughFilter.include( artifact );
        }
    }

    public static void checkPlexusUtils( ResolutionGroup resolutionGroup, ArtifactFactory artifactFactory )
    {
        // ----------------------------------------------------------------------------
        // If the plugin already declares a dependency on plexus-utils then we're all
        // set as the plugin author is aware of its use. If we don't have a dependency
        // on plexus-utils then we must protect users from stupid plugin authors who
        // did not declare a direct dependency on plexus-utils because the version
        // Maven uses is hidden from downstream use. We will also bump up any
        // anything below 1.1 to 1.1 as this mimics the behaviour in 2.0.5 where
        // plexus-utils 1.1 was being forced into use.
        // ----------------------------------------------------------------------------

        VersionRange vr = null;

        try
        {
            vr = VersionRange.createFromVersionSpec( "[1.1,)" );
        }
        catch ( InvalidVersionSpecificationException e )
        {
            // Won't happen
        }

        boolean plexusUtilsPresent = false;

        for ( Iterator i = resolutionGroup.getArtifacts().iterator(); i.hasNext(); )
        {
            Artifact a = (Artifact) i.next();

            if ( a.getArtifactId().equals( "plexus-utils" ) &&
                vr.containsVersion( new DefaultArtifactVersion( a.getVersion() ) ) )
            {
                plexusUtilsPresent = true;

                break;
            }
        }

        if ( !plexusUtilsPresent )
        {
            // We will add plexus-utils as every plugin was getting this anyway from Maven itself. We will set the
            // version to the latest version we know that works as of the 2.0.6 release. We set the scope to runtime
            // as this is what's implicitly happening in 2.0.6.

            resolutionGroup.getArtifacts().add( artifactFactory.createArtifact( "org.codehaus.plexus",
                                                                                "plexus-utils", "1.1",
                                                                                Artifact.SCOPE_RUNTIME, "jar" ) );
        }
    }    
}
