package org.apache.maven.plugin;

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

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.ResolutionErrorHandler;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.execution.RuntimeInformation;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.version.PluginVersionManager;
import org.apache.maven.plugin.version.PluginVersionNotFoundException;
import org.apache.maven.plugin.version.PluginVersionResolutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.realm.RealmManagementException;
import org.apache.maven.realm.RealmScanningUtils;
import org.apache.maven.repository.MavenRepositorySystem;
import org.apache.maven.repository.VersionNotFoundException;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.ContextException;
import org.codehaus.plexus.logging.LogEnabled;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;

@Component(role = PluginManagerSupport.class)
public class DefaultPluginManagerSupport
    implements PluginManagerSupport, LogEnabled, Contextualizable
{
    @Requirement
    private MavenRepositorySystem repositorySystem;

    @Requirement
    private ResolutionErrorHandler resolutionErrorHandler;
        
    @Requirement
    private MavenProjectBuilder mavenProjectBuilder;
    
    @Requirement
    private RuntimeInformation runtimeInformation;
    
    @Requirement
    private PluginVersionManager pluginVersionManager;
    
    //@Requirement
    private Logger logger;

    private Context containerContext;

    public Artifact resolvePluginArtifact( Plugin plugin, MavenProject project, MavenSession session )
        throws PluginManagerException, InvalidPluginException, PluginVersionResolutionException, ArtifactResolutionException, ArtifactNotFoundException
    {
        ArtifactRepository localRepository = session.getLocalRepository();

        List remoteRepositories = new ArrayList();

        remoteRepositories.addAll( project.getRemoteArtifactRepositories() );

        MavenProject pluginProject = null;
        for(MavenProject mp : (List<MavenProject>) session.getSortedProjects())
        {
            if(mp.getId().equals(project.getId()))
            {
                pluginProject = mp;
                break;
            }
        }

        if(pluginProject == null)
        {
            pluginProject = buildPluginProject( plugin, localRepository, remoteRepositories );
        }

        checkRequiredMavenVersion( plugin, pluginProject, localRepository, remoteRepositories );

        checkPluginDependencySpec( plugin, pluginProject );

        Artifact pluginArtifact = repositorySystem.createPluginArtifact( plugin.getGroupId(), plugin.getArtifactId(), plugin.getVersion() );

        pluginArtifact = project.replaceWithActiveArtifact( pluginArtifact );

        ArtifactResolutionRequest request = new ArtifactResolutionRequest( pluginArtifact, localRepository, remoteRepositories );
        ArtifactResolutionResult result = repositorySystem.resolve( request );
        resolutionErrorHandler.throwErrors( request, result );

        return pluginArtifact;
    }

    public MavenProject buildPluginProject( Plugin plugin, ArtifactRepository localRepository, List remoteRepositories )
        throws InvalidPluginException
    {
        Artifact artifact = repositorySystem.createProjectArtifact( plugin.getGroupId(), plugin.getArtifactId(), plugin.getVersion() );
        try
        {
            return mavenProjectBuilder.buildFromRepository( artifact, remoteRepositories, localRepository );
        }
        catch ( ProjectBuildingException e )
        {
            throw new InvalidPluginException( "Unable to build project for plugin '"
                                              + plugin.getKey() + "': " + e.getMessage(), e );
        }
    }

    /**
     * @param pluginProject
     * @todo would be better to store this in the plugin descriptor, but then it won't be available to the version
     * manager which executes before the plugin is instantiated
     */
    public void checkRequiredMavenVersion( Plugin plugin,
                                           MavenProject pluginProject,
                                           ArtifactRepository localRepository,
                                           List remoteRepositories )
        throws PluginVersionResolutionException, InvalidPluginException
    {
        // if we don't have the required Maven version, then ignore an update
        if ( ( pluginProject.getPrerequisites() != null )
             && ( pluginProject.getPrerequisites().getMaven() != null ) )
        {
            DefaultArtifactVersion requiredVersion = new DefaultArtifactVersion(
                                                                                 pluginProject.getPrerequisites()
                                                                                              .getMaven() );

            if ( runtimeInformation.getApplicationInformation().getVersion().compareTo( requiredVersion ) < 0 )
            {
                throw new PluginVersionResolutionException( plugin.getGroupId(),
                                                            plugin.getArtifactId(),
                                                            "Plugin requires Maven version "
                                                                            + requiredVersion );
            }
        }
    }

    public void checkPluginDependencySpec( Plugin plugin, MavenProject pluginProject )
        throws InvalidPluginException
    {
        ArtifactFilter filter = new ScopeArtifactFilter( "runtime" );
        try
        {
            repositorySystem.createArtifacts( pluginProject.getDependencies(), null, filter, pluginProject );
        }
        catch ( VersionNotFoundException e )
        {
            throw new InvalidPluginException( "Plugin: " + plugin.getKey() + " has a dependency with an invalid version." );
        }
    }

    public PluginDescriptor loadIsolatedPluginDescriptor( Plugin plugin,
                                                          MavenProject project,
                                                          MavenSession session )
    {
        if ( plugin.getVersion() == null )
        {
            try
            {
                plugin.setVersion( pluginVersionManager.resolvePluginVersion( plugin.getGroupId(),
                                                                              plugin.getArtifactId(),
                                                                              project,
                                                                              session ) );
            }
            catch ( PluginVersionResolutionException e )
            {
                logger.debug( "Failed to load plugin descriptor for: " + plugin.getKey(), e );
            }
            catch ( InvalidPluginException e )
            {
                logger.debug( "Failed to load plugin descriptor for: " + plugin.getKey(), e );
            }
            catch ( PluginVersionNotFoundException e )
            {
                logger.debug( "Failed to load plugin descriptor for: " + plugin.getKey(), e );
            }
        }

        if ( plugin.getVersion() == null )
        {
            return null;
        }

        Artifact artifact = null;
        try
        {
            artifact = resolvePluginArtifact( plugin, project, session );
        }
        catch ( ArtifactResolutionException e )
        {
            logger.debug( "Failed to load plugin descriptor for: " + plugin.getKey(), e );
        }
        catch ( ArtifactNotFoundException e )
        {
            logger.debug( "Failed to load plugin descriptor for: " + plugin.getKey(), e );
        }
        catch ( PluginManagerException e )
        {
            logger.debug( "Failed to load plugin descriptor for: " + plugin.getKey(), e );
        }
        catch ( InvalidPluginException e )
        {
            logger.debug( "Failed to load plugin descriptor for: " + plugin.getKey(), e );
        }
        catch ( PluginVersionResolutionException e )
        {
            logger.debug( "Failed to load plugin descriptor for: " + plugin.getKey(), e );
        }

        if ( artifact == null )
        {
            return null;
        }

        MavenPluginDiscoverer discoverer = new MavenPluginDiscoverer();
        discoverer.setManager( RealmScanningUtils.getDummyComponentDiscovererManager() );

        try
        {
            List componentSetDescriptors = RealmScanningUtils.scanForComponentSetDescriptors( artifact,
                                                                                              discoverer,
                                                                                              containerContext,
                                                                                              "Plugin: "
                                                                                                              + plugin.getKey() );

            if ( !componentSetDescriptors.isEmpty() )
            {
                return (PluginDescriptor) componentSetDescriptors.get( 0 );
            }
        }
        catch ( RealmManagementException e )
        {
            logger.debug( "Failed to scan plugin artifact: " + artifact.getId()
                          + " for descriptors.", e );
        }

        return null;
    }

    public void enableLogging( Logger logger )
    {
        this.logger = logger;
    }

    public void contextualize( Context context )
        throws ContextException
    {
        containerContext = context;
    }
}
