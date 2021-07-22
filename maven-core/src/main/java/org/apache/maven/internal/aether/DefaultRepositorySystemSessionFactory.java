package org.apache.maven.internal.aether;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.RepositoryUtils;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.bridge.MavenRepositorySystem;
import org.apache.maven.eventspy.internal.EventSpyDispatcher;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.feature.Features;
import org.apache.maven.model.building.TransformerContext;
import org.apache.maven.model.building.TransformerException;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.apache.maven.settings.Mirror;
import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.building.SettingsProblem;
import org.apache.maven.settings.crypto.DefaultSettingsDecryptionRequest;
import org.apache.maven.settings.crypto.SettingsDecrypter;
import org.apache.maven.settings.crypto.SettingsDecryptionResult;
import org.codehaus.plexus.configuration.xml.XmlPlexusConfiguration;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.aether.ConfigurationProperties;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.SessionData;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.NoLocalRepositoryManagerException;
import org.eclipse.aether.repository.RepositoryPolicy;
import org.eclipse.aether.repository.WorkspaceReader;
import org.eclipse.aether.resolution.ResolutionErrorPolicy;
import org.eclipse.aether.spi.localrepo.LocalRepositoryManagerFactory;
import org.eclipse.aether.transform.FileTransformer;
import org.eclipse.aether.transform.TransformException;
import org.eclipse.aether.util.repository.AuthenticationBuilder;
import org.eclipse.aether.util.repository.DefaultAuthenticationSelector;
import org.eclipse.aether.util.repository.DefaultMirrorSelector;
import org.eclipse.aether.util.repository.DefaultProxySelector;
import org.eclipse.aether.util.repository.SimpleResolutionErrorPolicy;
import org.eclipse.sisu.Nullable;

import javax.inject.Inject;
import javax.inject.Named;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

/**
 * @since 3.3.0
 */
@Named
public class DefaultRepositorySystemSessionFactory
{
    @Inject
    private Logger logger;

    @Inject
    private ArtifactHandlerManager artifactHandlerManager;

    @Inject
    private RepositorySystem repoSystem;

    @Inject
    @Nullable
    @Named( "simple" )
    private LocalRepositoryManagerFactory simpleLocalRepoMgrFactory;

    @Inject
    @Nullable
    @Named( "ide" )
    private WorkspaceReader workspaceRepository;

    @Inject
    private SettingsDecrypter settingsDecrypter;

    @Inject
    private EventSpyDispatcher eventSpyDispatcher;

    @Inject
    MavenRepositorySystem mavenRepositorySystem;

