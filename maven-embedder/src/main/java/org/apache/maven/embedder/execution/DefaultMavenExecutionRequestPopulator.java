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
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.Maven;
import org.apache.maven.artifact.InvalidRepositoryException;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.embedder.Configuration;
import org.apache.maven.embedder.MavenEmbedder;
import org.apache.maven.embedder.MavenEmbedderException;
import org.apache.maven.errors.DefaultCoreErrorReporter;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.model.Profile;
import org.apache.maven.model.Repository;
import org.apache.maven.model.RepositoryPolicy;
import org.apache.maven.monitor.event.DefaultEventMonitor;
import org.apache.maven.monitor.event.EventMonitor;
import org.apache.maven.profiles.DefaultProfileManager;
import org.apache.maven.profiles.ProfileActivationContext;
import org.apache.maven.profiles.ProfileManager;
import org.apache.maven.realm.DefaultMavenRealmManager;
import org.apache.maven.repository.MavenRepositorySystem;
import org.apache.maven.settings.MavenSettingsBuilder;
import org.apache.maven.settings.Mirror;
import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.SettingsUtils;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.util.StringUtils;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcher;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcherException;

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
    @Requirement
    private PlexusContainer container;

    @Requirement
    private MavenSettingsBuilder settingsBuilder;

    @Requirement
    private MavenRepositorySystem repositorySystem;

    // 2009-02-12 Oleg: this component is defined in maven-core components.xml
    // because it already has another declared (not generated) component
    @Requirement(hint = "maven")
    private SecDispatcher securityDispatcher;

    public MavenExecutionRequest populateDefaults( MavenExecutionRequest request, Configuration configuration )
        throws MavenEmbedderException
    {
        eventing( request, configuration );

        reporter( request, configuration );

        executionProperties( request, configuration );

        pom( request, configuration );

        settings( request, configuration );

        localRepository( request, configuration );

        snapshotPolicy( request, configuration );

        checksumPolicy( request, configuration );

        artifactTransferMechanism( request, configuration );

        realmManager( request, configuration );

        profileManager( request, configuration );

        processSettings( request, configuration );

        return request;
    }

    private void reporter( MavenExecutionRequest request, Configuration configuration )
    {
        if ( request.getErrorReporter() == null )
        {
            if ( configuration.getErrorReporter() != null )
            {
                request.setErrorReporter( configuration.getErrorReporter() );
            }
            else
            {
                request.setErrorReporter( new DefaultCoreErrorReporter() );
            }
        }
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

    private void realmManager( MavenExecutionRequest request, Configuration configuration )
    {
        if ( request.getRealmManager() == null )
        {
            if ( configuration.getRealmManager() == null )
            {
                request.setRealmManager( new DefaultMavenRealmManager( container, getLogger() ) );
            }
            else
            {
                request.setRealmManager( configuration.getRealmManager() );
            }
        }
    }

    private void processSettings( MavenExecutionRequest request, Configuration configuration )
        throws MavenEmbedderException
    {
        ProfileManager profileManager = request.getProfileManager();

        Settings settings = request.getSettings();

        request.setPluginGroups( settings.getPluginGroups() );

        List<org.apache.maven.settings.Profile> settingsProfiles = settings.getProfiles();

        List<String> settingsActiveProfileIds = settings.getActiveProfiles();

        if ( settingsActiveProfileIds != null )
        {
            for ( String profileId : settingsActiveProfileIds )
            {
                profileManager.getProfileActivationContext().setActive( profileId );
            }
        }

        if ( ( settingsProfiles != null ) && !settingsProfiles.isEmpty() )
        {
            for ( org.apache.maven.settings.Profile rawProfile : settings.getProfiles() )
            {
                Profile profile = SettingsUtils.convertFromSettingsProfile( rawProfile );

                profileManager.addProfile( profile );

                // We need to convert profile repositories to artifact repositories

                for ( Repository r : profile.getRepositories() )
                {
                    try
                    {
                        request.addRemoteRepository( repositorySystem.buildArtifactRepository( r ) );
                    }
                    catch ( InvalidRepositoryException e )
                    {
                        throw new MavenEmbedderException( "Cannot create remote repository " + r.getId(), e );
                    }
                }
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

        if ( !definedRepositories.contains( "central" ) )
        {
            Repository repo = new Repository();
            repo.setId( "central" );
            repo.setUrl( "http://repo1.maven.org/maven2" );
            repo.setName( "Maven Repository Switchboard" );
            RepositoryPolicy snapshotPolicy = new RepositoryPolicy();
            snapshotPolicy.setEnabled( false );
            repo.setSnapshots( snapshotPolicy );
            try
            {
                ArtifactRepository ar = repositorySystem.buildArtifactRepository( repo );
                request.addRemoteRepository( ar );
            }
            catch ( InvalidRepositoryException e )
            {
                throw new MavenEmbedderException( "Cannot create remote repository " + repo.getId(), e );
            }
        }
    }

    private void processRepositoriesInSettings( MavenExecutionRequest request, Configuration configuration )
        throws MavenEmbedderException
    {
        Settings settings = request.getSettings();

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
            String pass;
            String phrase;
            try
            {
                pass = securityDispatcher.decrypt( server.getPassword() );
                phrase = securityDispatcher.decrypt( server.getPassphrase() );
            }
            catch ( SecDispatcherException e )
            {
                throw new MavenEmbedderException( "Error decrypting server password/passphrase.", e );
            }

            repositorySystem.addAuthenticationInfo( server.getId(), server.getUsername(), pass, server.getPrivateKey(), phrase );

            repositorySystem.addPermissionInfo( server.getId(), server.getFilePermissions(), server.getDirectoryPermissions() );
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

        System.out.println( "ORIGINAL REPOSITORIES" );
        for ( ArtifactRepository repo : request.getRemoteRepositories() )
        {
            System.out.println( repo );
        }
        request.setRemoteRepositories( repositorySystem.getMirrors( request.getRemoteRepositories() ) );
        System.out.println( "MIRRORED REPOSITORIES" );
        for ( ArtifactRepository repo : request.getRemoteRepositories() )
        {
            System.out.println( repo );
        }
    }

    // ------------------------------------------------------------------------
    // POM
    // ------------------------------------------------------------------------

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
            // Look for a release POM
            File pom = new File( request.getBaseDirectory(), Maven.RELEASE_POMv4 );

            if ( !pom.exists() )
            {
                pom = new File( request.getBaseDirectory(), Maven.POMv4 );
            }

            request.setPom( pom );
        }
        // TODO: Is this correct?
        else if ( request.getBaseDirectory() == null )
        {
            request.setBaseDirectory( new File( System.getProperty( "user.dir" ) ) );
        }
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
    // Snapshot Policy
    // ------------------------------------------------------------------------

    private void snapshotPolicy( MavenExecutionRequest request, Configuration configuration )
    {
        // ------------------------------------------------------------------------
        // Snapshot Repository Update Policies
        //
        // Set the global policies for snapshot updates.
        // ------------------------------------------------------------------------

        boolean snapshotPolicySet = false;

        if ( request.isOffline() )
        {
            snapshotPolicySet = true;
        }

        if ( !snapshotPolicySet )
        {
            if ( request.isUpdateSnapshots() )
            {
                repositorySystem.setGlobalUpdatePolicy( ArtifactRepositoryPolicy.UPDATE_POLICY_ALWAYS );
            }
            else if ( request.isNoSnapshotUpdates() )
            {
                getLogger().info( "+ Supressing SNAPSHOT updates." );
                repositorySystem.setGlobalUpdatePolicy( ArtifactRepositoryPolicy.UPDATE_POLICY_NEVER );
            }
        }
    }

    // ------------------------------------------------------------------------
    // Checksum Policy
    // ------------------------------------------------------------------------

    private void checksumPolicy( MavenExecutionRequest request, Configuration configuration )
    {
        repositorySystem.setGlobalChecksumPolicy( request.getGlobalChecksumPolicy() );
    }

    // ------------------------------------------------------------------------
    // Artifact Transfer Mechanism
    // ------------------------------------------------------------------------

    private void artifactTransferMechanism( MavenExecutionRequest request, Configuration configuration )
        throws MavenEmbedderException
    {
        // ------------------------------------------------------------------------
        // Artifact Transfer Mechanism
        // ------------------------------------------------------------------------

        if ( request.isOffline() )
        {
            repositorySystem.setOnline( false );
        }
        else if ( ( request.getSettings() != null ) && request.getSettings().isOffline() )
        {
            repositorySystem.setOnline( false );
        }
        else
        {
            repositorySystem.setDownloadMonitor( request.getTransferListener() );

            repositorySystem.setOnline( true );
        }
    }

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
            localRepositoryPath = MavenEmbedder.defaultUserLocalRepository.getAbsolutePath();
        }

        try
        {
            return repositorySystem.createLocalRepository( localRepositoryPath, MavenEmbedder.DEFAULT_LOCAL_REPO_ID );
        }
        catch ( IOException e )
        {
            throw new MavenEmbedderException( "Cannot create local repository.", e );
        }
    }

    // ------------------------------------------------------------------------
    // Eventing
    // ------------------------------------------------------------------------

    private void eventing( MavenExecutionRequest request, Configuration configuration )
    {
        // ------------------------------------------------------------------------
        // Event Monitor/Logging
        //
        //
        // ------------------------------------------------------------------------

        if ( ( request.getEventMonitors() == null ) || request.getEventMonitors().isEmpty() )
        {
            request.addEventMonitor( new DefaultEventMonitor( getLogger() ) );
        }

        // Now, add in any event monitors from the Configuration instance.
        List<EventMonitor> configEventMonitors = configuration.getEventMonitors();

        if ( ( configEventMonitors != null ) && !configEventMonitors.isEmpty() )
        {
            for ( EventMonitor monitor : configEventMonitors )
            {
                request.addEventMonitor( monitor );
            }
        }
    }

    // ------------------------------------------------------------------------
    // Profile Manager
    // ------------------------------------------------------------------------

    private void profileManager( MavenExecutionRequest request, Configuration configuration )
    {
        // ------------------------------------------------------------------------
        // Profile Manager
        // ------------------------------------------------------------------------

        ProfileActivationContext activationContext = request.getProfileActivationContext();
        if ( activationContext == null )
        {
            activationContext = new ProfileActivationContext( request.getProperties(), false );
        }

        activationContext.setExplicitlyActiveProfileIds( request.getActiveProfiles() );
        activationContext.setExplicitlyInactiveProfileIds( request.getInactiveProfiles() );

        ProfileManager globalProfileManager = new DefaultProfileManager( container, activationContext );

        request.setProfileManager( globalProfileManager );
        request.setProfileActivationContext( activationContext );
    }
}
