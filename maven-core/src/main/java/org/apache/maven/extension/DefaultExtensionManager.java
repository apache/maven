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
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Extension;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Plugin;
import org.apache.maven.monitor.event.DefaultEventDispatcher;
import org.apache.maven.monitor.event.EventDispatcher;
import org.apache.maven.plugin.InvalidPluginException;
import org.apache.maven.plugin.PluginManager;
import org.apache.maven.plugin.PluginManagerException;
import org.apache.maven.plugin.PluginNotFoundException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.version.PluginVersionNotFoundException;
import org.apache.maven.plugin.version.PluginVersionResolutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.realm.MavenRealmManager;
import org.apache.maven.realm.RealmManagementException;
import org.apache.maven.realm.RealmUtils;
import org.codehaus.plexus.MutablePlexusContainer;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.ContextException;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
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

    private PluginManager pluginManager;

    // used for unit testing.
    protected DefaultExtensionManager( ArtifactFactory artifactFactory,
                                    ArtifactResolver artifactResolver,
                                    ArtifactMetadataSource artifactMetadataSource,
                                    MutablePlexusContainer container,
                                    ArtifactFilterManager artifactFilterManager,
                                    WagonManager wagonManager )
    {
        this.artifactFactory = artifactFactory;
        this.artifactResolver = artifactResolver;
        this.artifactMetadataSource = artifactMetadataSource;
        this.container = container;
        this.artifactFilterManager = artifactFilterManager;
        this.wagonManager = wagonManager;
    }

    public DefaultExtensionManager()
    {
        // used for plexus init.
    }

    public void addExtension( Extension extension,
                              Model originatingModel,
                              List remoteRepositories,
                              MavenExecutionRequest request )
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
                      request,
                      null,
                      groupId,
                      artifactId,
                      version );
    }

    public void addPluginAsExtension( Plugin plugin,
                              Model originatingModel,
                              List remoteRepositories,
                              MavenExecutionRequest request )
        throws ExtensionManagerException
    {
        getLogger().debug( "Adding plugin: " + plugin.getKey() + " as an extension." );

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

        String pluginVersion = plugin.getVersion();

        // TODO: Forbid this?
        if ( pluginVersion == null )
        {
            pluginVersion = Artifact.RELEASE_VERSION;
        }

        Artifact pluginArtifact = artifactFactory.createBuildArtifact( plugin.getGroupId(),
                                                                       plugin.getArtifactId(),
                                                                       pluginVersion, "maven-plugin" );

        getLogger().debug( "Starting extension-addition process for: " + pluginArtifact );

        ArtifactFilter coreFilter = artifactFilterManager.getArtifactFilter();
        MavenRealmManager realmManager = request.getRealmManager();

        // if the extension is null,
        // or if it's excluded by the core filter,
        //
        // skip it.
        if ( ( pluginArtifact != null )
             && coreFilter.include( pluginArtifact ) )
        {
            MavenProject dummyProject = new MavenProject( originatingModel );

            dummyProject.setRemoteArtifactRepositories( remoteRepositories );

            EventDispatcher dispatcher = new DefaultEventDispatcher( request.getEventMonitors() );
            MavenSession session = new MavenSession( container, request, dispatcher, null );

            PluginDescriptor pd;
            try
            {
                pd = pluginManager.verifyPlugin( plugin, dummyProject, session );
                pluginArtifact = pd.getPluginArtifact();
            }
            catch ( ArtifactResolutionException e )
            {
                throw new ExtensionManagerException( "Failed to resolve extension plugin: " + pluginArtifact, pluginArtifact, groupId, artifactId, version, e );
            }
            catch ( ArtifactNotFoundException e )
            {
                throw new ExtensionManagerException( "Failed to resolve extension plugin: " + pluginArtifact, pluginArtifact, groupId, artifactId, version, e );
            }
            catch ( PluginNotFoundException e )
            {
                throw new ExtensionManagerException( "Failed to resolve extension plugin: " + pluginArtifact, pluginArtifact, groupId, artifactId, version, e );
            }
            catch ( PluginVersionResolutionException e )
            {
                throw new ExtensionManagerException( "Failed to resolve extension plugin: " + pluginArtifact, pluginArtifact, groupId, artifactId, version, e );
            }
            catch ( InvalidPluginException e )
            {
                throw new ExtensionManagerException( "Failed to resolve extension plugin: " + pluginArtifact, pluginArtifact, groupId, artifactId, version, e );
            }
            catch ( PluginManagerException e )
            {
                throw new ExtensionManagerException( "Failed to resolve extension plugin: " + pluginArtifact, pluginArtifact, groupId, artifactId, version, e );
            }
            catch ( PluginVersionNotFoundException e )
            {
                throw new ExtensionManagerException( "Failed to resolve extension plugin: " + pluginArtifact, pluginArtifact, groupId, artifactId, version, e );
            }

            if ( !realmManager.hasExtensionRealm( pluginArtifact ) )
            {
                try
                {
                    realmManager.createExtensionRealm( pluginArtifact, pd.getArtifacts() );
                }
                catch ( RealmManagementException e )
                {
                    String projectId = RealmUtils.createProjectId( groupId, artifactId, version );
                    throw new ExtensionManagerException( "Unable to create extension ClassRealm for extension: " + pluginArtifact.getId() + " within session for project: " + projectId, pluginArtifact, groupId, artifactId, version, e );
                }
            }

            try
            {
                realmManager.importExtensionsIntoProjectRealm( groupId, artifactId, version, pluginArtifact );
            }
            catch ( RealmManagementException e )
            {
                throw new ExtensionManagerException( "Unable to import extension components into project realm.", pluginArtifact, groupId, artifactId, version, e );
            }
        }
    }

    public void addExtension( Extension extension,
                              MavenProject project,
                              MavenExecutionRequest request )
        throws ExtensionManagerException
    {
        String extensionId = ArtifactUtils.versionlessKey( extension.getGroupId(), extension.getArtifactId() );

        getLogger().debug( "Initialising extension: " + extensionId );

        Artifact artifact = (Artifact) project.getExtensionArtifactMap().get( extensionId );

        addExtension( artifact,
                      project.getArtifact(),
                      project.getRemoteArtifactRepositories(),
                      request,
                      new ActiveArtifactResolver( project ),
                      project.getGroupId(),
                      project.getArtifactId(),
                      project.getVersion() );
    }

    private void addExtension( Artifact extensionArtifact,
                               Artifact projectArtifact,
                               List<ArtifactRepository> remoteRepositories,
                               MavenExecutionRequest request,
                               ActiveArtifactResolver activeArtifactResolver,
                               String projectGroupId,
                               String projectArtifactId,
                               String projectVersion )
        throws ExtensionManagerException
    {
        getLogger().debug( "Starting extension-addition process for: " + extensionArtifact );

        ArtifactFilter coreFilter = artifactFilterManager.getArtifactFilter();
        MavenRealmManager realmManager = request.getRealmManager();

        // if the extension is null,
        // or if it's excluded by the core filter,
        //
        // skip it.
        if ( ( extensionArtifact != null )
             && coreFilter.include( extensionArtifact ) )
        {
            ArtifactFilter filter =
                new ProjectArtifactExceptionFilter( coreFilter, projectArtifact );

            ResolutionGroup resolutionGroup;

            ArtifactRepository localRepository = request.getLocalRepository();

            try
            {
                resolutionGroup = artifactMetadataSource.retrieve( extensionArtifact, localRepository, remoteRepositories );
            }
            catch ( ArtifactMetadataRetrievalException e )
            {
                throw new ExtensionManagerException( "Unable to download metadata from repository for extension artifact '" +
                    extensionArtifact.getId() + "': " + e.getMessage(), extensionArtifact, projectGroupId, projectArtifactId, projectVersion, e );
            }

            Set<Artifact> dependencies = new LinkedHashSet<Artifact>();

            dependencies.add( extensionArtifact );
            dependencies.addAll( resolutionGroup.getArtifacts() );

            ArtifactResolutionRequest dependencyReq = new ArtifactResolutionRequest().setArtifact( projectArtifact )
                                                                           .setArtifactDependencies( dependencies )
                                                                           .setFilter( filter )
                                                                           .setLocalRepository( localRepository )
                                                                           .setRemoteRepostories( remoteRepositories )
                                                                           .setMetadataSource( artifactMetadataSource );

            // TODO: Make this work with managed dependencies, or an analogous management section in the POM.
            ArtifactResolutionResult result = artifactResolver.resolve( dependencyReq );

            if ( result.hasCircularDependencyExceptions() || result.hasErrorArtifactExceptions()
                 || result.hasMetadataResolutionExceptions() || result.hasVersionRangeViolations() )
            {
                throw new ExtensionManagerException( "Failed to resolve extension: " + extensionArtifact, extensionArtifact, projectGroupId, projectArtifactId, projectVersion, result );
            }

            Set resultArtifacts = new LinkedHashSet();
            for ( Iterator iterator = result.getArtifacts().iterator(); iterator.hasNext(); )
            {
                Artifact a = (Artifact) iterator.next();


                if ( activeArtifactResolver != null )
                {
                    a = activeArtifactResolver.replaceWithActiveArtifact( a );
                }

                getLogger().debug( "Adding: " + a.getFile() + " to classpath for extension: " + extensionArtifact.getId() );
                resultArtifacts.add( a );
            }

            if ( !realmManager.hasExtensionRealm( extensionArtifact ) )
            {
                try
                {
                    realmManager.createExtensionRealm( extensionArtifact, new ArrayList( resultArtifacts ) );
                }
                catch ( RealmManagementException e )
                {
                    String projectId = RealmUtils.createProjectId( projectGroupId, projectArtifactId, projectVersion );
                    throw new ExtensionManagerException( "Unable to create extension ClassRealm for extension: " + extensionArtifact.getId() + " within session for project: " + projectId, extensionArtifact, projectGroupId, projectArtifactId, projectVersion, e );
                }
            }

            try
            {
                realmManager.importExtensionsIntoProjectRealm( projectGroupId, projectArtifactId, projectVersion, extensionArtifact );
            }
            catch ( RealmManagementException e )
            {
                throw new ExtensionManagerException( "Unable to import extension components into project realm.", extensionArtifact, projectGroupId, projectArtifactId, projectVersion, e );
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
}
