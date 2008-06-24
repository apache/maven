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

import org.apache.maven.MavenArtifactFilterManager;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.manager.WagonManager;
import org.apache.maven.artifact.metadata.ArtifactMetadataRetrievalException;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.metadata.ResolutionGroup;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.model.Extension;
import org.apache.maven.plugin.DefaultPluginManager;
import org.apache.maven.project.MavenProject;
import org.apache.maven.wagon.Wagon;
import org.codehaus.classworlds.ClassRealm;
import org.codehaus.classworlds.ClassWorld;
import org.codehaus.classworlds.DuplicateRealmException;
import org.codehaus.classworlds.NoSuchRealmException;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.ContextException;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarFile;

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

    private ArtifactFactory artifactFactory;

    private ArtifactMetadataSource artifactMetadataSource;

    private DefaultPlexusContainer container;

    private ArtifactFilter artifactFilter = MavenArtifactFilterManager.createExtensionFilter();

    private WagonManager wagonManager;

    private PlexusContainer extensionContainer;

    private static final String CONTAINER_NAME = "extensions";

    public void addExtension( Extension extension,
                              MavenProject project,
                              ArtifactRepository localRepository )
        throws ArtifactResolutionException, PlexusContainerException, ArtifactNotFoundException
    {
        String extensionId = ArtifactUtils.versionlessKey( extension.getGroupId(), extension.getArtifactId() );

        getLogger().debug( "Initialising extension: " + extensionId );

        Artifact artifact = (Artifact) project.getExtensionArtifactMap().get( extensionId );

        if ( artifact != null )
        {
            ArtifactFilter filter = new ProjectArtifactExceptionFilter( artifactFilter, project.getArtifact() );

            ResolutionGroup resolutionGroup;
            try
            {
                resolutionGroup = artifactMetadataSource.retrieve( artifact, localRepository,
                                                                   project.getRemoteArtifactRepositories() );
            }
            catch ( ArtifactMetadataRetrievalException e )
            {
                throw new ArtifactResolutionException( "Unable to download metadata from repository for plugin '" +
                    artifact.getId() + "': " + e.getMessage(), artifact, e );
            }

            // We use the same hack here to make sure that plexus 1.1 is available for extensions that do
            // not declare plexus-utils but need it. MNG-2900
            Set rgArtifacts = resolutionGroup.getArtifacts();
            rgArtifacts = DefaultPluginManager.checkPlexusUtils( rgArtifacts, artifactFactory );

            rgArtifacts.add( artifact );

            // Make sure that we do not influence the dependenecy resolution of extensions with the project's
            // dependencyManagement

            ArtifactResolutionResult result = artifactResolver.resolveTransitively( rgArtifacts, project.getArtifact(),
                                                                                    Collections.EMPTY_MAP,
                                                                                    //project.getManagedVersionMap(),
                                                                                    localRepository,
                                                                                    project.getRemoteArtifactRepositories(),
                                                                                    artifactMetadataSource, filter );

            // gross hack for some backwards compat (MNG-2749)
            // if it is a lone artifact, then we assume it to be a resource package, and put it in the main container
            // as before. If it has dependencies, that's when we risk conflict and exile to the child container
            // jvz: we have to make this 2 because plexus is always added now.

            Set artifacts = result.getArtifacts();

            // Lifecycles are loaded by the Lifecycle executor by looking up lifecycle definitions from the
            // core container. So we need to look if an extension has a lifecycle mapping and use the container
            // and not an extension container. (MNG-2831)

            if ( extensionContainsLifeycle( artifact.getFile() ) )
            {
                for ( Iterator i = artifacts.iterator(); i.hasNext(); )
                {
                    Artifact a = (Artifact) i.next();

                    if ( artifactFilter.include( a ) )
                    {
                        getLogger().debug( "Adding extension to core container: " + a.getFile() );

                        container.addJarResource( a.getFile() );
                    }
                }
            }
            else if ( artifacts.size() == 2 )
            {
                for ( Iterator i = artifacts.iterator(); i.hasNext(); )
                {
                    Artifact a = (Artifact) i.next();

                    if ( !a.getArtifactId().equals( "plexus-utils" ) )
                    {
                        a = project.replaceWithActiveArtifact( a );

                        getLogger().debug( "Adding extension to core container: " + a.getFile() );

                        container.addJarResource( a.getFile() );
                    }
                }
            }
            else
            {
                // create a child container for the extension
                // TODO: this could surely be simpler/different on trunk with the new classworlds

                if ( extensionContainer == null )
                {
                    extensionContainer = createContainer();
                }

                for ( Iterator i = result.getArtifacts().iterator(); i.hasNext(); )
                {
                    Artifact a = (Artifact) i.next();

                    a = project.replaceWithActiveArtifact( a );

                    getLogger().debug( "Adding to extension classpath: " + a.getFile() );

                    extensionContainer.addJarResource( a.getFile() );
                }

                if ( getLogger().isDebugEnabled() )
                {
                    extensionContainer.getContainerRealm().display();
                }
            }
        }
    }

    private PlexusContainer createContainer()
        throws PlexusContainerException
    {
        DefaultPlexusContainer child = new DefaultPlexusContainer();

        ClassWorld classWorld = container.getClassWorld();
        child.setClassWorld( classWorld );

        ClassRealm childRealm = null;

        // note: ideally extensions would live in their own realm, but this would mean that things like wagon-scm would
        // have no way to obtain SCM extensions
        String childRealmId = "plexus.core.child-container[" + CONTAINER_NAME + "]";
        try
        {
            childRealm = classWorld.getRealm( childRealmId );
        }
        catch ( NoSuchRealmException e )
        {
            try
            {
                childRealm = classWorld.newRealm( childRealmId );
            }
            catch ( DuplicateRealmException impossibleError )
            {
                getLogger().error( "An impossible error has occurred. After getRealm() failed, newRealm() " +
                    "produced duplication error on same id!", impossibleError );
            }
        }

        childRealm.setParent( container.getContainerRealm() );

        child.setCoreRealm( childRealm );

        child.setName( CONTAINER_NAME );

        // This is what we are skipping - we use the parent realm, but not the parent container since otherwise
        // we won't reload component descriptors that already exist in there
//        child.setParentPlexusContainer( this );

        // ----------------------------------------------------------------------
        // Set all the child elements from the parent that were set
        // programmatically.
        // ----------------------------------------------------------------------

        child.setLoggerManager( container.getLoggerManager() );

        child.initialize();

        child.start();

        return child;
    }

    public void registerWagons()
    {
        if ( extensionContainer != null )
        {
            try
            {
                Map wagons = extensionContainer.lookupMap( Wagon.ROLE );
                wagonManager.registerWagons( wagons.keySet(), extensionContainer );
            }
            catch ( ComponentLookupException e )
            {
                // no wagons found in the extension
            }
        }
    }

    public void contextualize( Context context )
        throws ContextException
    {
        container = (DefaultPlexusContainer) context.get( PlexusConstants.PLEXUS_KEY );
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

    private boolean extensionContainsLifeycle( File extension )
    {
        JarFile f;

        try
        {
            f = new JarFile( extension );

            InputStream is = f.getInputStream( f.getEntry( "META-INF/plexus/components.xml" ) );

            if ( is == null )
            {
                return false;
            }

            Xpp3Dom dom = Xpp3DomBuilder.build( new InputStreamReader( is ) );

            Xpp3Dom[] components = dom.getChild( "components" ).getChildren( "component" );

            for ( int i = 0; i < components.length; i++ )
            {
                if ( components[i].getChild( "role" ).getValue().equals( "org.apache.maven.lifecycle.mapping.LifecycleMapping" ) )
                {
                    return true;
                }
            }
        }
        catch( Exception e )
        {
            // do nothing
        }

        return false;
    }
}
