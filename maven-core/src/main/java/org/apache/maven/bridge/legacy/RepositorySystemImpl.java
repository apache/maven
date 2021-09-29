package org.apache.maven.bridge.legacy;

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

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.InvalidRepositoryException;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.repository.Authentication;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.bridge.MavenRepositorySystem;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.Repository;
import org.apache.maven.repository.ArtifactDoesNotExistException;
import org.apache.maven.repository.ArtifactTransferFailedException;
import org.apache.maven.repository.ArtifactTransferListener;
import org.apache.maven.repository.RepositorySystem;
import org.apache.maven.settings.Mirror;
import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.building.SettingsProblem;
import org.apache.maven.settings.crypto.DefaultSettingsDecryptionRequest;
import org.apache.maven.settings.crypto.SettingsDecrypter;
import org.apache.maven.settings.crypto.SettingsDecryptionRequest;
import org.apache.maven.settings.crypto.SettingsDecryptionResult;
import org.apache.maven.wagon.proxy.ProxyInfo;
import org.apache.maven.wagon.proxy.ProxyUtils;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.StringUtils;
import org.eclipse.aether.RepositorySystemSession;

@Singleton
@Named
public class RepositorySystemImpl implements RepositorySystem
{

    private final Logger logger;
    private final MavenRepositorySystem mrs;
    private final PlexusContainer plexus;
    private final ArtifactResolver artifactResolver;
    private final WagonManager wagonManager;
    private final SettingsDecrypter settingsDecrypter;

    @Inject
    public RepositorySystemImpl(
            Logger logger,
            MavenRepositorySystem mrs,
            PlexusContainer plexus,
            ArtifactResolver artifactResolver,
            WagonManager wagonManager,
            SettingsDecrypter settingsDecrypter )
    {
        this.logger = logger;
        this.mrs = mrs;
        this.plexus = plexus;
        this.artifactResolver = artifactResolver;
        this.wagonManager = wagonManager;
        this.settingsDecrypter = settingsDecrypter;
    }

    @Override
    public Artifact createArtifact( String groupId, String artifactId, String version, String type )
    {
        return mrs.createArtifact( groupId, artifactId, version, null, type );
    }

    @Override
    public Artifact createArtifact( String groupId, String artifactId, String version, String scope, String type )
    {
        return mrs.createArtifact( groupId, artifactId, version, scope, type );
    }

    @Override
    public Artifact createProjectArtifact( String groupId, String artifactId, String version )
    {
        return mrs.createProjectArtifact( groupId, artifactId, version );
    }

    @Override
    public Artifact createArtifactWithClassifier( String groupId, String artifactId, String version,
                                                  String type, String classifier )
    {
        return mrs.createArtifactWithClassifier( groupId, artifactId, version, type, classifier );
    }

    @Override
    public Artifact createPluginArtifact( Plugin plugin )
    {
        return mrs.createPluginArtifact( plugin );
    }

    @Override
    public Artifact createDependencyArtifact( Dependency dependency )
    {
        return mrs.createDependencyArtifact( dependency );
    }

    @Override
    public ArtifactRepository buildArtifactRepository( Repository repository ) throws InvalidRepositoryException
    {
        return MavenRepositorySystem.buildArtifactRepository( repository );
    }

    @Override
    public ArtifactRepository createDefaultRemoteRepository() throws InvalidRepositoryException
    {
        return mrs.createDefaultRemoteRepository( null );
    }

    @Override
    public ArtifactRepository createDefaultLocalRepository() throws InvalidRepositoryException
    {
        return mrs.createLocalRepository( null, RepositorySystem.defaultUserLocalRepository );
    }

    @Override
    public ArtifactRepository createLocalRepository( File localRepository ) throws InvalidRepositoryException
    {
        return mrs.createLocalRepository( null, localRepository );
    }

    @Override
    public ArtifactRepository createArtifactRepository(
            String id, String url, ArtifactRepositoryLayout repositoryLayout,
            ArtifactRepositoryPolicy snapshots, ArtifactRepositoryPolicy releases )
    {
        return MavenRepositorySystem.createArtifactRepository( id, url, repositoryLayout, snapshots, releases );
    }

    @Override
    public List<ArtifactRepository> getEffectiveRepositories( List<ArtifactRepository> repositories )
    {
        return mrs.getEffectiveRepositories( repositories );
    }

    @Override
    public Mirror getMirror( ArtifactRepository repository, List<Mirror> mirrors )
    {
        return MavenRepositorySystem.getMirror( repository, mirrors );
    }

    @Override
    public void injectMirror( List<ArtifactRepository> repositories, List<Mirror> mirrors )
    {
        mrs.injectMirror( repositories, mirrors );
    }

    @Override
    public void injectMirror( RepositorySystemSession session, List<ArtifactRepository> repositories )
    {
        mrs.injectMirror( session, repositories );
    }

    @Override
    public void injectProxy( RepositorySystemSession session, List<ArtifactRepository> repositories )
    {
        mrs.injectProxy( session, repositories );
    }

    @Override
    public void injectAuthentication( RepositorySystemSession session, List<ArtifactRepository> repositories )
    {
        mrs.injectAuthentication( session, repositories );
    }

