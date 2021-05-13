package org.apache.maven.repository.legacy;

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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.RepositoryUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.InvalidRepositoryException;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.repository.legacy.repository.ArtifactRepositoryFactory;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.repository.Authentication;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.resolver.filter.ExcludesArtifactFilter;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Exclusion;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.Repository;
import org.apache.maven.model.RepositoryPolicy;
import org.apache.maven.repository.DelegatingLocalArtifactRepository;
import org.apache.maven.repository.LocalArtifactRepository;
import org.apache.maven.repository.ArtifactTransferListener;
import org.apache.maven.repository.MirrorSelector;
import org.apache.maven.repository.Proxy;
import org.apache.maven.repository.RepositorySystem;
import org.apache.maven.repository.ArtifactDoesNotExistException;
import org.apache.maven.repository.ArtifactTransferFailedException;
import org.apache.maven.settings.Mirror;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.building.SettingsProblem;
import org.apache.maven.settings.crypto.DefaultSettingsDecryptionRequest;
import org.apache.maven.settings.crypto.SettingsDecrypter;
import org.apache.maven.settings.crypto.SettingsDecryptionRequest;
import org.apache.maven.settings.crypto.SettingsDecryptionResult;
import org.apache.maven.wagon.proxy.ProxyInfo;
import org.apache.maven.wagon.proxy.ProxyUtils;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.StringUtils;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.AuthenticationContext;
import org.eclipse.aether.repository.AuthenticationSelector;
import org.eclipse.aether.repository.ProxySelector;
import org.eclipse.aether.repository.RemoteRepository;

/**
 * @author Jason van Zyl
 */