    public DefaultRepositorySystemSession newRepositorySession( MavenExecutionRequest request )
    {
        DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
        session.setCache( request.getRepositoryCache() );

        Map<Object, Object> configProps = new LinkedHashMap<>();
        configProps.put( ConfigurationProperties.USER_AGENT, getUserAgent() );
        configProps.put( ConfigurationProperties.INTERACTIVE, request.isInteractiveMode() );
        configProps.put( "maven.startTime", request.getStartTime() );
        configProps.putAll( request.getSystemProperties() );
        configProps.putAll( request.getUserProperties() );

        session.setOffline( request.isOffline() );
        session.setChecksumPolicy( request.getGlobalChecksumPolicy() );
        if ( request.isNoSnapshotUpdates() )
        {
            session.setUpdatePolicy( RepositoryPolicy.UPDATE_POLICY_NEVER );
        }
        else if ( request.isUpdateSnapshots() )
        {
            session.setUpdatePolicy( RepositoryPolicy.UPDATE_POLICY_ALWAYS );
        }
        else
        {
            session.setUpdatePolicy( null );
        }

        int errorPolicy = 0;
        errorPolicy |= request.isCacheNotFound() ? ResolutionErrorPolicy.CACHE_NOT_FOUND
            : ResolutionErrorPolicy.CACHE_DISABLED;
        errorPolicy |= request.isCacheTransferError() ? ResolutionErrorPolicy.CACHE_TRANSFER_ERROR
            : ResolutionErrorPolicy.CACHE_DISABLED;
        session.setResolutionErrorPolicy(
            new SimpleResolutionErrorPolicy( errorPolicy, errorPolicy | ResolutionErrorPolicy.CACHE_NOT_FOUND ) );

        session.setArtifactTypeRegistry( RepositoryUtils.newArtifactTypeRegistry( artifactHandlerManager ) );

        LocalRepository localRepo = new LocalRepository( request.getLocalRepository().getBasedir() );

        if ( request.isUseLegacyLocalRepository() )
        {
            try
            {
                session.setLocalRepositoryManager( simpleLocalRepoMgrFactory.newInstance( session, localRepo ) );
                logger.info( "Disabling enhanced local repository: using legacy is strongly discouraged to ensure"
                                 + " build reproducibility." );
            }
            catch ( NoLocalRepositoryManagerException e )
            {
                logger.error( "Failed to configure legacy local repository: falling back to default" );
                session.setLocalRepositoryManager( repoSystem.newLocalRepositoryManager( session, localRepo ) );
            }
        }
        else
        {
            session.setLocalRepositoryManager( repoSystem.newLocalRepositoryManager( session, localRepo ) );
        }

        if ( request.getWorkspaceReader() != null )
        {
            session.setWorkspaceReader( request.getWorkspaceReader() );
        }
        else
        {
            session.setWorkspaceReader( workspaceRepository );
        }

        DefaultSettingsDecryptionRequest decrypt = new DefaultSettingsDecryptionRequest();
        decrypt.setProxies( request.getProxies() );
        decrypt.setServers( request.getServers() );
        SettingsDecryptionResult decrypted = settingsDecrypter.decrypt( decrypt );

        if ( logger.isDebugEnabled() )
        {
            for ( SettingsProblem problem : decrypted.getProblems() )
            {
                logger.debug( problem.getMessage(), problem.getException() );
            }
        }

        DefaultMirrorSelector mirrorSelector = new DefaultMirrorSelector();
        for ( Mirror mirror : request.getMirrors() )
        {
            mirrorSelector.add( mirror.getId(), mirror.getUrl(), mirror.getLayout(), false, mirror.isBlocked(),
                                mirror.getMirrorOf(), mirror.getMirrorOfLayouts() );
        }
        session.setMirrorSelector( mirrorSelector );

        DefaultProxySelector proxySelector = new DefaultProxySelector();
        for ( Proxy proxy : decrypted.getProxies() )
        {
            AuthenticationBuilder authBuilder = new AuthenticationBuilder();
            authBuilder.addUsername( proxy.getUsername() ).addPassword( proxy.getPassword() );
            proxySelector.add(
                new org.eclipse.aether.repository.Proxy( proxy.getProtocol(), proxy.getHost(), proxy.getPort(),
                                                         authBuilder.build() ), proxy.getNonProxyHosts() );
        }
        session.setProxySelector( proxySelector );

        DefaultAuthenticationSelector authSelector = new DefaultAuthenticationSelector();
        for ( Server server : decrypted.getServers() )
        {
            AuthenticationBuilder authBuilder = new AuthenticationBuilder();
            authBuilder.addUsername( server.getUsername() ).addPassword( server.getPassword() );
            authBuilder.addPrivateKey( server.getPrivateKey(), server.getPassphrase() );
            authSelector.add( server.getId(), authBuilder.build() );

            if ( server.getConfiguration() != null )
            {
                Xpp3Dom dom = (Xpp3Dom) server.getConfiguration();
                for ( int i = dom.getChildCount() - 1; i >= 0; i-- )
                {
                    Xpp3Dom child = dom.getChild( i );
                    if ( "wagonProvider".equals( child.getName() ) )
                    {
                        dom.removeChild( i );
                    }
                }

                XmlPlexusConfiguration config = new XmlPlexusConfiguration( dom );
                configProps.put( "aether.connector.wagon.config." + server.getId(), config );
            }

            configProps.put( "aether.connector.perms.fileMode." + server.getId(), server.getFilePermissions() );
            configProps.put( "aether.connector.perms.dirMode." + server.getId(), server.getDirectoryPermissions() );
        }
        session.setAuthenticationSelector( authSelector );

        session.setTransferListener( request.getTransferListener() );

        session.setRepositoryListener( eventSpyDispatcher.chainListener( new LoggingRepositoryListener( logger ) ) );

        session.setUserProperties( request.getUserProperties() );
        session.setSystemProperties( request.getSystemProperties() );
        session.setConfigProperties( configProps );

        mavenRepositorySystem.injectMirror( request.getRemoteRepositories(), request.getMirrors() );
        mavenRepositorySystem.injectProxy( session, request.getRemoteRepositories() );
        mavenRepositorySystem.injectAuthentication( session, request.getRemoteRepositories() );

        mavenRepositorySystem.injectMirror( request.getPluginArtifactRepositories(), request.getMirrors() );
        mavenRepositorySystem.injectProxy( session, request.getPluginArtifactRepositories() );
        mavenRepositorySystem.injectAuthentication( session, request.getPluginArtifactRepositories() );

        if ( Features.buildConsumer( request.getUserProperties() ).isActive() )
        {
            session.setFileTransformerManager( a -> getTransformersForArtifact( a, session.getData() ) );
        }

        return session;
    }

    private String getUserAgent()
    {
        return "Apache-Maven/" + getMavenVersion() + " (Java " + System.getProperty( "java.version" ) + "; "
            + System.getProperty( "os.name" ) + " " + System.getProperty( "os.version" ) + ")";
    }

    private String getMavenVersion()
    {
        Properties props = new Properties();

        try ( InputStream is = getClass().getResourceAsStream(
            "/META-INF/maven/org.apache.maven/maven-core/pom.properties" ) )
        {
            if ( is != null )
            {
                props.load( is );
            }
        }
        catch ( IOException e )
        {
            logger.debug( "Failed to read Maven version", e );
        }

        return props.getProperty( "version", "unknown-version" );
    }

    private Collection<FileTransformer> getTransformersForArtifact( final Artifact artifact,
                                                                    final SessionData sessionData )
    {
        TransformerContext context = (TransformerContext) sessionData.get( TransformerContext.KEY );
        Collection<FileTransformer> transformers = new ArrayList<>();

        // In case of install:install-file there's no transformer context, as the goal is unrelated to the lifecycle.
        if ( "pom".equals( artifact.getExtension() ) && context != null )
        {
            transformers.add( new FileTransformer()
            {
                @Override
                public InputStream transformData( File pomFile )
                    throws IOException, TransformException
                {
                    try
                    {
                        return new ConsumerModelSourceTransformer().transform( pomFile.toPath(), context );
                    }
                    catch ( TransformerException e )
                    {
                        throw new TransformException( e );
                    }
                }

                @Override
                public Artifact transformArtifact( Artifact artifact )
                {
                    return artifact;
                }
            } );
        }
        return Collections.unmodifiableCollection( transformers );
    }

}
