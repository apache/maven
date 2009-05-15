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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.Maven;
import org.apache.maven.artifact.InvalidRepositoryException;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.embedder.Configuration;
import org.apache.maven.embedder.MavenEmbedderException;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.repository.RepositorySystem;
import org.apache.maven.settings.MavenSettingsBuilder;
import org.apache.maven.settings.Mirror;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.SettingsUtils;
import org.apache.maven.toolchain.ToolchainsBuilder;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.util.StringUtils;

/**
 * Things that we deal with in this populator to ensure that we have a valid
 * {@MavenExecutionRequest}
 * <p/>
 * - POM - Settings - Local Repository - Snapshot update policies - Repository checksum policies -
 * Artifact transfer mechanism configuration - Eventing/Logging configuration - Profile manager
 * configuration
 */
@Component(role = MavenExecutionRequestPopulator.class)
public class DefaultMavenExecutionRequestPopulator
    extends AbstractLogEnabled
    implements MavenExecutionRequestPopulator
{
    //TODO: this needs to be pushed up to the front-end
    @Requirement
    private MavenSettingsBuilder settingsBuilder;

    @Requirement
    private RepositorySystem repositorySystem;

    @Requirement
    private ToolchainsBuilder toolchainsBuilder;

    public MavenExecutionRequest populateDefaults( MavenExecutionRequest request, Configuration configuration )
        throws MavenEmbedderException
    {
        executionProperties( request, configuration );

        pom( request, configuration );

        settings( request, configuration );

        localRepository( request, configuration );

        toolchains( request, configuration );

        processSettings( request, configuration );
                
        return request;
    }

    private void executionProperties( MavenExecutionRequest request, Configuration configuration )
    {
        Properties requestProperties = request.getProperties();

        if ( requestProperties == null )
        {
            requestProperties = configuration.getSystemProperties();
            if ( requestProperties == null )
            {
                requestProperties = System.getProperties();
            }

            request.setProperties( requestProperties );
        }

        Properties userProperties = request.getUserProperties();
        if ( userProperties != null )
        {
            for ( Iterator<?> it = userProperties.keySet().iterator(); it.hasNext(); )
            {
                String key = (String) it.next();
                if ( !requestProperties.containsKey( key ) )
                {
                    requestProperties.setProperty( key, userProperties.getProperty( key ) );
                }
            }
        }
    }
    
    private void pom( MavenExecutionRequest request, Configuration configuration )
    {
        // ------------------------------------------------------------------------
        // POM
        //
        // If we are not given a specific POM file, but passed a base directory
        // then we will use a release POM in the directory provide, or and then
        // look for the standard POM.
        // ------------------------------------------------------------------------

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
    
    // Process plugin groups
    // Get profile models
    // Get active profiles
    private void processSettings( MavenExecutionRequest request, Configuration configuration )
        throws MavenEmbedderException
    {
        Settings settings = request.getSettings();

        request.setPluginGroups( settings.getPluginGroups() );
        
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

        processRepositoriesInSettings( request, configuration );
    }

    private void injectDefaultRepositories( MavenExecutionRequest request )
        throws MavenEmbedderException
    {
        Set<String> definedRepositories = new HashSet<String>();
        if ( request.getRemoteRepositories() != null )
        {
            for ( ArtifactRepository repository : request.getRemoteRepositories() )
            {
                definedRepositories.add( repository.getId() );
            }
        }

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

    private void processRepositoriesInSettings( MavenExecutionRequest request, Configuration configuration )
        throws MavenEmbedderException
    {
        Settings settings = request.getSettings();

        /*
        Proxy proxy = settings.getActiveProxy();

        if ( proxy != null )
        {
            if ( proxy.getHost() == null )
            {
                throw new MavenEmbedderException( "Proxy in settings.xml has no host" );
            }

            repositorySystem.addProxy( proxy.getProtocol(), proxy.getHost(), proxy.getPort(), proxy.getUsername(), proxy.getPassword(), proxy.getNonProxyHosts() );
        }

        for ( Server server : settings.getServers() )
        {
            String password;
            String passPhrase;
            
            try
            {
                password = securityDispatcher.decrypt( server.getPassword() );
                passPhrase = securityDispatcher.decrypt( server.getPassphrase() );
            }
            catch ( SecDispatcherException e )
            {
                throw new MavenEmbedderException( "Error decrypting server password/passphrase.", e );
            }

            repositorySystem.addAuthenticationInfo( server.getId(), server.getUsername(), password, server.getPrivateKey(), passPhrase );

            repositorySystem.addPermissionInfo( server.getId(), server.getFilePermissions(), server.getDirectoryPermissions() );
        }
        */

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
    }

    // ------------------------------------------------------------------------
    // Settings
    // ------------------------------------------------------------------------

    private void settings( MavenExecutionRequest request, Configuration configuration )
    {
        // ------------------------------------------------------------------------
        // Settings
        //
        // If a settings instance has been provided in the request the we use
        // that for execution, otherwise we will look in the embedder configuration
        // for a user/global settings file to use. The settings file should have
        // been validated upfront but we will still catch any parsing exception
        // ------------------------------------------------------------------------

        if ( request.getSettings() == null )
        {
            if ( configuration.getGlobalSettingsFile() != null )
            {
                request.setGlobalSettingsFile( configuration.getGlobalSettingsFile() );
            }

            if ( configuration.getUserSettingsFile() != null )
            {
                request.setUserSettingsFile( configuration.getUserSettingsFile() );
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

    private void localRepository( MavenExecutionRequest request, Configuration configuration )
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
            request.setLocalRepository( createLocalRepository( request, request.getSettings(), configuration ) );
        }

        if ( request.getLocalRepositoryPath() == null )
        {
            request.setLocalRepositoryPath( new File( request.getLocalRepository().getBasedir() ).getAbsoluteFile() );
        }
    }

    // ------------------------------------------------------------------------
    // Artifact Transfer Mechanism
    // ------------------------------------------------------------------------

    public ArtifactRepository createLocalRepository( MavenExecutionRequest request, Settings settings, Configuration configuration )
        throws MavenEmbedderException
    {
        String localRepositoryPath = null;

        if ( request.getLocalRepositoryPath() != null )
        {
            localRepositoryPath = request.getLocalRepositoryPath().getAbsolutePath();
        }

        if ( StringUtils.isEmpty( localRepositoryPath ) && ( configuration.getLocalRepository() != null ) )
        {
            localRepositoryPath = configuration.getLocalRepository().getAbsolutePath();
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

    private void toolchains( MavenExecutionRequest request, Configuration configuration )
    {
        toolchainsBuilder.setUserToolchainsFile( request.getUserToolchainsFile() );
    }

}