    @Override
    @Deprecated
    public void injectProxy( List<ArtifactRepository> repositories, List<Proxy> proxies )
    {
        if ( repositories != null )
        {
            for ( ArtifactRepository repository : repositories )
            {
                org.apache.maven.settings.Proxy proxy = getProxy( repository, proxies );

                if ( proxy != null )
                {
                    SettingsDecryptionRequest request = new DefaultSettingsDecryptionRequest( proxy );
                    SettingsDecryptionResult result = settingsDecrypter.decrypt( request );
                    proxy = result.getProxy();

                    if ( logger.isDebugEnabled() )
                    {
                        for ( SettingsProblem problem : result.getProblems() )
                        {
                            logger.debug( problem.getMessage(), problem.getException() );
                        }
                    }

                    org.apache.maven.repository.Proxy p = new org.apache.maven.repository.Proxy();
                    p.setHost( proxy.getHost() );
                    p.setProtocol( proxy.getProtocol() );
                    p.setPort( proxy.getPort() );
                    p.setNonProxyHosts( proxy.getNonProxyHosts() );
                    p.setUserName( proxy.getUsername() );
                    p.setPassword( proxy.getPassword() );

                    repository.setProxy( p );
                }
                else
                {
                    repository.setProxy( null );
                }
            }
        }
    }

    private org.apache.maven.settings.Proxy getProxy( ArtifactRepository repository,
                                                      List<org.apache.maven.settings.Proxy> proxies )
    {
        if ( proxies != null && repository.getProtocol() != null )
        {
            for ( org.apache.maven.settings.Proxy proxy : proxies )
            {
                if ( proxy.isActive() && repository.getProtocol().equalsIgnoreCase( proxy.getProtocol() ) )
                {
                    if ( StringUtils.isNotEmpty( proxy.getNonProxyHosts() ) )
                    {
                        ProxyInfo pi = new ProxyInfo();
                        pi.setNonProxyHosts( proxy.getNonProxyHosts() );

                        org.apache.maven.wagon.repository.Repository repo =
                                new org.apache.maven.wagon.repository.Repository(
                                        repository.getId(), repository.getUrl() );

                        if ( !ProxyUtils.validateNonProxyHosts( pi, repo.getHost() ) )
                        {
                            return proxy;
                        }
                    }
                    else
                    {
                        return proxy;
                    }
                }
            }
        }

        return null;
    }

    @Override
    @Deprecated
    public void injectAuthentication( List<ArtifactRepository> repositories, List<Server> servers )
    {
        if ( repositories != null )
        {
            Map<String, Server> serversById = new HashMap<>();

            if ( servers != null )
            {
                for ( Server server : servers )
                {
                    if ( !serversById.containsKey( server.getId() ) )
                    {
                        serversById.put( server.getId(), server );
                    }
                }
            }

            for ( ArtifactRepository repository : repositories )
            {
                Server server = serversById.get( repository.getId() );

                if ( server != null )
                {
                    SettingsDecryptionRequest request = new DefaultSettingsDecryptionRequest( server );
                    SettingsDecryptionResult result = settingsDecrypter.decrypt( request );
                    server = result.getServer();

                    if ( logger.isDebugEnabled() )
                    {
                        for ( SettingsProblem problem : result.getProblems() )
                        {
                            logger.debug( problem.getMessage(), problem.getException() );
                        }
                    }

                    Authentication authentication = new Authentication( server.getUsername(), server.getPassword() );
                    authentication.setPrivateKey( server.getPrivateKey() );
                    authentication.setPassphrase( server.getPassphrase() );

                    repository.setAuthentication( authentication );
                }
                else
                {
                    repository.setAuthentication( null );
                }
            }
        }
    }

    @Override
    @Deprecated
    public ArtifactResolutionResult resolve( ArtifactResolutionRequest request )
    {
        return artifactResolver.resolve( request );
    }

    @Override
    @Deprecated
    public void publish(
            ArtifactRepository repository,
            File source,
            String remotePath,
            ArtifactTransferListener transferListener )
        throws ArtifactTransferFailedException
    {
        try
        {
            wagonManager.putRemoteFile( repository, source, remotePath,
                    TransferListenerAdapter.newAdapter( transferListener ) );
        }
        catch ( org.apache.maven.wagon.TransferFailedException e )
        {
            throw new ArtifactTransferFailedException( getMessage( e, "Error transferring artifact." ), e );
        }
    }

    @Override
    @Deprecated
    public void retrieve(
            ArtifactRepository repository,
            File destination,
            String remotePath,
            ArtifactTransferListener transferListener )
        throws ArtifactTransferFailedException, ArtifactDoesNotExistException
    {
        try
        {
            wagonManager.getRemoteFile( repository, destination, remotePath,
                    TransferListenerAdapter.newAdapter( transferListener ),
                    ArtifactRepositoryPolicy.CHECKSUM_POLICY_WARN, true );
        }
        catch ( org.apache.maven.wagon.TransferFailedException e )
        {
            throw new ArtifactTransferFailedException( getMessage( e, "Error transferring artifact." ), e );
        }
        catch ( org.apache.maven.wagon.ResourceDoesNotExistException e )
        {
            throw new ArtifactDoesNotExistException( getMessage( e, "Requested artifact does not exist." ), e );
        }
    }

    private static String getMessage( Throwable error, String def )
    {
        if ( error == null )
        {
            return def;
        }
        String msg = error.getMessage();
        if ( StringUtils.isNotEmpty( msg ) )
        {
            return msg;
        }
        return getMessage( error.getCause(), def );
    }

}
