package org.apache.maven.execution;

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

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.maven.artifact.InvalidRepositoryException;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.bridge.MavenRepositorySystem;
import org.apache.maven.repository.RepositorySystem;
//
// All of this needs to go away and be couched in terms of the execution request
//
import org.apache.maven.settings.Mirror;
import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.Repository;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.SettingsUtils;
//
// Settings in core
//
import org.apache.maven.toolchain.model.PersistedToolchains;
import org.apache.maven.toolchain.model.ToolchainModel;

import static org.apache.commons.lang3.StringUtils.isEmpty;

/**
 * Assists in populating an execution request for invocation of Maven.
 */
@Named
public class DefaultMavenExecutionRequestPopulator
    implements MavenExecutionRequestPopulator
{
            
    private final MavenRepositorySystem repositorySystem;
    
    @Inject
    public DefaultMavenExecutionRequestPopulator( MavenRepositorySystem repositorySystem )
    {
        this.repositorySystem = repositorySystem;
    }


    @Override
    public MavenExecutionRequest populateFromToolchains( MavenExecutionRequest request, PersistedToolchains toolchains )
        throws MavenExecutionRequestPopulationException
    {
        if ( toolchains != null )
        {
            Map<String, List<ToolchainModel>> groupedToolchains = new HashMap<>( 2 );

            for ( ToolchainModel model : toolchains.getToolchains() )
            {
                if ( !groupedToolchains.containsKey( model.getType() ) )
                {
                    groupedToolchains.put( model.getType(), new ArrayList<ToolchainModel>() );
                }

                groupedToolchains.get( model.getType() ).add( model );
            }

            request.setToolchains( groupedToolchains );
        }
        return request;
    }
    
    @Override
    public MavenExecutionRequest populateDefaults( MavenExecutionRequest request )
        throws MavenExecutionRequestPopulationException
    {
        baseDirectory( request );

        localRepository( request );

        populateDefaultPluginGroups( request );

        injectDefaultRepositories( request );

        injectDefaultPluginRepositories( request );

        return request;
    }
    
    //
    //
    //
    
    private void populateDefaultPluginGroups( MavenExecutionRequest request )
    {
        request.addPluginGroup( "org.apache.maven.plugins" );
        request.addPluginGroup( "org.codehaus.mojo" );
    }

    private void injectDefaultRepositories( MavenExecutionRequest request )
        throws MavenExecutionRequestPopulationException
    {
        Set<String> definedRepositories = repositorySystem.getRepoIds( request.getRemoteRepositories() );

        if ( !definedRepositories.contains( RepositorySystem.DEFAULT_REMOTE_REPO_ID ) )
        {
            try
            {
                request.addRemoteRepository( repositorySystem.createDefaultRemoteRepository( request ) );
            }
            catch ( Exception e )
            {
                throw new MavenExecutionRequestPopulationException( "Cannot create default remote repository.", e );
            }
        }
    }

    private void injectDefaultPluginRepositories( MavenExecutionRequest request )
        throws MavenExecutionRequestPopulationException
    {
        Set<String> definedRepositories = repositorySystem.getRepoIds( request.getPluginArtifactRepositories() );

        if ( !definedRepositories.contains( RepositorySystem.DEFAULT_REMOTE_REPO_ID ) )
        {
            try
            {
                request.addPluginArtifactRepository( repositorySystem.createDefaultRemoteRepository( request ) );
            }
            catch ( Exception e )
            {
                throw new MavenExecutionRequestPopulationException( "Cannot create default remote repository.", e );
            }
        }
    }

    private void localRepository( MavenExecutionRequest request )
        throws MavenExecutionRequestPopulationException
    {
        // ------------------------------------------------------------------------
        // Local Repository
        //
        // 1. Use a value has been passed in via the configuration
        // 2. Use value in the resultant settings
        // 3. Use default value
        // ------------------------------------------------------------------------

        if ( request.getLocalRepository() == null )
        {
            request.setLocalRepository( createLocalRepository( request ) );
        }

        if ( request.getLocalRepositoryPath() == null )
        {
            request.setLocalRepositoryPath( new File( request.getLocalRepository().getBasedir() ).getAbsoluteFile() );
        }
    }

    // ------------------------------------------------------------------------
    // Artifact Transfer Mechanism
    // ------------------------------------------------------------------------

    private ArtifactRepository createLocalRepository( MavenExecutionRequest request )
        throws MavenExecutionRequestPopulationException
    {
        String localRepositoryPath = null;

        if ( request.getLocalRepositoryPath() != null )
        {
            localRepositoryPath = request.getLocalRepositoryPath().getAbsolutePath();
        }

        if ( isEmpty( localRepositoryPath ) )
        {
            localRepositoryPath = RepositorySystem.defaultUserLocalRepository.getAbsolutePath();
        }

        try
        {
            return repositorySystem.createLocalRepository( request, new File( localRepositoryPath ) );
        }
        catch ( Exception e )
        {
            throw new MavenExecutionRequestPopulationException( "Cannot create local repository.", e );
        }
    }

    private void baseDirectory( MavenExecutionRequest request )
    {
        if ( request.getBaseDirectory() == null && request.getPom() != null )
        {
            request.setBaseDirectory( request.getPom().getAbsoluteFile().getParentFile() );
        }
    }   
    
    /*if_not[MAVEN4]*/
    
    @Override
    @Deprecated
    public MavenExecutionRequest populateFromSettings( MavenExecutionRequest request, Settings settings )
        throws MavenExecutionRequestPopulationException
    {
        if ( settings == null )
        {
            return request;
        }

        request.setOffline( settings.isOffline() );

        request.setInteractiveMode( settings.isInteractiveMode() );

        request.setPluginGroups( settings.getPluginGroups() );

        request.setLocalRepositoryPath( settings.getLocalRepository() );

        for ( Server server : settings.getServers() )
        {
            server = server.clone();

            request.addServer( server );
        }

        //  <proxies>
        //    <proxy>
        //      <active>true</active>
        //      <protocol>http</protocol>
        //      <host>proxy.somewhere.com</host>
        //      <port>8080</port>
        //      <username>proxyuser</username>
        //      <password>somepassword</password>
        //      <nonProxyHosts>www.google.com|*.somewhere.com</nonProxyHosts>
        //    </proxy>
        //  </proxies>

        for ( Proxy proxy : settings.getProxies() )
        {
            if ( !proxy.isActive() )
            {
                continue;
            }

            proxy = proxy.clone();

            request.addProxy( proxy );
        }

        // <mirrors>
        //   <mirror>
        //     <id>nexus</id>
        //     <mirrorOf>*</mirrorOf>
        //     <url>http://repository.sonatype.org/content/groups/public</url>
        //   </mirror>
        // </mirrors>

        for ( Mirror mirror : settings.getMirrors() )
        {
            mirror = mirror.clone();

            request.addMirror( mirror );
        }

        request.setActiveProfiles( settings.getActiveProfiles() );

        for ( org.apache.maven.settings.Profile rawProfile : settings.getProfiles() )
        {
            request.addProfile( SettingsUtils.convertFromSettingsProfile( rawProfile ) );

            if ( settings.getActiveProfiles().contains( rawProfile.getId() ) )
            {
                List<Repository> remoteRepositories = rawProfile.getRepositories();
                for ( Repository remoteRepository : remoteRepositories )
                {
                    try
                    {
                        request.addRemoteRepository( repositorySystem.buildArtifactRepository( remoteRepository ) );
                    }
                    catch ( InvalidRepositoryException e )
                    {
                        // do nothing for now
                    }
                }

                List<Repository> pluginRepositories = rawProfile.getPluginRepositories();
                for ( Repository pluginRepo : pluginRepositories )
                {
                    try
                    {
                        request.addPluginArtifactRepository( repositorySystem.buildArtifactRepository( pluginRepo ) );
                    }
                    catch ( InvalidRepositoryException e )
                    {
                        // do nothing for now
                    }
                }
            }
        }

        return request;
    }    
    
    /*end[MAVEN4]*/

}
