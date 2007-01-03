package org.apache.maven.extension;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.MavenArtifactFilterManager;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.artifact.manager.WagonManager;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.model.Extension;
import org.apache.maven.project.MavenProject;
import org.apache.maven.wagon.Wagon;
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
import java.util.Map;

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
    private ArtifactResolver artifactResolver;

    private ArtifactMetadataSource artifactMetadataSource;

    private PlexusContainer container;

    private ArtifactFilter artifactFilter = MavenArtifactFilterManager.createStandardFilter();

    private WagonManager wagonManager;

    private static final String CONTAINER_NAME = "extensions";

    public void addExtension( Extension extension, MavenProject project, ArtifactRepository localRepository )
        throws ArtifactResolutionException, PlexusContainerException, ArtifactNotFoundException
    {
        String extensionId = ArtifactUtils.versionlessKey( extension.getGroupId(), extension.getArtifactId() );

        getLogger().debug( "Initialising extension: " + extensionId );

        Artifact artifact = (Artifact) project.getExtensionArtifactMap().get( extensionId );

        if ( artifact != null )
        {
            ArtifactFilter filter = new ProjectArtifactExceptionFilter( artifactFilter, project.getArtifact() );

            ArtifactResolutionResult result = artifactResolver.resolveTransitively( Collections.singleton( artifact ),
                                                                                    project.getArtifact(),
                                                                                    localRepository,
                                                                                    project.getRemoteArtifactRepositories(),
                                                                                    artifactMetadataSource, filter );

            // create a child container for the extension
            // TODO: this could surely be simpler/different on trunk with the new classworlds
            PlexusContainer extensionContainer = getExtensionContainer();
            if ( extensionContainer == null )
            {
                extensionContainer =
                    container.createChildContainer( CONTAINER_NAME, Collections.EMPTY_LIST, Collections.EMPTY_MAP );
            }

            for ( Iterator i = result.getArtifacts().iterator(); i.hasNext(); )
            {
                Artifact a = (Artifact) i.next();

                a = project.replaceWithActiveArtifact( a );

                getLogger().debug( "Adding to extension classpath: " + a.getFile() );

                extensionContainer.addJarResource( a.getFile() );
            }
        }
    }

    public void registerWagons()
    {
        PlexusContainer extensionContainer = getExtensionContainer();
        if ( extensionContainer != null )
        {
            try
            {
                Map wagons = extensionContainer.lookupMap( Wagon.ROLE );
                wagonManager.registerWagons( wagons.keySet(), extensionContainer );
            }
            catch ( ComponentLookupException e )
            {
                // now wagons found in the extension
            }
        }
    }

    private PlexusContainer getExtensionContainer()
    {
        // note: ideally extensions would live in their own realm, but this would mean that things like wagon-scm would
        // have no way to obtain SCM extensions
        return container.getChildContainer( CONTAINER_NAME );
    }

    public void contextualize( Context context )
        throws ContextException
    {
        this.container = (PlexusContainer) context.get( PlexusConstants.PLEXUS_KEY );
    }

    private static final class ProjectArtifactExceptionFilter
        implements ArtifactFilter
    {
        private ArtifactFilter passThroughFilter;

        private String projectDependencyConflictId;

        ProjectArtifactExceptionFilter( ArtifactFilter passThroughFilter, Artifact projectArtifact )
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
