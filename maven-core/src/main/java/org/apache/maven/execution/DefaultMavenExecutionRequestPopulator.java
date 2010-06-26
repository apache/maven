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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.InvalidRepositoryException;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.repository.RepositorySystem;
import org.apache.maven.settings.Mirror;
import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.SettingsUtils;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.util.StringUtils;

@Component( role = MavenExecutionRequestPopulator.class )
public class DefaultMavenExecutionRequestPopulator
    implements MavenExecutionRequestPopulator
{

    @Requirement
    private RepositorySystem repositorySystem;

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
        }

        return request;
    }

    private void populateDefaultPluginGroups( MavenExecutionRequest request )
    {
        request.addPluginGroup( "org.apache.maven.plugins" );
        request.addPluginGroup( "org.codehaus.mojo" );
    }

    private void injectDefaultRepositories( MavenExecutionRequest request )
        throws MavenExecutionRequestPopulationException
    {
        Set<String> definedRepositories = getRepoIds( request.getRemoteRepositories() );

        if ( !definedRepositories.contains( RepositorySystem.DEFAULT_REMOTE_REPO_ID ) )
        {
            try
            {
                request.addRemoteRepository( repositorySystem.createDefaultRemoteRepository() );
            }
            catch ( InvalidRepositoryException e )
            {
                throw new MavenExecutionRequestPopulationException( "Cannot create default remote repository.", e );
            }
        }
    }

    private void injectDefaultPluginRepositories( MavenExecutionRequest request )
        throws MavenExecutionRequestPopulationException
    {
        Set<String> definedRepositories = getRepoIds( request.getPluginArtifactRepositories() );

        if ( !definedRepositories.contains( RepositorySystem.DEFAULT_REMOTE_REPO_ID ) )
        {
            try
            {
                request.addPluginArtifactRepository( repositorySystem.createDefaultRemoteRepository() );
            }
            catch ( InvalidRepositoryException e )
            {
                throw new MavenExecutionRequestPopulationException( "Cannot create default remote repository.", e );
            }
        }
    }

    private Set<String> getRepoIds( List<ArtifactRepository> repositories )
    {
        Set<String> repoIds = new HashSet<String>();

        if ( repositories != null )
        {
            for ( ArtifactRepository repository : repositories )
            {
                repoIds.add( repository.getId() );
            }
        }

        return repoIds;
    }

    private void processRepositoriesInSettings( MavenExecutionRequest request )
        throws MavenExecutionRequestPopulationException
    {
        repositorySystem.injectMirror( request.getRemoteRepositories(), request.getMirrors() );
        repositorySystem.injectProxy( request.getRemoteRepositories(), request.getProxies() );
        repositorySystem.injectAuthentication( request.getRemoteRepositories(), request.getServers() );

        request.setRemoteRepositories( repositorySystem.getEffectiveRepositories( request.getRemoteRepositories() ) );

        repositorySystem.injectMirror( request.getPluginArtifactRepositories(), request.getMirrors() );
        repositorySystem.injectProxy( request.getPluginArtifactRepositories(), request.getProxies() );
        repositorySystem.injectAuthentication( request.getPluginArtifactRepositories(), request.getServers() );

        request.setPluginArtifactRepositories( repositorySystem.getEffectiveRepositories( request.getPluginArtifactRepositories() ) );
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

    public ArtifactRepository createLocalRepository( MavenExecutionRequest request )
        throws MavenExecutionRequestPopulationException
    {
        String localRepositoryPath = null;

        if ( request.getLocalRepositoryPath() != null )
        {
            localRepositoryPath = request.getLocalRepositoryPath().getAbsolutePath();
        }

        if ( StringUtils.isEmpty( localRepositoryPath ) )
        {
            localRepositoryPath = RepositorySystem.defaultUserLocalRepository.getAbsolutePath();
        }

        try
        {
            return repositorySystem.createLocalRepository( new File( localRepositoryPath ) );
        }
        catch ( InvalidRepositoryException e )
        {
            throw new MavenExecutionRequestPopulationException( "Cannot create local repository.", e );
        }
    }

    private void baseDirectory( MavenExecutionRequest request )
    {
        if ( request.getBaseDirectory() == null )
        {
            if ( request.getPom() != null )
            {
                request.setBaseDirectory( request.getPom().getAbsoluteFile().getParentFile() );
            }
        }
    }

    public MavenExecutionRequest populateDefaults( MavenExecutionRequest request )
        throws MavenExecutionRequestPopulationException
    {
        baseDirectory( request );

        localRepository( request );

        populateDefaultPluginGroups( request );

        injectDefaultRepositories( request );

        injectDefaultPluginRepositories( request );

        processRepositoriesInSettings( request );

        return request;
    }

}
