package org.apache.maven.embedder.execution;

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

import org.apache.maven.Maven;
import org.apache.maven.artifact.manager.WagonManager;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryFactory;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.embedder.Configuration;
import org.apache.maven.embedder.MavenEmbedder;
import org.apache.maven.embedder.MavenEmbedderException;
import org.apache.maven.errors.DefaultCoreErrorReporter;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.model.Profile;
import org.apache.maven.model.Repository;
import org.apache.maven.monitor.event.DefaultEventMonitor;
import org.apache.maven.monitor.event.EventMonitor;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.profiles.DefaultProfileManager;
import org.apache.maven.profiles.ProfileManager;
import org.apache.maven.profiles.activation.DefaultProfileActivationContext;
import org.apache.maven.profiles.activation.ProfileActivationContext;
import org.apache.maven.realm.DefaultMavenRealmManager;
import org.apache.maven.settings.MavenSettingsBuilder;
import org.apache.maven.settings.Mirror;
import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.SettingsConfigurationException;
import org.apache.maven.settings.SettingsUtils;
import org.apache.maven.wagon.repository.RepositoryPermissions;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLifecycleException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.ContextException;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

/**
 * Things that we deal with in this populator to ensure that we have a valid {@MavenExecutionRequest}
 * <p/>
 * - POM
 * - Settings
 * - Local Repository
 * - Snapshot update policies
 * - Repository checksum policies
 * - Artifact transfer mechanism configuration
 * - Eventing/Logging configuration
 * - Profile manager configuration
 *
 * @version $Id$
 */
