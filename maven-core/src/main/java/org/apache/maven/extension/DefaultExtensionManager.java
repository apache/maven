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
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.execution.MavenProjectSession;
import org.apache.maven.model.Extension;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.plugin.DefaultPluginManager;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.MutablePlexusContainer;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.classworlds.realm.DuplicateRealmException;
import org.codehaus.plexus.classworlds.realm.NoSuchRealmException;
import org.codehaus.plexus.component.discovery.ComponentDiscoverer;
import org.codehaus.plexus.component.discovery.ComponentDiscovererManager;
import org.codehaus.plexus.component.discovery.ComponentDiscoveryEvent;
import org.codehaus.plexus.component.discovery.ComponentDiscoveryListener;
import org.codehaus.plexus.component.discovery.DefaultComponentDiscoverer;
import org.codehaus.plexus.component.repository.ComponentDescriptor;
import org.codehaus.plexus.component.repository.ComponentSetDescriptor;
import org.codehaus.plexus.component.repository.exception.ComponentRepositoryException;
import org.codehaus.plexus.configuration.PlexusConfigurationException;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.ContextException;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;

import java.net.MalformedURLException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    private MutablePlexusContainer container;

    private ArtifactFilterManager artifactFilterManager;

    private WagonManager wagonManager;

    public void addExtension( Extension extension,
                              Model originatingModel,
                              List remoteRepositories,
                              ArtifactRepository localRepository, Map projectSessions )
        throws ExtensionManagerException
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

        addExtension( extensionArtifact,
                      projectArtifact,
                      remoteRepositories,
                      localRepository,
                      null,
                      projectSessions,
                      MavenProjectSession.createProjectId( groupId, artifactId, version ) );
    }

    public void addExtension( Extension extension,
                              MavenProject project,
                              ArtifactRepository localRepository, Map projectSessions )
        throws ExtensionManagerException
    {
        String extensionId = ArtifactUtils.versionlessKey( extension.getGroupId(), extension.getArtifactId() );

        getLogger().debug( "Initialising extension: " + extensionId );

        Artifact artifact = (Artifact) project.getExtensionArtifactMap().get( extensionId );

        addExtension( artifact,
                      project.getArtifact(),
                      project.getRemoteArtifactRepositories(),
                      localRepository,
                      new ActiveArtifactResolver( project ),
                      projectSessions,
                      MavenProjectSession.createProjectId( project.getGroupId(), project.getArtifactId(), project.getVersion() )  );
    }

    private void addExtension( Artifact extensionArtifact,
                               Artifact projectArtifact,
                               List remoteRepositories,
                               ArtifactRepository localRepository,
                               ActiveArtifactResolver activeArtifactResolver,
                               Map projectSessions,
                               String projectId )
        throws ExtensionManagerException
    {
        getLogger().debug( "Starting extension-addition process for: " + extensionArtifact );

        MavenProjectSession projectSession = (MavenProjectSession) projectSessions.get( projectId );
        if ( projectSession == null )
        {
            try
            {
                projectSession = new MavenProjectSession( projectId, container );
            }
            catch ( PlexusContainerException e )
            {
                throw new ExtensionManagerException( "Failed to create project realm for: " + projectId, projectId, e );
            }

            projectSessions.put( projectId, projectSession );
        }

        // if the extension is null, or if it's already been added to the current project-session, skip it.
        if ( ( extensionArtifact != null ) && !projectSession.containsRealm( extensionArtifact ) )
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
                throw new ExtensionManagerException( "Unable to download metadata from repository for extension artifact '" +
                    extensionArtifact.getId() + "': " + e.getMessage(), extensionArtifact, projectId, e );
            }

            // We use the same hack here to make sure that plexus 1.1 is available for extensions that do
            // not declare plexus-utils but need it. MNG-2900
            DefaultPluginManager.checkPlexusUtils( resolutionGroup, artifactFactory );

            Set dependencies = new HashSet( resolutionGroup.getArtifacts() );

            dependencies.add( extensionArtifact );

            // TODO: Make this work with managed dependencies, or an analogous management section in the POM.
            ArtifactResolutionResult result;
            try
            {
                result = artifactResolver.resolveTransitively( dependencies, projectArtifact,
                                                      Collections.EMPTY_MAP, localRepository, remoteRepositories,
                                                      artifactMetadataSource, filter );
            }
            catch ( ArtifactResolutionException e )
            {
                throw new ExtensionManagerException( "Unable to resolve one or more extension artifacts for: " + extensionArtifact.getId(), extensionArtifact, projectId, e );
            }
            catch ( ArtifactNotFoundException e )
            {
                throw new ExtensionManagerException( "One or more extension artifacts is missing for: " + extensionArtifact.getId(), extensionArtifact, projectId, e );
            }

            ClassRealm extensionRealm;
            try
            {
                extensionRealm = projectSession.createExtensionRealm( extensionArtifact );
            }
            catch ( DuplicateRealmException e )
            {
                throw new ExtensionManagerException( "Unable to create extension ClassRealm for extension: " + extensionArtifact.getId() + " within session for project: " + projectId, extensionArtifact, projectId, e );
            }

            projectSession.addComponentRealm( extensionRealm );

            for ( Iterator i = result.getArtifacts().iterator(); i.hasNext(); )
            {
                Artifact a = (Artifact) i.next();

                if ( activeArtifactResolver != null )
                {
                    a = activeArtifactResolver.replaceWithActiveArtifact( a );
                }

                getLogger().debug( "Adding to extension classpath: " + a.getFile() + " in classRealm: " + extensionRealm.getId() );

                try
                {
                    extensionRealm.addURL( a.getFile().toURL() );
                }
                catch ( MalformedURLException e )
                {
                    throw new ExtensionManagerException( "Unable to generate URL from extension artifact file: " + a.getFile(), extensionArtifact, projectId, e );
                }
            }

            ComponentDiscoverer discoverer = new DefaultComponentDiscoverer();
            discoverer.setManager( new DummyDiscovererManager() );

            ClassRealm projectRealm = projectSession.getProjectRealm();
            try
            {
                List componentSetDescriptors = discoverer.findComponents( container.getContext(), extensionRealm );
                for ( Iterator it = componentSetDescriptors.iterator(); it.hasNext(); )
                {
                    ComponentSetDescriptor compSet = (ComponentSetDescriptor) it.next();
                    for ( Iterator compIt = compSet.getComponents().iterator(); compIt.hasNext(); )
                    {
                        ComponentDescriptor comp = (ComponentDescriptor) compIt.next();
                        String implementation = comp.getImplementation();

                        try
                        {
                            getLogger().debug( "Importing: " + implementation + " from extension realm: " + extensionRealm.getId() + " to project realm: " + projectRealm.getId() );

                            projectRealm.importFrom( extensionRealm.getId(), implementation );

                            comp.setRealmId( projectRealm.getId() );
                            container.addComponentDescriptor( comp );
                        }
                        catch ( NoSuchRealmException e )
                        {
                            throw new ExtensionManagerException( "Failed to create import for component: " + implementation + " from extension realm: " + extensionRealm.getId() + " to project realm: " + projectRealm.getId(), extensionArtifact, projectId, e );
                        }
                    }
                }
            }
            catch ( PlexusConfigurationException e )
            {
                throw new ExtensionManagerException( "Unable to discover extension components.", extensionArtifact, projectId, e );
            }
            catch ( ComponentRepositoryException e )
            {
                throw new ExtensionManagerException( "Unable to discover extension components from imports added to project-session realm.", extensionArtifact, projectId, e );
            }
        }
    }

    public void registerWagons( Map projectSessions )
    {
        for ( Iterator it = projectSessions.values().iterator(); it.hasNext(); )
        {
            MavenProjectSession projectSession = (MavenProjectSession) it.next();

            ClassRealm oldRealm = container.setLookupRealm( projectSession.getProjectRealm() );

            wagonManager.findAndRegisterWagons( container );

            container.setLookupRealm( oldRealm );
        }
    }

    public void contextualize( Context context )
        throws ContextException
    {
        container = (MutablePlexusContainer) context.get( PlexusConstants.PLEXUS_KEY );
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

    private static final class DummyDiscovererManager implements ComponentDiscovererManager
    {

        public void fireComponentDiscoveryEvent( ComponentDiscoveryEvent arg0 )
        {
        }

        public List getComponentDiscoverers()
        {
            return null;
        }

        public Map getComponentDiscoveryListeners()
        {
            return null;
        }

        public List getListeners()
        {
            return null;
        }

        public void initialize()
        {
        }

        public void registerComponentDiscoveryListener( ComponentDiscoveryListener arg0 )
        {
        }

        public void removeComponentDiscoveryListener( ComponentDiscoveryListener arg0 )
        {
        }

    }
}
