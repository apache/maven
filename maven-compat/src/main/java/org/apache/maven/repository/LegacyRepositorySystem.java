package org.apache.maven.repository;

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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.InvalidRepositoryException;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryFactory;
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
import org.apache.maven.repository.MetadataResolutionRequest;
import org.apache.maven.repository.MetadataResolutionResult;
import org.apache.maven.repository.RepositorySystem;
import org.apache.maven.repository.legacy.WagonManager;
import org.apache.maven.settings.Mirror;
import org.apache.maven.settings.Server;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.events.TransferListener;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.util.StringUtils;

/**
 * @author Jason van Zyl
 */
@Component(role = RepositorySystem.class, hint = "default")
public class LegacyRepositorySystem
    implements RepositorySystem
{
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

    public Artifact createArtifact( String groupId, String artifactId, String version, String scope, String type )
    {
        return artifactFactory.createArtifact( groupId, artifactId, version, scope, type );
    }

    public Artifact createArtifact( String groupId, String artifactId, String version, String packaging )
    {
        return artifactFactory.createBuildArtifact( groupId, artifactId, version, packaging );
    }

    public Artifact createArtifactWithClassifier( String groupId, String artifactId, String version, String type, String classifier )
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
            return null;
        }

        Artifact artifact = artifactFactory.createDependencyArtifact( d.getGroupId(), d.getArtifactId(), versionRange, d.getType(), d.getClassifier(), d.getScope(), d.isOptional() );

        if ( Artifact.SCOPE_SYSTEM.equals( d.getScope() ) && d.getSystemPath() != null )
        {
            artifact.setFile( new File( d.getSystemPath() ) );
        }

        if ( !d.getExclusions().isEmpty() )
        {
            List<String> exclusions = new ArrayList<String>();

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
        VersionRange versionRange;
        try
        {
            versionRange = VersionRange.createFromVersionSpec( plugin.getVersion() );
        }
        catch ( InvalidVersionSpecificationException e )
        {
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
        return createRepository( "file://" + localRepository.toURI().getRawPath(), RepositorySystem.DEFAULT_LOCAL_REPO_ID, true, ArtifactRepositoryPolicy.UPDATE_POLICY_ALWAYS, true,
                                 ArtifactRepositoryPolicy.UPDATE_POLICY_ALWAYS, ArtifactRepositoryPolicy.CHECKSUM_POLICY_IGNORE );
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
        return createRepository( canonicalFileUrl( url ), repositoryId, true, ArtifactRepositoryPolicy.UPDATE_POLICY_ALWAYS, true, ArtifactRepositoryPolicy.UPDATE_POLICY_ALWAYS,
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
            LocalArtifactRepository ideWorkspace = plexus.lookup( LocalArtifactRepository.class, LocalArtifactRepository.IDE_WORKSPACE );

            if ( request.getLocalRepository() instanceof DelegatingLocalArtifactRepository )
            {
                DelegatingLocalArtifactRepository delegatingLocalRepository = (DelegatingLocalArtifactRepository) request.getLocalRepository();

                LocalArtifactRepository orig = delegatingLocalRepository.getIdeWorspace();

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
                DelegatingLocalArtifactRepository delegatingLocalRepository = new DelegatingLocalArtifactRepository( localRepository );
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

    /*
    public void addProxy( String protocol, String host, int port, String username, String password, String nonProxyHosts )
    {
        ProxyInfo proxyInfo = new ProxyInfo();
        proxyInfo.setHost( host );
        proxyInfo.setType( protocol );
        proxyInfo.setPort( port );
        proxyInfo.setNonProxyHosts( nonProxyHosts );
        proxyInfo.setUserName( username );
        proxyInfo.setPassword( password );

        proxies.put( protocol, proxyInfo );

        wagonManager.addProxy( protocol, host, port, username, password, nonProxyHosts );
    }
    */

    public List<ArtifactRepository> getEffectiveRepositories( List<ArtifactRepository> repositories )
    {
        if ( repositories == null )
        {
            return null;
        }

        Map<String, List<ArtifactRepository>> reposByKey = new LinkedHashMap<String, List<ArtifactRepository>>();

        for ( ArtifactRepository repository : repositories )
        {
            String key = repository.getId();

            List<ArtifactRepository> aliasedRepos = reposByKey.get( key );

            if ( aliasedRepos == null )
            {
                aliasedRepos = new ArrayList<ArtifactRepository>();
                reposByKey.put( key, aliasedRepos );
            }

            aliasedRepos.add( repository );
        }

        List<ArtifactRepository> effectiveRepositories = new ArrayList<ArtifactRepository>();

        for ( List<ArtifactRepository> aliasedRepos : reposByKey.values() )
        {
            List<ArtifactRepositoryPolicy> releasePolicies = new ArrayList<ArtifactRepositoryPolicy>( aliasedRepos.size() );

            for ( ArtifactRepository aliasedRepo : aliasedRepos )
            {
                releasePolicies.add( aliasedRepo.getReleases() );
            }

            ArtifactRepositoryPolicy releasePolicy = getEffectivePolicy( releasePolicies );

            List<ArtifactRepositoryPolicy> snapshotPolicies = new ArrayList<ArtifactRepositoryPolicy>( aliasedRepos.size() );

            for ( ArtifactRepository aliasedRepo : aliasedRepos )
            {
                snapshotPolicies.add( aliasedRepo.getSnapshots() );
            }

            ArtifactRepositoryPolicy snapshotPolicy = getEffectivePolicy( snapshotPolicies );

            ArtifactRepository aliasedRepo = aliasedRepos.get( 0 );

            ArtifactRepository effectiveRepository = 
                createArtifactRepository( aliasedRepo.getId(), aliasedRepo.getUrl(), aliasedRepo.getLayout(), snapshotPolicy, releasePolicy );

            effectiveRepository.setAuthentication( aliasedRepo.getAuthentication() );

            effectiveRepository.setProxy( aliasedRepo.getProxy() );

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
                effectivePolicy = new ArtifactRepositoryPolicy( policy.isEnabled(), policy.getUpdatePolicy(), policy.getChecksumPolicy() );
            }
            else
            {
                if ( policy.isEnabled() )
                {
                    effectivePolicy.setEnabled( true );

                    if ( ordinalOfChecksumPolicy( policy.getChecksumPolicy() ) < ordinalOfChecksumPolicy( effectivePolicy.getChecksumPolicy() ) )
                    {
                        effectivePolicy.setChecksumPolicy( policy.getChecksumPolicy() );
                    }

                    if ( ordinalOfUpdatePolicy( policy.getUpdatePolicy() ) < ordinalOfUpdatePolicy( effectivePolicy.getUpdatePolicy() ) )
                    {
                        effectivePolicy.setUpdatePolicy( policy.getUpdatePolicy() );
                    }
                }
            }
        }

        return effectivePolicy;
    }

    private int ordinalOfChecksumPolicy( String policy )
    {
        if ( ArtifactRepositoryPolicy.CHECKSUM_POLICY_FAIL.equals( policy ) )
        {
            return 2;
        }
        else if ( ArtifactRepositoryPolicy.CHECKSUM_POLICY_IGNORE.equals( policy ) )
        {
            return 0;
        }
        else
        {
            return 1;
        }
    }

    private int ordinalOfUpdatePolicy( String policy )
    {
        if ( ArtifactRepositoryPolicy.UPDATE_POLICY_DAILY.equals( policy ) )
        {
            return 1440;
        }
        else if ( ArtifactRepositoryPolicy.UPDATE_POLICY_ALWAYS.equals( policy ) )
        {
            return 0;
        }
        else if ( policy != null && policy.startsWith( ArtifactRepositoryPolicy.UPDATE_POLICY_INTERVAL ) )
        {
            return 60;
        }
        else
        {
            return Integer.MAX_VALUE;
        }
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

                if ( mirror != null )
                {
                    repository.setId( mirror.getId() );
                    repository.setUrl( mirror.getUrl() );
                }
            }
        }
    }

    public void injectAuthentication( List<ArtifactRepository> repositories, List<Server> servers )
    {
        if ( repositories != null )
        {
            Map<String, Server> serversById = new HashMap<String, Server>();

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
                    Authentication authentication = new Authentication( server.getUsername(), server.getPassword() );

                    repository.setAuthentication( authentication );
                }
                else
                {
                    repository.setAuthentication( null );
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
                    return proxy;
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

    public MetadataResolutionResult resolveMetadata( MetadataResolutionRequest request )
    {

        //      ArtifactResolutionResult collect( Set<Artifact> artifacts,
        //      Artifact originatingArtifact,
        //      Map managedVersions,
        //      ArtifactRepository localRepository,
        //      List<ArtifactRepository> remoteRepositories,
        //      ArtifactMetadataSource source,
        //      ArtifactFilter filter,
        //      List<ResolutionListener> listeners,
        //      List<ConflictResolver> conflictResolvers )

        //        ArtifactResolutionResult result = artifactCollector.
        return null;
    }

    public void retrieve( ArtifactRepository repository, File destination, String remotePath, TransferListener downloadMonitor )
        throws TransferFailedException, ResourceDoesNotExistException
    {
        wagonManager.getRemoteFile( repository, destination, remotePath, downloadMonitor, ArtifactRepositoryPolicy.CHECKSUM_POLICY_WARN, true );
    }

    public void publish( ArtifactRepository repository, File source, String remotePath, TransferListener downloadMonitor )
        throws TransferFailedException
    {
        wagonManager.putRemoteFile( repository, source, remotePath, downloadMonitor );
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
            
            return createArtifactRepository( id, url, layouts.get( repo.getLayout() ), snapshots, releases );
        }
        else
        {
            return null;
        }
    }
    
    private ArtifactRepository createRepository( String url, String repositoryId, boolean releases, String releaseUpdates, boolean snapshots, String snapshotUpdates, String checksumPolicy )
    {
        ArtifactRepositoryPolicy snapshotsPolicy = new ArtifactRepositoryPolicy( snapshots, snapshotUpdates, checksumPolicy );

        ArtifactRepositoryPolicy releasesPolicy = new ArtifactRepositoryPolicy( releases, releaseUpdates, checksumPolicy );

        return createArtifactRepository( repositoryId, url, null, snapshotsPolicy, releasesPolicy );
    }

    public ArtifactRepository createArtifactRepository( String repositoryId, String url, ArtifactRepositoryLayout repositoryLayout, ArtifactRepositoryPolicy snapshots, ArtifactRepositoryPolicy releases )
    {
        if ( repositoryLayout == null )
        {
            repositoryLayout = layouts.get( "default" );
        }

        ArtifactRepository artifactRepository = artifactRepositoryFactory.createArtifactRepository( repositoryId, url, repositoryLayout, snapshots, releases );

        return artifactRepository;
    }

}
