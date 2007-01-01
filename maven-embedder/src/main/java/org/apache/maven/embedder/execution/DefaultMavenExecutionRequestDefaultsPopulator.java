package org.apache.maven.embedder.execution;

import java.io.File;
import java.util.Iterator;

import org.apache.maven.MavenTools;
import org.apache.maven.SettingsConfigurationException;
import org.apache.maven.monitor.event.DefaultEventMonitor;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Mirror;
import org.apache.maven.usability.SystemWarnings;
import org.apache.maven.artifact.repository.ArtifactRepositoryFactory;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.manager.WagonManager;
import org.apache.maven.embedder.MavenEmbedderException;
import org.apache.maven.execution.MavenExecutionRequest;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.component.repository.exception.ComponentLifecycleException;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;
import org.codehaus.plexus.context.ContextException;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.util.xml.Xpp3Dom;

public class DefaultMavenExecutionRequestDefaultsPopulator
    extends AbstractLogEnabled
	implements MavenExecutionRequestDefaultsPopulator, Contextualizable
{
	private MavenTools mavenTools;
	
	private ArtifactRepositoryFactory artifactRepositoryFactory;

    private PlexusContainer container;

    public MavenExecutionRequest populateDefaults(MavenExecutionRequest request)
        throws MavenEmbedderException
    {
		// Settings

        if ( request.getSettings() == null )
        {
            // A local repository set in the request should win over what's in a settings.xml file.

            File userSettingsPath = mavenTools.getUserSettingsPath( request.getSettingsFile() );

            File globalSettingsFile = mavenTools.getGlobalSettingsPath();

            try
            {
                request.setSettings( mavenTools.buildSettings( userSettingsPath, globalSettingsFile, request.isInteractiveMode(),
                                                         request.isOffline(), request.isUsePluginRegistry(),
                                                         request.isUsePluginUpdateOverride() ) );
            }
            catch ( SettingsConfigurationException e )
            {
                throw new MavenEmbedderException( "Error processing settings.xml.", e );
            }
        }

        // Local repository
        
        if ( request.getLocalRepository() == null )
        {
            String localRepositoryPath = mavenTools.getLocalRepositoryPath( request.getSettings() );

            if ( request.getLocalRepository() == null )
            {
                request.setLocalRepository( mavenTools.createLocalRepository( new File( localRepositoryPath ) ) );
            }                        
        }
        
        // Repository update policies
        
        boolean snapshotPolicySet = false;

        if ( request.isOffline() )
        {
            snapshotPolicySet = true;
        }

        if ( !snapshotPolicySet ) {
            if ( request.isUpdateSnapshots() )
            {
                artifactRepositoryFactory.setGlobalUpdatePolicy( ArtifactRepositoryPolicy.UPDATE_POLICY_ALWAYS );
            }
            else if ( request.isNoSnapshotUpdates() )
            {
                getLogger().info( "+ Supressing SNAPSHOT updates.");
                artifactRepositoryFactory.setGlobalUpdatePolicy( ArtifactRepositoryPolicy.UPDATE_POLICY_NEVER );
            }
        }

        artifactRepositoryFactory.setGlobalChecksumPolicy( request.getGlobalChecksumPolicy() );        

        // Wagon        

        if ( request.getSettings().isOffline() )
        {
            getLogger().info( SystemWarnings.getOfflineWarning() );

            WagonManager wagonManager = null;

            try
            {
                wagonManager = (WagonManager) container.lookup( WagonManager.ROLE );

                if ( request.isOffline() )
                {
                    wagonManager.setOnline( false );
                }
                else
                {
                    wagonManager.setInteractive( request.isInteractiveMode() );

                    wagonManager.setDownloadMonitor( request.getTransferListener() );

                    wagonManager.setOnline( true );
                }
            }
            catch ( ComponentLookupException e )
            {
                throw new MavenEmbedderException( "Cannot retrieve WagonManager in order to set offline mode.", e );
            }
            finally
            {
                try
                {
                    container.release( wagonManager );
                }
                catch ( ComponentLifecycleException e )
                {
                    getLogger().warn( "Cannot release WagonManager.", e );
                }
            }
        }

        try
        {
            resolveParameters( request.getSettings() );
        }
        catch ( ComponentLookupException e )
        {
            throw new MavenEmbedderException( "Unable to configure Maven for execution", e );
        }
        catch ( ComponentLifecycleException e )
        {
            throw new MavenEmbedderException( "Unable to configure Maven for execution", e );
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

                wagonManager.addProxy( proxy.getProtocol(), proxy.getHost(), proxy.getPort(), proxy.getUsername(),
                                       proxy.getPassword(), proxy.getNonProxyHosts() );
            }

            for ( Iterator i = settings.getServers().iterator(); i.hasNext(); )
            {
                Server server = (Server) i.next();

                wagonManager.addAuthenticationInfo( server.getId(), server.getUsername(), server.getPassword(),
                                                    server.getPrivateKey(), server.getPassphrase() );

                wagonManager.addPermissionInfo( server.getId(), server.getFilePermissions(),
                                                server.getDirectoryPermissions() );

                if ( server.getConfiguration() != null )
                {
                    wagonManager.addConfiguration( server.getId(), (Xpp3Dom) server.getConfiguration() );
                }
            }

            for ( Iterator i = settings.getMirrors().iterator(); i.hasNext(); )
            {
                Mirror mirror = (Mirror) i.next();

                wagonManager.addMirror( mirror.getId(), mirror.getMirrorOf(), mirror.getUrl() );
            }
        }
        finally
        {
            container.release( wagonManager );
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