@Component( role = RepositorySystem.class, hint = "default" )
public class LegacyRepositorySystem
    implements RepositorySystem
{

    @Requirement
    private Logger logger;

    @Requirement
    private ArtifactFactory artifactFactory;

    @Requirement
    private ArtifactResolver artifactResolver;

    @Requirement
    private ArtifactRepositoryFactory artifactRepositoryFactory;

    @Requirement( role = ArtifactRepositoryLayout.class )
    private Map<String, ArtifactRepositoryLayout> layouts;

    @Requirement
    private WagonManager wagonManager;

    @Requirement
    private PlexusContainer plexus;

    @Requirement
    private MirrorSelector mirrorSelector;

    @Requirement
    private SettingsDecrypter settingsDecrypter;

    public Artifact createArtifact( String groupId, String artifactId, String version, String scope, String type )
    {
        return artifactFactory.createArtifact( groupId, artifactId, version, scope, type );
    }

    public Artifact createArtifact( String groupId, String artifactId, String version, String packaging )
    {
        return artifactFactory.createBuildArtifact( groupId, artifactId, version, packaging );
    }

    public Artifact createArtifactWithClassifier( String groupId, String artifactId, String version, String type,
                                                  String classifier )
    {
        return artifactFactory.createArtifactWithClassifier( groupId, artifactId, version, type, classifier );
    }

    public Artifact createProjectArtifact( String groupId, String artifactId, String metaVersionId )
    {
        return artifactFactory.createProjectArtifact( groupId, artifactId, metaVersionId );
    }

    public Artifact createDependencyArtifact( Dependency d )
    {
        VersionRange versionRange;
        try
        {
            versionRange = VersionRange.createFromVersionSpec( d.getVersion() );
        }
        catch ( InvalidVersionSpecificationException e )
        {
            // MNG-5368: Log a message instead of returning 'null' silently.
            this.logger.error( String.format( "Invalid version specification '%s' creating dependency artifact '%s'.",
                                              d.getVersion(), d ), e );
            return null;
        }

        Artifact artifact =
            artifactFactory.createDependencyArtifact( d.getGroupId(), d.getArtifactId(), versionRange, d.getType(),
                                                      d.getClassifier(), d.getScope(), d.isOptional() );

        if ( Artifact.SCOPE_SYSTEM.equals( d.getScope() ) && d.getSystemPath() != null )
        {
            artifact.setFile( new File( d.getSystemPath() ) );
        }

        if ( !d.getExclusions().isEmpty() )
        {
            List<String> exclusions = new ArrayList<>();

            for ( Exclusion exclusion : d.getExclusions() )
            {
                exclusions.add( exclusion.getGroupId() + ':' + exclusion.getArtifactId() );
            }

            artifact.setDependencyFilter( new ExcludesArtifactFilter( exclusions ) );
        }

        return artifact;
    }

    public Artifact createExtensionArtifact( String groupId, String artifactId, String version )
    {
        VersionRange versionRange;
        try
        {
            versionRange = VersionRange.createFromVersionSpec( version );
        }
        catch ( InvalidVersionSpecificationException e )
        {
            // MNG-5368: Log a message instead of returning 'null' silently.
            this.logger.error( String.format(
                "Invalid version specification '%s' creating extension artifact '%s:%s:%s'.",
                version, groupId, artifactId, version ), e );

            return null;
        }

        return artifactFactory.createExtensionArtifact( groupId, artifactId, versionRange );
    }

    public Artifact createParentArtifact( String groupId, String artifactId, String version )
    {
        return artifactFactory.createParentArtifact( groupId, artifactId, version );
    }

    public Artifact createPluginArtifact( Plugin plugin )
    {
        String version = plugin.getVersion();
        if ( StringUtils.isEmpty( version ) )
        {
            version = "RELEASE";
        }

        VersionRange versionRange;
        try
        {
            versionRange = VersionRange.createFromVersionSpec( version );
        }
        catch ( InvalidVersionSpecificationException e )
        {
            // MNG-5368: Log a message instead of returning 'null' silently.
            this.logger.error( String.format(
                "Invalid version specification '%s' creating plugin artifact '%s'.",
                version, plugin ), e );

            return null;
        }

        return artifactFactory.createPluginArtifact( plugin.getGroupId(), plugin.getArtifactId(), versionRange );
    }

    public ArtifactRepositoryPolicy buildArtifactRepositoryPolicy( RepositoryPolicy policy )
    {
        boolean enabled = true;

        String updatePolicy = null;

        String checksumPolicy = null;

        if ( policy != null )
        {
            enabled = policy.isEnabled();

            if ( policy.getUpdatePolicy() != null )
            {
                updatePolicy = policy.getUpdatePolicy();
            }
            if ( policy.getChecksumPolicy() != null )
            {
                checksumPolicy = policy.getChecksumPolicy();
            }
        }

        return new ArtifactRepositoryPolicy( enabled, updatePolicy, checksumPolicy );
    }

    public ArtifactRepository createDefaultLocalRepository()
        throws InvalidRepositoryException
    {
        return createLocalRepository( RepositorySystem.defaultUserLocalRepository );
    }

    public ArtifactRepository createLocalRepository( File localRepository )
        throws InvalidRepositoryException
    {
        return createRepository( "file://" + localRepository.toURI().getRawPath(),
                                 RepositorySystem.DEFAULT_LOCAL_REPO_ID, true,
                                 ArtifactRepositoryPolicy.UPDATE_POLICY_ALWAYS, true,
                                 ArtifactRepositoryPolicy.UPDATE_POLICY_ALWAYS,
                                 ArtifactRepositoryPolicy.CHECKSUM_POLICY_IGNORE );
    }

    public ArtifactRepository createDefaultRemoteRepository()
        throws InvalidRepositoryException
    {
        return createRepository( RepositorySystem.DEFAULT_REMOTE_REPO_URL, RepositorySystem.DEFAULT_REMOTE_REPO_ID,
                                 true, ArtifactRepositoryPolicy.UPDATE_POLICY_DAILY, false,
                                 ArtifactRepositoryPolicy.UPDATE_POLICY_DAILY,
                                 ArtifactRepositoryPolicy.CHECKSUM_POLICY_WARN );
    }

    public ArtifactRepository createLocalRepository( String url, String repositoryId )
        throws IOException
    {
        return createRepository( canonicalFileUrl( url ), repositoryId, true,
                                 ArtifactRepositoryPolicy.UPDATE_POLICY_ALWAYS, true,
                                 ArtifactRepositoryPolicy.UPDATE_POLICY_ALWAYS,
                                 ArtifactRepositoryPolicy.CHECKSUM_POLICY_IGNORE );
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

    public ArtifactResolutionResult resolve( ArtifactResolutionRequest request )
    {
        /*
         * Probably is not worth it, but here I make sure I restore request
         * to its original state.
         */
        try
        {
            LocalArtifactRepository ideWorkspace =
                plexus.lookup( LocalArtifactRepository.class, LocalArtifactRepository.IDE_WORKSPACE );

            if ( request.getLocalRepository() instanceof DelegatingLocalArtifactRepository )
            {
                DelegatingLocalArtifactRepository delegatingLocalRepository =
                    (DelegatingLocalArtifactRepository) request.getLocalRepository();

                LocalArtifactRepository orig = delegatingLocalRepository.getIdeWorkspace();

                delegatingLocalRepository.setIdeWorkspace( ideWorkspace );

                try
                {
                    return artifactResolver.resolve( request );
                }
                finally
                {
                    delegatingLocalRepository.setIdeWorkspace( orig );
                }
            }
            else
            {
                ArtifactRepository localRepository = request.getLocalRepository();
                DelegatingLocalArtifactRepository delegatingLocalRepository =
                    new DelegatingLocalArtifactRepository( localRepository );
                delegatingLocalRepository.setIdeWorkspace( ideWorkspace );
                request.setLocalRepository( delegatingLocalRepository );
                try
                {
                    return artifactResolver.resolve( request );
                }
                finally
                {
                    request.setLocalRepository( localRepository );
                }
            }
        }
        catch ( ComponentLookupException e )
        {
            // no ide workspace artifact resolution
        }

        return artifactResolver.resolve( request );
    }

//    public void addProxy( String protocol, String host, int port, String username, String password,
//                          String nonProxyHosts )
//    {
//        ProxyInfo proxyInfo = new ProxyInfo();
//        proxyInfo.setHost( host );
//        proxyInfo.setType( protocol );
//        proxyInfo.setPort( port );
//        proxyInfo.setNonProxyHosts( nonProxyHosts );
//        proxyInfo.setUserName( username );
//        proxyInfo.setPassword( password );
//
//        proxies.put( protocol, proxyInfo );
//
//        wagonManager.addProxy( protocol, host, port, username, password, nonProxyHosts );
//    }

    public List<ArtifactRepository> getEffectiveRepositories( List<ArtifactRepository> repositories )
    {
        if ( repositories == null )
        {
            return null;
        }

        Map<String, List<ArtifactRepository>> reposByKey = new LinkedHashMap<>();

        for ( ArtifactRepository repository : repositories )
        {
            String key = repository.getId();

            List<ArtifactRepository> aliasedRepos = reposByKey.computeIfAbsent( key, k -> new ArrayList<>() );

            aliasedRepos.add( repository );
        }

        List<ArtifactRepository> effectiveRepositories = new ArrayList<>();

        for ( List<ArtifactRepository> aliasedRepos : reposByKey.values() )
        {
            List<ArtifactRepository> mirroredRepos = new ArrayList<>();

            List<ArtifactRepositoryPolicy> releasePolicies =
                new ArrayList<>( aliasedRepos.size() );

            for ( ArtifactRepository aliasedRepo : aliasedRepos )
            {
                releasePolicies.add( aliasedRepo.getReleases() );
                mirroredRepos.addAll( aliasedRepo.getMirroredRepositories() );
            }

            ArtifactRepositoryPolicy releasePolicy = getEffectivePolicy( releasePolicies );

            List<ArtifactRepositoryPolicy> snapshotPolicies =
                new ArrayList<>( aliasedRepos.size() );

            for ( ArtifactRepository aliasedRepo : aliasedRepos )
            {
                snapshotPolicies.add( aliasedRepo.getSnapshots() );
            }

            ArtifactRepositoryPolicy snapshotPolicy = getEffectivePolicy( snapshotPolicies );

            ArtifactRepository aliasedRepo = aliasedRepos.get( 0 );

            ArtifactRepository effectiveRepository =
                createArtifactRepository( aliasedRepo.getId(), aliasedRepo.getUrl(), aliasedRepo.getLayout(),
                                          snapshotPolicy, releasePolicy );

            effectiveRepository.setAuthentication( aliasedRepo.getAuthentication() );

            effectiveRepository.setProxy( aliasedRepo.getProxy() );

            effectiveRepository.setMirroredRepositories( mirroredRepos );

            effectiveRepository.setBlocked( aliasedRepo.isBlocked() );

            effectiveRepositories.add( effectiveRepository );
        }

        return effectiveRepositories;
    }

    private ArtifactRepositoryPolicy getEffectivePolicy( Collection<ArtifactRepositoryPolicy> policies )
    {
        ArtifactRepositoryPolicy effectivePolicy = null;

        for ( ArtifactRepositoryPolicy policy : policies )
        {
            if ( effectivePolicy == null )
            {
                effectivePolicy = new ArtifactRepositoryPolicy( policy );
            }
            else
            {
                effectivePolicy.merge( policy );
            }
        }

        return effectivePolicy;
    }

    public Mirror getMirror( ArtifactRepository repository, List<Mirror> mirrors )
    {
        return mirrorSelector.getMirror( repository, mirrors );
    }

    public void injectMirror( List<ArtifactRepository> repositories, List<Mirror> mirrors )
    {
        if ( repositories != null && mirrors != null )
        {
            for ( ArtifactRepository repository : repositories )
            {
                Mirror mirror = getMirror( repository, mirrors );
                injectMirror( repository, mirror );
            }
        }
    }

    private Mirror getMirror( RepositorySystemSession session, ArtifactRepository repository )
    {
        if ( session != null )
        {
            org.eclipse.aether.repository.MirrorSelector selector = session.getMirrorSelector();
            if ( selector != null )
            {
                RemoteRepository repo = selector.getMirror( RepositoryUtils.toRepo( repository ) );
                if ( repo != null )
                {
                    Mirror mirror = new Mirror();
                    mirror.setId( repo.getId() );
                    mirror.setUrl( repo.getUrl() );
                    mirror.setLayout( repo.getContentType() );
                    mirror.setBlocked( repo.isBlocked() );
                    return mirror;
                }
            }
        }
        return null;
    }

    public void injectMirror( RepositorySystemSession session, List<ArtifactRepository> repositories )
    {
        if ( repositories != null && session != null )
        {
            for ( ArtifactRepository repository : repositories )
            {
                Mirror mirror = getMirror( session, repository );
                injectMirror( repository, mirror );
            }
        }
    }

    private void injectMirror( ArtifactRepository repository, Mirror mirror )
    {
        if ( mirror != null )
        {
            ArtifactRepository original =
                createArtifactRepository( repository.getId(), repository.getUrl(), repository.getLayout(),
                                          repository.getSnapshots(), repository.getReleases() );

            repository.setMirroredRepositories( Collections.singletonList( original ) );

            repository.setId( mirror.getId() );
            repository.setUrl( mirror.getUrl() );

            if ( StringUtils.isNotEmpty( mirror.getLayout() ) )
            {
                repository.setLayout( getLayout( mirror.getLayout() ) );
            }

            repository.setBlocked( mirror.isBlocked() );
        }
    }

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

    private Authentication getAuthentication( RepositorySystemSession session, ArtifactRepository repository )
    {
        if ( session != null )
        {
            AuthenticationSelector selector = session.getAuthenticationSelector();
            if ( selector != null )
            {
                RemoteRepository repo = RepositoryUtils.toRepo( repository );
                org.eclipse.aether.repository.Authentication auth = selector.getAuthentication( repo );
                if ( auth != null )
                {
                    repo = new RemoteRepository.Builder( repo ).setAuthentication( auth ).build();
                    AuthenticationContext authCtx = AuthenticationContext.forRepository( session, repo );
                    Authentication result =
                        new Authentication( authCtx.get( AuthenticationContext.USERNAME ),
                                            authCtx.get( AuthenticationContext.PASSWORD ) );
                    result.setPrivateKey( authCtx.get( AuthenticationContext.PRIVATE_KEY_PATH ) );
                    result.setPassphrase( authCtx.get( AuthenticationContext.PRIVATE_KEY_PASSPHRASE ) );
                    authCtx.close();
                    return result;
                }
            }
        }
        return null;
    }

    public void injectAuthentication( RepositorySystemSession session, List<ArtifactRepository> repositories )
    {
        if ( repositories != null && session != null )
        {
            for ( ArtifactRepository repository : repositories )
            {
                repository.setAuthentication( getAuthentication( session, repository ) );
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
                            new org.apache.maven.wagon.repository.Repository( repository.getId(), repository.getUrl() );

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

    public void injectProxy( List<ArtifactRepository> repositories, List<org.apache.maven.settings.Proxy> proxies )
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

                    Proxy p = new Proxy();
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

    private Proxy getProxy( RepositorySystemSession session, ArtifactRepository repository )
    {
        if ( session != null )
        {
            ProxySelector selector = session.getProxySelector();
            if ( selector != null )
            {
                RemoteRepository repo = RepositoryUtils.toRepo( repository );
                org.eclipse.aether.repository.Proxy proxy = selector.getProxy( repo );
                if ( proxy != null )
                {
                    Proxy p = new Proxy();
                    p.setHost( proxy.getHost() );
                    p.setProtocol( proxy.getType() );
                    p.setPort( proxy.getPort() );
                    if ( proxy.getAuthentication() != null )
                    {
                        repo = new RemoteRepository.Builder( repo ).setProxy( proxy ).build();
                        AuthenticationContext authCtx = AuthenticationContext.forProxy( session, repo );
                        p.setUserName( authCtx.get( AuthenticationContext.USERNAME ) );
                        p.setPassword( authCtx.get( AuthenticationContext.PASSWORD ) );
                        p.setNtlmDomain( authCtx.get( AuthenticationContext.NTLM_DOMAIN ) );
                        p.setNtlmHost( authCtx.get( AuthenticationContext.NTLM_WORKSTATION ) );
                        authCtx.close();
                    }
                    return p;
                }
            }
        }
        return null;
    }

    public void injectProxy( RepositorySystemSession session, List<ArtifactRepository> repositories )
    {
        if ( repositories != null && session != null )
        {
            for ( ArtifactRepository repository : repositories )
            {
                repository.setProxy( getProxy( session, repository ) );
            }
        }
    }

    public void retrieve( ArtifactRepository repository, File destination, String remotePath,
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

    public void publish( ArtifactRepository repository, File source, String remotePath,
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

    //
    // Artifact Repository Creation
    //
    public ArtifactRepository buildArtifactRepository( Repository repo )
        throws InvalidRepositoryException
    {
        if ( repo != null )
        {
            String id = repo.getId();

            if ( StringUtils.isEmpty( id ) )
            {
                throw new InvalidRepositoryException( "Repository identifier missing", "" );
            }

            String url = repo.getUrl();

            if ( StringUtils.isEmpty( url ) )
            {
                throw new InvalidRepositoryException( "URL missing for repository " + id, id );
            }

            ArtifactRepositoryPolicy snapshots = buildArtifactRepositoryPolicy( repo.getSnapshots() );

            ArtifactRepositoryPolicy releases = buildArtifactRepositoryPolicy( repo.getReleases() );

            return createArtifactRepository( id, url, getLayout( repo.getLayout() ), snapshots, releases );
        }
        else
        {
            return null;
        }
    }

    private ArtifactRepository createRepository( String url, String repositoryId, boolean releases,
                                                 String releaseUpdates, boolean snapshots, String snapshotUpdates,
                                                 String checksumPolicy )
    {
        ArtifactRepositoryPolicy snapshotsPolicy =
            new ArtifactRepositoryPolicy( snapshots, snapshotUpdates, checksumPolicy );

        ArtifactRepositoryPolicy releasesPolicy =
            new ArtifactRepositoryPolicy( releases, releaseUpdates, checksumPolicy );

        return createArtifactRepository( repositoryId, url, null, snapshotsPolicy, releasesPolicy );
    }

    public ArtifactRepository createArtifactRepository( String repositoryId, String url,
                                                        ArtifactRepositoryLayout repositoryLayout,
                                                        ArtifactRepositoryPolicy snapshots,
                                                        ArtifactRepositoryPolicy releases )
    {
        if ( repositoryLayout == null )
        {
            repositoryLayout = layouts.get( "default" );
        }

        ArtifactRepository artifactRepository =
            artifactRepositoryFactory.createArtifactRepository( repositoryId, url, repositoryLayout, snapshots,
                                                                releases );

        return artifactRepository;
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

    private ArtifactRepositoryLayout getLayout( String id )
    {
        ArtifactRepositoryLayout layout = layouts.get( id );

        if ( layout == null )
        {
            layout = new UnknownRepositoryLayout( id, layouts.get( "default" ) );
        }

        return layout;
    }

    /**
     * In the future, the legacy system might encounter repository types for which no layout components exists because
     * the actual communication with the repository happens via a repository connector. As a minimum, the legacy system
     * needs to retain the id of this layout so that the content type of the remote repository can still be accurately
     * described.
     */
    static class UnknownRepositoryLayout
        implements ArtifactRepositoryLayout
    {

        private final String id;

        private final ArtifactRepositoryLayout fallback;

        UnknownRepositoryLayout( String id, ArtifactRepositoryLayout fallback )
        {
            this.id = id;
            this.fallback = fallback;
        }

        public String getId()
        {
            return id;
        }

        public String pathOf( Artifact artifact )
        {
            return fallback.pathOf( artifact );
        }

        public String pathOfLocalRepositoryMetadata( ArtifactMetadata metadata, ArtifactRepository repository )
        {
            return fallback.pathOfLocalRepositoryMetadata( metadata, repository );
        }

        public String pathOfRemoteRepositoryMetadata( ArtifactMetadata metadata )
        {
            return fallback.pathOfRemoteRepositoryMetadata( metadata );
        }

        @Override
        public String toString()
        {
            return getId();
        }

    }

}
