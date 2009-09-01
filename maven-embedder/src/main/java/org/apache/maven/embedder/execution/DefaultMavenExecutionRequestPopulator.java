package org.apache.maven.embedder.execution;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.Maven;
import org.apache.maven.artifact.InvalidRepositoryException;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.embedder.MavenEmbedder;
import org.apache.maven.embedder.MavenEmbedderException;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.repository.RepositorySystem;
import org.apache.maven.settings.MavenSettingsBuilder;
import org.apache.maven.settings.Mirror;
import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.SettingsUtils;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.StringUtils;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcher;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcherException;

@Component(role = MavenExecutionRequestPopulator.class)
public class DefaultMavenExecutionRequestPopulator
    implements MavenExecutionRequestPopulator
{

    @Requirement
    private Logger logger;

    //TODO: this needs to be pushed up to the front-end
    @Requirement
    private MavenSettingsBuilder settingsBuilder;

    @Requirement
    private RepositorySystem repositorySystem;

    @Requirement( hint = "maven" )
    private SecDispatcher securityDispatcher;

    private void pom( MavenExecutionRequest request )
    {
        if ( request.getPom() != null && !request.getPom().isAbsolute() )
        {
            request.setPom( request.getPom().getAbsoluteFile() );
        }

        if ( ( request.getPom() != null ) && ( request.getPom().getParentFile() != null ) )
        {
            request.setBaseDirectory( request.getPom().getParentFile() );
        }
        else if ( ( request.getPom() == null ) && ( request.getBaseDirectory() != null ) )
        {
            File pom = new File( request.getBaseDirectory(), Maven.POMv4 );

            request.setPom( pom );
        }
        // TODO: Is this correct?
        else if ( request.getBaseDirectory() == null )
        {
            request.setBaseDirectory( new File( System.getProperty( "user.dir" ) ) );
        }
    }

    private void populateDefaultPluginGroups( MavenExecutionRequest request )
    {
        request.addPluginGroup( "org.apache.maven.plugins" );
        request.addPluginGroup( "org.codehaus.mojo" );
    }

    // Process plugin groups
    // Get profile models
    // Get active profiles
    private void processSettings( MavenExecutionRequest request )
        throws MavenEmbedderException
    {
        Settings settings = request.getSettings();

        request.addPluginGroups( settings.getPluginGroups() );

        populateDefaultPluginGroups( request );

        List<org.apache.maven.settings.Profile> settingsProfiles = settings.getProfiles();

        // We just need to keep track of what profiles are being activated by the settings. We don't need to process
        // them here. This should be taken care of by the project builder.
        //
        request.addActiveProfiles( settings.getActiveProfiles() );

        // We only need to take the profiles and make sure they are available when the calculation of the active profiles
        // is determined.
        //
        if ( ( settingsProfiles != null ) && !settingsProfiles.isEmpty() )
        {
            for ( org.apache.maven.settings.Profile rawProfile : settings.getProfiles() )
            {
                request.addProfile( SettingsUtils.convertFromSettingsProfile( rawProfile ) );
            }
        }

        injectDefaultRepositories( request );
        
        injectDefaultPluginRepositories( request );        

        processRepositoriesInSettings( request );
    }

    private void injectDefaultRepositories( MavenExecutionRequest request )
        throws MavenEmbedderException
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
                throw new MavenEmbedderException( "Cannot create default remote repository.", e );
            }
        }
    }

    private void injectDefaultPluginRepositories( MavenExecutionRequest request )
        throws MavenEmbedderException
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
                throw new MavenEmbedderException( "Cannot create default remote repository.", e );
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
        throws MavenEmbedderException
    {
        Settings settings = request.getSettings();

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

        Proxy proxy = settings.getActiveProxy();

        if ( proxy != null )
        {
            if ( proxy.getHost() == null )
            {
                throw new MavenEmbedderException( "Proxy in settings.xml has no host" );
            }

            String password = decrypt( proxy.getPassword(), "password for proxy " + proxy.getId() );

            repositorySystem.addProxy( proxy.getProtocol(), proxy.getHost(), proxy.getPort(), proxy.getUsername(),
                                       password, proxy.getNonProxyHosts() );
        }

        for ( Server server : settings.getServers() )
        {
            String password = decrypt( server.getPassword(), "password for server " + server.getId() );

            repositorySystem.addAuthenticationForArtifactRepository( server.getId(), server.getUsername(), password );
        }

        for ( Mirror mirror : settings.getMirrors() )
        {
            repositorySystem.addMirror( mirror.getId(), mirror.getMirrorOf(), mirror.getUrl() );
        }

        // <mirrors>
        //   <mirror>
        //     <id>nexus</id>
        //     <mirrorOf>*</mirrorOf>
        //     <url>http://repository.sonatype.org/content/groups/public</url>
        //   </mirror>
        // </mirrors>        

        request.setRemoteRepositories( repositorySystem.getMirrors( request.getRemoteRepositories() ) );

        request.setPluginArtifactRepositories( repositorySystem.getMirrors( request.getPluginArtifactRepositories() ) );
    }

    private String decrypt( String encrypted, String source )
    {
        try
        {
            return securityDispatcher.decrypt( encrypted );
        }
        catch ( SecDispatcherException e )
        {
            logger.warn( "Not decrypting " + source + " due to exception in security handler: " + e.getMessage() );

            Throwable cause = e;

            while ( cause.getCause() != null )
            {
                cause = cause.getCause();
            }

            if ( cause instanceof FileNotFoundException )
            {
                logger.warn( "Ensure that you have configured your master password file (and relocation if appropriate)." );
                logger.warn( "See the installation instructions for details." );
            }

            logger.debug( "Full stack trace follows", e );

            return encrypted;
        }
    }

    // ------------------------------------------------------------------------
    // Settings
    // ------------------------------------------------------------------------

    private void settings( MavenExecutionRequest request )
    {
        // ------------------------------------------------------------------------
        // Settings
        //
        // If a settings instance has been provided in the request then we use
        // that for execution, otherwise we will look in the embedder configuration
        // for a user/global settings file to use. The settings file should have
        // been validated upfront but we will still catch any parsing exception
        // ------------------------------------------------------------------------

        if ( request.getSettings() == null )
        {
            if ( request.getGlobalSettingsFile() == null )
            {
                request.setGlobalSettingsFile( MavenEmbedder.DEFAULT_GLOBAL_SETTINGS_FILE );
            }

            if ( request.getUserSettingsFile() == null )
            {
                request.setUserSettingsFile( MavenEmbedder.DEFAULT_USER_SETTINGS_FILE );
            }

            try
            {
                Settings settings = settingsBuilder.buildSettings( request );

                request.setSettings( new SettingsAdapter( request, settings ) );
            }
            catch ( Exception e )
            {
                request.setSettings( new SettingsAdapter( request, new Settings() ) );
            }
        }
    }

    private void localRepository( MavenExecutionRequest request )
        throws MavenEmbedderException
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
            request.setLocalRepository( createLocalRepository( request, request.getSettings() ) );
        }

        if ( request.getLocalRepositoryPath() == null )
        {
            request.setLocalRepositoryPath( new File( request.getLocalRepository().getBasedir() ).getAbsoluteFile() );
        }
    }

    // ------------------------------------------------------------------------
    // Artifact Transfer Mechanism
    // ------------------------------------------------------------------------

    public ArtifactRepository createLocalRepository( MavenExecutionRequest request, Settings settings )
        throws MavenEmbedderException
    {
        String localRepositoryPath = null;

        if ( request.getLocalRepositoryPath() != null )
        {
            localRepositoryPath = request.getLocalRepositoryPath().getAbsolutePath();
        }

        if ( StringUtils.isEmpty( localRepositoryPath ) )
        {
            localRepositoryPath = settings.getLocalRepository();
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
            throw new MavenEmbedderException( "Cannot create local repository.", e );
        }
    }

    public MavenExecutionRequest populateDefaults( MavenExecutionRequest request )
        throws MavenEmbedderException
    {
        pom( request );

        settings( request );

        localRepository( request );

        processSettings( request );

        return request;
    }
}