public class DefaultMavenExecutionRequestPopulator
    extends AbstractLogEnabled
    implements MavenExecutionRequestPopulator,
    Contextualizable
{
    private ArtifactRepositoryFactory artifactRepositoryFactory;

    private ArtifactRepositoryLayout defaultArtifactRepositoryLayout;

    private PlexusContainer container;

    private WagonManager wagonManager;

    private MavenSettingsBuilder settingsBuilder;

    public MavenExecutionRequest populateDefaults( MavenExecutionRequest request,
                                                   Configuration configuration )
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

    private void reporter( MavenExecutionRequest request,
                           Configuration configuration )
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

    private void executionProperties( MavenExecutionRequest request,
                                      Configuration configuration )
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

        if ( requestProperties.getProperty( MavenEmbedder.STANDALONE_MODE ) == null )
        {
            // TODO: Remove this!
            System.setProperty( MavenEmbedder.STANDALONE_MODE, "true" );
            requestProperties.setProperty( MavenEmbedder.STANDALONE_MODE, "true" );
        }

        Properties userProperties = request.getUserProperties();
        if ( userProperties != null )
        {
            for ( Iterator it = userProperties.keySet().iterator(); it.hasNext(); )
            {
                String key = (String) it.next();
                if ( !requestProperties.containsKey( key ) )
                {
                    requestProperties.setProperty( key, userProperties.getProperty( key ) );
                }
            }
        }
    }

    private void realmManager( MavenExecutionRequest request,
                               Configuration configuration )
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

    private void processSettings( MavenExecutionRequest request,
                                  Configuration configuration )
    {
        ProfileManager profileManager = request.getProfileManager();

        Settings settings = request.getSettings();

        List settingsProfiles = settings.getProfiles();

        List settingsActiveProfileIds = settings.getActiveProfiles();

        profileManager.explicitlyActivate( settingsActiveProfileIds );

        if ( ( settingsProfiles != null ) && !settingsProfiles.isEmpty() )
        {
            for ( Iterator it = settings.getProfiles().iterator(); it.hasNext(); )
            {
                org.apache.maven.settings.Profile rawProfile = (org.apache.maven.settings.Profile) it.next();

                Profile profile = SettingsUtils.convertFromSettingsProfile( rawProfile );

                profileManager.addProfile( profile );

                // We need to convert profile repositories to artifact repositories

                for ( Iterator j = profile.getRepositories().iterator(); j.hasNext(); )
                {
                    Repository r = (Repository) j.next();

                    ArtifactRepositoryPolicy releases = new ArtifactRepositoryPolicy();

                    if ( r.getReleases() != null )
                    {
                        releases.setChecksumPolicy( r.getReleases().getChecksumPolicy() );

                        releases.setUpdatePolicy( r.getReleases().getUpdatePolicy() );
                    }
                    else
                    {
                        releases.setChecksumPolicy( ArtifactRepositoryPolicy.UPDATE_POLICY_DAILY );

                        releases.setUpdatePolicy( ArtifactRepositoryPolicy.CHECKSUM_POLICY_WARN );
                    }

                    ArtifactRepositoryPolicy snapshots = new ArtifactRepositoryPolicy();

                    if ( r.getSnapshots() != null )
                    {
                        snapshots.setChecksumPolicy( r.getSnapshots().getChecksumPolicy() );

                        snapshots.setUpdatePolicy( r.getSnapshots().getUpdatePolicy() );
                    }
                    else
                    {
                        snapshots.setChecksumPolicy( ArtifactRepositoryPolicy.UPDATE_POLICY_DAILY );

                        snapshots.setUpdatePolicy( ArtifactRepositoryPolicy.CHECKSUM_POLICY_WARN );
                    }

                    ArtifactRepository ar = artifactRepositoryFactory.createArtifactRepository( r.getId(), r.getUrl(),
                        defaultArtifactRepositoryLayout, snapshots, releases );

                    request.addRemoteRepository( ar );
                }
            }
        }
    }

    // ------------------------------------------------------------------------
    // POM
    // ------------------------------------------------------------------------

    private void pom( MavenExecutionRequest request,
                      Configuration configuration )
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

    private void settings( MavenExecutionRequest request,
                           Configuration configuration )
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

    // ------------------------------------------------------------------------
    // Local Repository
    // ------------------------------------------------------------------------

    private void localRepository( MavenExecutionRequest request,
                                  Configuration configuration )
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
    }


    public ArtifactRepository createLocalRepository( MavenExecutionRequest request,
                                                     Settings settings,
                                                     Configuration configuration )
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

        return createLocalRepository(
            localRepositoryPath,
            MavenEmbedder.DEFAULT_LOCAL_REPO_ID );
    }

    public ArtifactRepository createLocalRepository( String url,
                                                     String repositoryId )
        throws MavenEmbedderException
    {
        try
        {
            return createRepository(
                canonicalFileUrl( url ),
                repositoryId );
        }
        catch ( IOException e )
        {
            throw new MavenEmbedderException(
                "Unable to resolve canonical path for local repository " + url,
                e );
        }
    }

    private String canonicalFileUrl( String url )
        throws IOException
    {
        if ( !url.startsWith( "file:" ) )
        {
            url = "file://" + url;
        }
        else if ( url.startsWith( "file:" ) && !url.startsWith( "file://" ) )
        {
            url = "file://" + url.substring( "file:".length() );
        }

        // So now we have an url of the form file://<path>

        // We want to eliminate any relative path nonsense and lock down the path so we
        // need to fully resolve it before any sub-modules use the path. This can happen
        // when you are using a custom settings.xml that contains a relative path entry
        // for the local repository setting.

        File localRepository = new File( url.substring( "file://".length() ) );

        if ( !localRepository.isAbsolute() )
        {
            url = "file://" + localRepository.getCanonicalPath();
        }

        return url;
    }

    public ArtifactRepository createRepository( String url,
                                                String repositoryId )
    {
        // snapshots vs releases
        // offline = to turning the update policy off

        //TODO: we'll need to allow finer grained creation of repositories but this will do for now

        String updatePolicyFlag = ArtifactRepositoryPolicy.UPDATE_POLICY_ALWAYS;

        String checksumPolicyFlag = ArtifactRepositoryPolicy.CHECKSUM_POLICY_WARN;

        ArtifactRepositoryPolicy snapshotsPolicy =
            new ArtifactRepositoryPolicy(
                true,
                updatePolicyFlag,
                checksumPolicyFlag );

        ArtifactRepositoryPolicy releasesPolicy =
            new ArtifactRepositoryPolicy(
                true,
                updatePolicyFlag,
                checksumPolicyFlag );

        return artifactRepositoryFactory.createArtifactRepository(
            repositoryId,
            url,
            defaultArtifactRepositoryLayout,
            snapshotsPolicy,
            releasesPolicy );
    }

    // ------------------------------------------------------------------------
    // Snapshot Policy
    // ------------------------------------------------------------------------

    private void snapshotPolicy( MavenExecutionRequest request,
                                 Configuration configuration )
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
                artifactRepositoryFactory.setGlobalUpdatePolicy( ArtifactRepositoryPolicy.UPDATE_POLICY_ALWAYS );
            }
            else if ( request.isNoSnapshotUpdates() )
            {
                getLogger().info( "+ Supressing SNAPSHOT updates." );
                artifactRepositoryFactory.setGlobalUpdatePolicy( ArtifactRepositoryPolicy.UPDATE_POLICY_NEVER );
            }
        }
    }

    // ------------------------------------------------------------------------
    // Checksum Policy
    // ------------------------------------------------------------------------

    private void checksumPolicy( MavenExecutionRequest request,
                                 Configuration configuration )
    {
        // ------------------------------------------------------------------------
        // Repository Checksum Policies
        //
        // We take the checksum passed in via the request.
        // ------------------------------------------------------------------------

        artifactRepositoryFactory.setGlobalChecksumPolicy( request.getGlobalChecksumPolicy() );
    }

    // ------------------------------------------------------------------------
    // Artifact Transfer Mechanism
    // ------------------------------------------------------------------------

    private void artifactTransferMechanism( MavenExecutionRequest request,
                                            Configuration configuration )
        throws MavenEmbedderException
    {
        // ------------------------------------------------------------------------
        // Artifact Transfer Mechanism
        //
        //
        // ------------------------------------------------------------------------

        if ( request.isOffline() )
        {
            wagonManager.setOnline( false );
        }
        else if ( ( request.getSettings() != null ) && request.getSettings().isOffline() )
        {
            wagonManager.setOnline( false );
        }
        else
        {
            wagonManager.findAndRegisterWagons( container );

            wagonManager.setInteractive( request.isInteractiveMode() );

            wagonManager.setDownloadMonitor( request.getTransferListener() );

            wagonManager.setOnline( true );
        }

        try
        {
            resolveParameters( request.getSettings() );
        }
        catch ( Exception e )
        {
            throw new MavenEmbedderException(
                "Unable to configure Maven for execution",
                e );
        }
    }

    private void resolveParameters( Settings settings )
        throws ComponentLookupException, ComponentLifecycleException, SettingsConfigurationException
    {
        WagonManager wagonManager = (WagonManager) container.lookup( WagonManager.ROLE );

        try
        {
            Proxy proxy = settings.getActiveProxy();

            if ( proxy != null )
            {
                if ( proxy.getHost() == null )
                {
                    throw new SettingsConfigurationException( "Proxy in settings.xml has no host" );
                }

                wagonManager.addProxy(
                    proxy.getProtocol(),
                    proxy.getHost(),
                    proxy.getPort(),
                    proxy.getUsername(),
                    proxy.getPassword(),
                    proxy.getNonProxyHosts() );
            }

            for ( Iterator i = settings.getServers().iterator(); i.hasNext(); )
            {
                Server server = (Server) i.next();

                wagonManager.addAuthenticationInfo(
                    server.getId(),
                    server.getUsername(),
                    server.getPassword(),
                    server.getPrivateKey(),
                    server.getPassphrase() );

                wagonManager.addPermissionInfo(
                    server.getId(),
                    server.getFilePermissions(),
                    server.getDirectoryPermissions() );

                if ( server.getConfiguration() != null )
                {
                    wagonManager.addConfiguration(
                        server.getId(),
                        (Xpp3Dom) server.getConfiguration() );
                }
            }

            RepositoryPermissions defaultPermissions = new RepositoryPermissions();

            defaultPermissions.setDirectoryMode( "775" );

            defaultPermissions.setFileMode( "664" );

            wagonManager.setDefaultRepositoryPermissions( defaultPermissions );

            for ( Iterator i = settings.getMirrors().iterator(); i.hasNext(); )
            {
                Mirror mirror = (Mirror) i.next();

                wagonManager.addMirror(
                    mirror.getId(),
                    mirror.getMirrorOf(),
                    mirror.getUrl() );
            }
        }
        finally
        {
            container.release( wagonManager );
        }
    }

    // ------------------------------------------------------------------------
    // Eventing
    // ------------------------------------------------------------------------

    private void eventing( MavenExecutionRequest request,
                           Configuration configuration )
    {
        // ------------------------------------------------------------------------
        // Event Monitor/Logging
        //
        //
        // ------------------------------------------------------------------------

        Logger logger = container.getLoggerManager().getLoggerForComponent( Mojo.ROLE );

        if ( ( request.getEventMonitors() == null ) || request.getEventMonitors().isEmpty() )
        {
            request.addEventMonitor( new DefaultEventMonitor( logger ) );
        }

        container.getLoggerManager().setThreshold( request.getLoggingLevel() );

        // Now, add in any event monitors from the Configuration instance.
        List configEventMonitors = configuration.getEventMonitors();

        if ( ( configEventMonitors != null ) && !configEventMonitors.isEmpty() )
        {
            for ( Iterator it = configEventMonitors.iterator(); it.hasNext(); )
            {
                EventMonitor monitor = (EventMonitor) it.next();
                request.addEventMonitor( monitor );
            }
        }
    }

    // ------------------------------------------------------------------------
    // Profile Manager
    // ------------------------------------------------------------------------

    private void profileManager( MavenExecutionRequest request,
                                 Configuration configuration )
    {
        // ------------------------------------------------------------------------
        // Profile Manager
        //
        //
        // ------------------------------------------------------------------------

        ProfileActivationContext activationContext = request.getProfileActivationContext();
        if ( activationContext == null )
        {
            activationContext = new DefaultProfileActivationContext(
                    request.getProperties(), false );
        }

        activationContext.setExplicitlyActiveProfileIds( request.getActiveProfiles() );
        activationContext.setExplicitlyInactiveProfileIds( request.getInactiveProfiles() );

        ProfileManager globalProfileManager = new DefaultProfileManager( container, activationContext );

        request.setProfileManager( globalProfileManager );
        request.setProfileActivationContext( activationContext );
    }

    // ----------------------------------------------------------------------------
    // Lifecycle
    // ----------------------------------------------------------------------------

    public void contextualize( Context context )
        throws ContextException
    {
        container = (PlexusContainer) context.get( PlexusConstants.PLEXUS_KEY );
    }
}
