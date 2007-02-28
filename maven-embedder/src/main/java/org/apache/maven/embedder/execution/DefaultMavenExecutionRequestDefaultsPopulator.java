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

import org.apache.maven.SettingsConfigurationException;
import org.apache.maven.artifact.repository.ArtifactRepositoryFactory;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.embedder.MavenEmbedderException;
import org.apache.maven.embedder.MavenEmbedder;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.monitor.event.DefaultEventMonitor;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.settings.Mirror;
import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.apache.maven.usability.SystemWarnings;
import org.apache.maven.wagon.manager.RepositorySettings;
import org.apache.maven.wagon.manager.WagonManager;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.configuration.xml.XmlPlexusConfiguration;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.ContextException;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import java.io.File;
import java.util.Iterator;

/**
 * DefaultMavenExecutionRequestDefaultsPopulator
 *
 * @version $Id$
 */
public class DefaultMavenExecutionRequestDefaultsPopulator
    extends AbstractLogEnabled
    implements MavenExecutionRequestDefaultsPopulator, Contextualizable
{
    private ArtifactRepositoryFactory artifactRepositoryFactory;

    private ArtifactRepositoryLayout defaultArtifactRepositoryLayout;

    private PlexusContainer container;

    private WagonManager wagonManager;

    public MavenExecutionRequest populateDefaults( MavenExecutionRequest request, MavenEmbedder embedder )
        throws MavenEmbedderException
    {
        if ( request.getSettings() == null )
        {
            request.setSettings( embedder.getSettings() );
        }

        if ( request.getLocalRepository() == null )
        {
            request.setLocalRepository( embedder.getLocalRepository() );
        }

        // Repository update policies

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

        artifactRepositoryFactory.setGlobalChecksumPolicy( request.getGlobalChecksumPolicy() );

        // Wagon

        wagonManager.setOnline( !request.isOffline() );

        if ( request.isOffline() )
        {
            getLogger().info( SystemWarnings.getOfflineWarning() );

            if ( request.isOffline() )
            {
                wagonManager.setOnline( false );
            }
            else
            {
                wagonManager.setInteractive( request.isInteractiveMode() );

                wagonManager.addTransferListener( request.getTransferListener() );

                wagonManager.setOnline( true );
            }
        }

        try
        {
            resolveParameters( request.getSettings() );
        }
        catch ( SettingsConfigurationException e )
        {
            throw new MavenEmbedderException( "Unable to configure Maven for execution", e );
        }

        // BaseDirectory in MavenExecutionRequest

        if ( request.getPomFile() != null && request.getBaseDirectory() == null )
        {
            request.setBaseDirectory( new File( request.getPomFile() ) );
        }

        // EventMonitor/Logger

        Logger logger = container.getLoggerManager().getLoggerForComponent( Mojo.ROLE );

        if ( request.getEventMonitors() == null )
        {
            request.addEventMonitor( new DefaultEventMonitor( logger ) );
        }

        container.getLoggerManager().setThreshold( request.getLoggingLevel() );

        return request;
    }

    private void resolveParameters( Settings settings )
        throws SettingsConfigurationException
    {
        Proxy proxy = settings.getActiveProxy();

        if ( proxy != null )
        {
            if ( proxy.getHost() == null )
            {
                throw new SettingsConfigurationException( "Proxy in settings.xml has no host" );
            }

            wagonManager.addProxy( proxy.getProtocol(), proxy.getHost(), proxy.getPort(), proxy.getUsername(), proxy
                .getPassword(), proxy.getNonProxyHosts() );
        }

        for ( Iterator i = settings.getServers().iterator(); i.hasNext(); )
        {
            Server server = (Server) i.next();

            RepositorySettings repoSetting = wagonManager.getRepositorySettings( server.getId() );

            repoSetting.setAuthentication( server.getUsername(), server.getPassword(), server.getPrivateKey(), server
                .getPassphrase() );

            repoSetting
                .setPermissions( /* group */null, server.getFilePermissions(), server.getDirectoryPermissions() );

            if ( server.getConfiguration() != null )
            {
                repoSetting.setConfiguration( new XmlPlexusConfiguration( (Xpp3Dom) server.getConfiguration() ) );
            }
        }

        for ( Iterator i = settings.getMirrors().iterator(); i.hasNext(); )
        {
            Mirror mirror = (Mirror) i.next();

            try
            {
                wagonManager.addRepositoryMirror( mirror.getMirrorOf(), mirror.getId(), mirror.getUrl() );
            }
            catch ( IllegalArgumentException e )
            {
                throw new SettingsConfigurationException(
                    "Unable to configure mirror " + mirror + ": " + e.getMessage(), e );
            }
        }
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
