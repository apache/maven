package org.apache.maven.bridge;

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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.RepositoryUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.InvalidRepositoryException;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.repository.Authentication;
import org.apache.maven.artifact.repository.MavenArtifactRepository;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout2;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.apache.maven.artifact.resolver.filter.ExclusionArtifactFilter;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.repository.Proxy;
import org.apache.maven.repository.RepositorySystem;
import org.apache.maven.settings.Mirror;
import org.codehaus.plexus.util.StringUtils;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.AuthenticationContext;
import org.eclipse.aether.repository.AuthenticationSelector;
import org.eclipse.aether.repository.ProxySelector;
import org.eclipse.aether.repository.RemoteRepository;

/**
 * @author Jason van Zyl
 */
@Named( "default" )
@Singleton
public class MavenRepositorySystem
{
    private final ArtifactHandlerManager artifactHandlerManager;

    private final Map<String, ArtifactRepositoryLayout> layouts;

    @Inject
    public MavenRepositorySystem( ArtifactHandlerManager artifactHandlerManager,
            Map<String, ArtifactRepositoryLayout> layouts )
    {
        this.artifactHandlerManager = artifactHandlerManager;
        this.layouts = layouts;
    }

    // DefaultProjectBuilder
    public Artifact createArtifact( String groupId, String artifactId, String version, String scope, String type )
    {
        return createArtifactX( groupId, artifactId, version, scope, type );
    }

    // DefaultProjectBuilder
    public Artifact createProjectArtifact( String groupId, String artifactId, String metaVersionId )
    {
        return createProjectArtifactX( groupId, artifactId, metaVersionId );
    }

    // DefaultProjectBuilder
    public Artifact createDependencyArtifact( Dependency d )
    {
        if ( d.getVersion() == null )
        {
            return null;
        }

        VersionRange versionRange;
        try
        {
            versionRange = VersionRange.createFromVersionSpec( d.getVersion() );
        }
        catch ( InvalidVersionSpecificationException e )
        {
            return null;
        }

        Artifact artifact =
            createDependencyArtifactX( d.getGroupId(), d.getArtifactId(), versionRange, d.getType(),
                                                      d.getClassifier(), d.getScope(), d.isOptional() );

        if ( Artifact.SCOPE_SYSTEM.equals( d.getScope() ) && d.getSystemPath() != null )
        {
            artifact.setFile( new File( d.getSystemPath() ) );
        }

        if ( !d.getExclusions().isEmpty() )
        {
            artifact.setDependencyFilter( new ExclusionArtifactFilter( d.getExclusions() ) );
        }

        return artifact;
    }

    // DefaultProjectBuilder
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

        return createExtensionArtifactX( groupId, artifactId, versionRange );
    }

    // DefaultProjectBuilder
    public Artifact createParentArtifact( String groupId, String artifactId, String version )
    {
        return createParentArtifactX( groupId, artifactId, version );
    }

    // DefaultProjectBuilder
    public Artifact createPluginArtifact( Plugin plugin )
    {
        VersionRange versionRange;
        try
        {
            String version = plugin.getVersion();
            if ( StringUtils.isEmpty( version ) )
            {
                version = "RELEASE";
            }
            versionRange = VersionRange.createFromVersionSpec( version );
        }
        catch ( InvalidVersionSpecificationException e )
        {
            return null;
        }

        return createPluginArtifactX( plugin.getGroupId(), plugin.getArtifactId(), versionRange );
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

    private ArtifactRepositoryLayout getLayout( String id )
    {
        ArtifactRepositoryLayout layout = layouts.get( id );

        return layout;
    }


    //
    // Taken from LegacyRepositorySystem
    //

    public static org.apache.maven.model.Repository fromSettingsRepository( org.apache.maven.settings.Repository
                                                                            settingsRepository )
    {
        org.apache.maven.model.Repository modelRepository = new org.apache.maven.model.Repository();
        modelRepository.setId( settingsRepository.getId() );
        modelRepository.setLayout( settingsRepository.getLayout() );
        modelRepository.setName( settingsRepository.getName() );
        modelRepository.setUrl( settingsRepository.getUrl() );
        modelRepository.setReleases( fromSettingsRepositoryPolicy( settingsRepository.getReleases() ) );
        modelRepository.setSnapshots( fromSettingsRepositoryPolicy( settingsRepository.getSnapshots() ) );
        return modelRepository;
    }

    public static org.apache.maven.model.RepositoryPolicy fromSettingsRepositoryPolicy(
                                                 org.apache.maven.settings.RepositoryPolicy settingsRepositoryPolicy )
    {
        org.apache.maven.model.RepositoryPolicy modelRepositoryPolicy = new org.apache.maven.model.RepositoryPolicy();
        if ( settingsRepositoryPolicy != null )
        {
            modelRepositoryPolicy.setEnabled( settingsRepositoryPolicy.isEnabled() );
            modelRepositoryPolicy.setUpdatePolicy( settingsRepositoryPolicy.getUpdatePolicy() );
            modelRepositoryPolicy.setChecksumPolicy( settingsRepositoryPolicy.getChecksumPolicy() );
        }
        return modelRepositoryPolicy;
    }

    public static ArtifactRepository buildArtifactRepository( org.apache.maven.settings.Repository repo )
        throws InvalidRepositoryException
    {
        return buildArtifactRepository( fromSettingsRepository( repo ) );
    }

    public static ArtifactRepository buildArtifactRepository( org.apache.maven.model.Repository repo )
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

            ArtifactRepositoryLayout layout = new DefaultRepositoryLayout();

            return createArtifactRepository( id, url, layout, snapshots, releases );
        }
        else
        {
            return null;
        }
    }

    public static ArtifactRepositoryPolicy buildArtifactRepositoryPolicy( org.apache.maven.model.RepositoryPolicy
                                                                          policy )
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

    public ArtifactRepository createArtifactRepository( String id, String url, String layoutId,
                                                        ArtifactRepositoryPolicy snapshots,
                                                        ArtifactRepositoryPolicy releases )
        throws Exception
    {
        ArtifactRepositoryLayout layout = layouts.get( layoutId );

        checkLayout( id, layoutId, layout );

        return createArtifactRepository( id, url, layout, snapshots, releases );
    }

    private void checkLayout( String repositoryId, String layoutId, ArtifactRepositoryLayout layout )
        throws Exception
    {
        if ( layout == null )
        {
            throw new Exception( String.format( "Cannot find ArtifactRepositoryLayout instance for: %s %s", layoutId,
                                                repositoryId ) );
        }
    }

    public static ArtifactRepository createArtifactRepository( String id, String url,
                                                        ArtifactRepositoryLayout repositoryLayout,
                                                        ArtifactRepositoryPolicy snapshots,
                                                        ArtifactRepositoryPolicy releases )
    {
        if ( snapshots == null )
        {
            snapshots = new ArtifactRepositoryPolicy();
        }

        if ( releases == null )
        {
            releases = new ArtifactRepositoryPolicy();
        }

        ArtifactRepository repository;
        if ( repositoryLayout instanceof ArtifactRepositoryLayout2 )
        {
            repository =
                ( (ArtifactRepositoryLayout2) repositoryLayout ).newMavenArtifactRepository( id, url, snapshots,
                                                                                             releases );
        }
        else
        {
            repository = new MavenArtifactRepository( id, url, repositoryLayout, snapshots, releases );
        }

        return repository;
    }

    // ArtifactFactory
    private Artifact createArtifactX( String groupId, String artifactId, String version, String scope, String type )
    {
        return createArtifactX( groupId, artifactId, version, scope, type, null, null );
    }

    private Artifact createDependencyArtifactX( String groupId, String artifactId, VersionRange versionRange,
                                               String type, String classifier, String scope, boolean optional )
    {
        return createArtifactX( groupId, artifactId, versionRange, type, classifier, scope, null, optional );
    }

    private Artifact createProjectArtifactX( String groupId, String artifactId, String version )
    {
        return createProjectArtifactX( groupId, artifactId, version, null );
    }

    private Artifact createParentArtifactX( String groupId, String artifactId, String version )
    {
        return createProjectArtifactX( groupId, artifactId, version );
    }

    private Artifact createPluginArtifactX( String groupId, String artifactId, VersionRange versionRange )
    {
        return createArtifactX( groupId, artifactId, versionRange, "maven-plugin", null, Artifact.SCOPE_RUNTIME, null );
    }

    private Artifact createProjectArtifactX( String groupId, String artifactId, String version, String scope )
    {
        return createArtifactX( groupId, artifactId, version, scope, "pom" );
    }

    private Artifact createExtensionArtifactX( String groupId, String artifactId, VersionRange versionRange )
    {
        return createArtifactX( groupId, artifactId, versionRange, "jar", null, Artifact.SCOPE_RUNTIME, null );
    }

    private Artifact createArtifactX( String groupId, String artifactId, String version, String scope, String type,
                                     String classifier, String inheritedScope )
    {
        VersionRange versionRange = null;
        if ( version != null )
        {
            versionRange = VersionRange.createFromVersion( version );
        }
        return createArtifactX( groupId, artifactId, versionRange, type, classifier, scope, inheritedScope );
    }

    private Artifact createArtifactX( String groupId, String artifactId, VersionRange versionRange, String type,
                                     String classifier, String scope, String inheritedScope )
    {
        return createArtifactX( groupId, artifactId, versionRange, type, classifier, scope, inheritedScope, false );
    }

    @SuppressWarnings( "checkstyle:parameternumber" )
    private Artifact createArtifactX( String groupId, String artifactId, VersionRange versionRange, String type,
                                     String classifier, String scope, String inheritedScope, boolean optional )
    {
        String desiredScope = Artifact.SCOPE_RUNTIME;

        if ( inheritedScope == null )
        {
            desiredScope = scope;
        }
        else if ( Artifact.SCOPE_TEST.equals( scope ) || Artifact.SCOPE_PROVIDED.equals( scope ) )
        {
            return null;
        }
        else if ( Artifact.SCOPE_COMPILE.equals( scope ) && Artifact.SCOPE_COMPILE.equals( inheritedScope ) )
        {
            // added to retain compile artifactScope. Remove if you want compile inherited as runtime
            desiredScope = Artifact.SCOPE_COMPILE;
        }

        if ( Artifact.SCOPE_TEST.equals( inheritedScope ) )
        {
            desiredScope = Artifact.SCOPE_TEST;
        }

        if ( Artifact.SCOPE_PROVIDED.equals( inheritedScope ) )
        {
            desiredScope = Artifact.SCOPE_PROVIDED;
        }

        if ( Artifact.SCOPE_SYSTEM.equals( scope ) )
        {
            // system scopes come through unchanged...
            desiredScope = Artifact.SCOPE_SYSTEM;
        }

        ArtifactHandler handler = artifactHandlerManager.getArtifactHandler( type );

        return new DefaultArtifact( groupId, artifactId, versionRange, desiredScope, type, classifier, handler,
                                    optional );
    }

    //
    // Code taken from LegacyRepositorySystem
    //

    public ArtifactRepository createDefaultRemoteRepository( MavenExecutionRequest request )
        throws Exception
    {
        return createRepository( RepositorySystem.DEFAULT_REMOTE_REPO_URL, RepositorySystem.DEFAULT_REMOTE_REPO_ID,
                                 true, ArtifactRepositoryPolicy.UPDATE_POLICY_DAILY, false,
                                 ArtifactRepositoryPolicy.UPDATE_POLICY_DAILY,
                                 ArtifactRepositoryPolicy.DEFAULT_CHECKSUM_POLICY );
    }

    public ArtifactRepository createRepository( String url, String repositoryId, boolean releases,
                                                 String releaseUpdates, boolean snapshots, String snapshotUpdates,
                                                 String checksumPolicy ) throws Exception
    {
        ArtifactRepositoryPolicy snapshotsPolicy =
            new ArtifactRepositoryPolicy( snapshots, snapshotUpdates, checksumPolicy );

        ArtifactRepositoryPolicy releasesPolicy =
            new ArtifactRepositoryPolicy( releases, releaseUpdates, checksumPolicy );

        return createArtifactRepository( repositoryId, url, "default", snapshotsPolicy, releasesPolicy );
    }

    public Set<String> getRepoIds( List<ArtifactRepository> repositories )
    {
        Set<String> repoIds = new HashSet<>();

        if ( repositories != null )
        {
            for ( ArtifactRepository repository : repositories )
            {
                repoIds.add( repository.getId() );
            }
        }

        return repoIds;
    }

    /**
     * Source from org.apache.maven.repository.legacy.LegacyRepositorySystem#getEffectiveRepositories
     *
     * @param repositories
     * @return
     * @since 3.6.1
     */
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

    public ArtifactRepository createLocalRepository( MavenExecutionRequest request, File localRepository )
        throws Exception
    {
        return createRepository( "file://" + localRepository.toURI().getRawPath(),
                                 RepositorySystem.DEFAULT_LOCAL_REPO_ID, true,
                                 ArtifactRepositoryPolicy.UPDATE_POLICY_ALWAYS, true,
                                 ArtifactRepositoryPolicy.UPDATE_POLICY_ALWAYS,
                                 ArtifactRepositoryPolicy.CHECKSUM_POLICY_IGNORE );
    }

    private static final String WILDCARD = "*";

    private static final String EXTERNAL_WILDCARD = "external:*";

    private static final String EXTERNAL_HTTP_WILDCARD = "external:http:*";

    public static Mirror getMirror( ArtifactRepository repository, List<Mirror> mirrors )
    {
        String repoId = repository.getId();

        if ( repoId != null && mirrors != null )
        {
            for ( Mirror mirror : mirrors )
            {
                if ( repoId.equals( mirror.getMirrorOf() ) && matchesLayout( repository, mirror ) )
                {
                    return mirror;
                }
            }

            for ( Mirror mirror : mirrors )
            {
                if ( matchPattern( repository, mirror.getMirrorOf() ) && matchesLayout( repository, mirror ) )
                {
                    return mirror;
                }
            }
        }

        return null;
    }

    /**
     * This method checks if the pattern matches the originalRepository. Valid patterns:
     * <ul>
     * <li>{@code *} = everything,</li>
     * <li>{@code external:*} = everything not on the localhost and not file based,</li>
     * <li>{@code external:http:*} = any repository not on the localhost using HTTP,</li>
     * <li>{@code repo,repo1} = {@code repo} or {@code repo1},</li>
     * <li>{@code *,!repo1} = everything except {@code repo1}.</li>
     * </ul>
     *
     * @param originalRepository to compare for a match.
     * @param pattern used for match.
     * @return true if the repository is a match to this pattern.
     */
    static boolean matchPattern( ArtifactRepository originalRepository, String pattern )
    {
        boolean result = false;
        String originalId = originalRepository.getId();

        // simple checks first to short circuit processing below.
        if ( WILDCARD.equals( pattern ) || pattern.equals( originalId ) )
        {
            result = true;
        }
        else
        {
            // process the list
            String[] repos = pattern.split( "," );
            for ( String repo : repos )
            {
                // see if this is a negative match
                if ( repo.length() > 1 && repo.startsWith( "!" ) )
                {
                    if ( repo.substring( 1 ).equals( originalId ) )
                    {
                        // explicitly exclude. Set result and stop processing.
                        result = false;
                        break;
                    }
                }
                // check for exact match
                else if ( repo.equals( originalId ) )
                {
                    result = true;
                    break;
                }
                // check for external:*
                else if ( EXTERNAL_WILDCARD.equals( repo ) && isExternalRepo( originalRepository ) )
                {
                    result = true;
                    // don't stop processing in case a future segment explicitly excludes this repo
                }
                // check for external:http:*
                else if ( EXTERNAL_HTTP_WILDCARD.equals( repo ) && isExternalHttpRepo( originalRepository ) )
                {
                    result = true;
                    // don't stop processing in case a future segment explicitly excludes this repo
                }
                else if ( WILDCARD.equals( repo ) )
                {
                    result = true;
                    // don't stop processing in case a future segment explicitly excludes this repo
                }
            }
        }
        return result;
    }

    /**
     * Checks the URL to see if this repository refers to an external repository
     *
     * @param originalRepository
     * @return true if external.
     */
    static boolean isExternalRepo( ArtifactRepository originalRepository )
    {
        try
        {
            URL url = new URL( originalRepository.getUrl() );
            return !( isLocal( url.getHost() ) || url.getProtocol().equals( "file" ) );
        }
        catch ( MalformedURLException e )
        {
            // bad url just skip it here. It should have been validated already, but the wagon lookup will deal with it
            return false;
        }
    }

    private static boolean isLocal( String host )
    {
        return "localhost".equals( host ) || "127.0.0.1".equals( host );
    }

    /**
     * Checks the URL to see if this repository refers to a non-localhost repository using HTTP.
     *
     * @param originalRepository
     * @return true if external.
     */
    static boolean isExternalHttpRepo( ArtifactRepository originalRepository )
    {
        try
        {
            URL url = new URL( originalRepository.getUrl() );
            return ( "http".equalsIgnoreCase( url.getProtocol() ) || "dav".equalsIgnoreCase( url.getProtocol() )
                || "dav:http".equalsIgnoreCase( url.getProtocol() )
                || "dav+http".equalsIgnoreCase( url.getProtocol() ) ) && !isLocal( url.getHost() );
        }
        catch ( MalformedURLException e )
        {
            // bad url just skip it here. It should have been validated already, but the wagon lookup will deal with it
            return false;
        }
    }

    static boolean matchesLayout( ArtifactRepository repository, Mirror mirror )
    {
        return matchesLayout( RepositoryUtils.getLayout( repository ), mirror.getMirrorOfLayouts() );
    }

    /**
     * Checks whether the layouts configured for a mirror match with the layout of the repository.
     *
     * @param repoLayout The layout of the repository, may be {@code null}.
     * @param mirrorLayout The layouts supported by the mirror, may be {@code null}.
     * @return {@code true} if the layouts associated with the mirror match the layout of the original repository,
     *         {@code false} otherwise.
     */
    static boolean matchesLayout( String repoLayout, String mirrorLayout )
    {
        boolean result = false;

        // simple checks first to short circuit processing below.
        if ( StringUtils.isEmpty( mirrorLayout ) || WILDCARD.equals( mirrorLayout ) )
        {
            result = true;
        }
        else if ( mirrorLayout.equals( repoLayout ) )
        {
            result = true;
        }
        else
        {
            // process the list
            String[] layouts = mirrorLayout.split( "," );
            for ( String layout : layouts )
            {
                // see if this is a negative match
                if ( layout.length() > 1 && layout.startsWith( "!" ) )
                {
                    if ( layout.substring( 1 ).equals( repoLayout ) )
                    {
                        // explicitly exclude. Set result and stop processing.
                        result = false;
                        break;
                    }
                }
                // check for exact match
                else if ( layout.equals( repoLayout ) )
                {
                    result = true;
                    break;
                }
                else if ( WILDCARD.equals( layout ) )
                {
                    result = true;
                    // don't stop processing in case a future segment explicitly excludes this repo
                }
            }
        }

        return result;
    }
}
