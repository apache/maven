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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
import org.apache.maven.artifact.resolver.filter.ExcludesArtifactFilter;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Exclusion;
import org.apache.maven.model.Plugin;
import org.apache.maven.repository.Proxy;
import org.apache.maven.settings.Mirror;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.building.SettingsProblem;
import org.apache.maven.settings.crypto.DefaultSettingsDecryptionRequest;
import org.apache.maven.settings.crypto.SettingsDecrypter;
import org.apache.maven.settings.crypto.SettingsDecryptionRequest;
import org.apache.maven.settings.crypto.SettingsDecryptionResult;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.StringUtils;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.impl.ArtifactResolver;
import org.eclipse.aether.repository.AuthenticationContext;
import org.eclipse.aether.repository.AuthenticationSelector;
import org.eclipse.aether.repository.ProxySelector;
import org.eclipse.aether.repository.RemoteRepository;

/**
 * @author Jason van Zyl
 */
@Component( role = MavenRepositorySystem.class, hint = "default" )
public class MavenRepositorySystem
{

    @Requirement
    private Logger logger;

    @Requirement
    private ArtifactHandlerManager artifactHandlerManager;

    @Requirement
    private ArtifactResolver artifactResolver;

    @Requirement( role = ArtifactRepositoryLayout.class )
    private Map<String, ArtifactRepositoryLayout> layouts;

    @Requirement
    private PlexusContainer plexus;

    @Requirement
    private SettingsDecrypter settingsDecrypter;

    // DefaultProjectBuilder
    public Artifact createArtifact( String groupId, String artifactId, String version, String scope, String type )
    {
        return XcreateArtifact( groupId, artifactId, version, scope, type );
    }

    // DefaultProjectBuilder
    public Artifact createProjectArtifact( String groupId, String artifactId, String metaVersionId )
    {
        return XcreateProjectArtifact( groupId, artifactId, metaVersionId );
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
            XcreateDependencyArtifact( d.getGroupId(), d.getArtifactId(), versionRange, d.getType(),
                                                      d.getClassifier(), d.getScope(), d.isOptional() );

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

        return XcreateExtensionArtifact( groupId, artifactId, versionRange );
    }

    // DefaultProjectBuilder
    public Artifact createParentArtifact( String groupId, String artifactId, String version )
    {
        return XcreateParentArtifact( groupId, artifactId, version );
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

        return XcreatePluginArtifact( plugin.getGroupId(), plugin.getArtifactId(), versionRange );
    }

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
            List<ArtifactRepository> mirroredRepos = new ArrayList<ArtifactRepository>();

            List<ArtifactRepositoryPolicy> releasePolicies =
                new ArrayList<ArtifactRepositoryPolicy>( aliasedRepos.size() );

            for ( ArtifactRepository aliasedRepo : aliasedRepos )
            {
                releasePolicies.add( aliasedRepo.getReleases() );
                mirroredRepos.addAll( aliasedRepo.getMirroredRepositories() );
            }

            ArtifactRepositoryPolicy releasePolicy = getEffectivePolicy( releasePolicies );

            List<ArtifactRepositoryPolicy> snapshotPolicies =
                new ArrayList<ArtifactRepositoryPolicy>( aliasedRepos.size() );

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
        return MirrorSelector.getMirror( repository, mirrors );
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

    public static org.apache.maven.model.Repository fromSettingsRepository( org.apache.maven.settings.Repository settingsRepository )
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

    public static org.apache.maven.model.RepositoryPolicy fromSettingsRepositoryPolicy( org.apache.maven.settings.RepositoryPolicy settingsRepositoryPolicy )
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

    public static ArtifactRepositoryPolicy buildArtifactRepositoryPolicy( org.apache.maven.model.RepositoryPolicy policy )
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
    private Artifact XcreateArtifact( String groupId, String artifactId, String version, String scope, String type )
    {
        return XcreateArtifact( groupId, artifactId, version, scope, type, null, null );
    }

    private Artifact XcreateDependencyArtifact( String groupId, String artifactId, VersionRange versionRange,
                                              String type, String classifier, String scope, boolean optional )
    {
        return XcreateArtifact( groupId, artifactId, versionRange, type, classifier, scope, null, optional );
    }

    private Artifact XcreateProjectArtifact( String groupId, String artifactId, String version )
    {
        return XcreateProjectArtifact( groupId, artifactId, version, null );
    }

    private Artifact XcreateParentArtifact( String groupId, String artifactId, String version )
    {
        return XcreateProjectArtifact( groupId, artifactId, version );
    }

    private Artifact XcreatePluginArtifact( String groupId, String artifactId, VersionRange versionRange )
    {
        return XcreateArtifact( groupId, artifactId, versionRange, "maven-plugin", null, Artifact.SCOPE_RUNTIME, null );
    }

    private Artifact XcreateProjectArtifact( String groupId, String artifactId, String version, String scope )
    {
        return XcreateArtifact( groupId, artifactId, version, scope, "pom" );
    }

    private Artifact XcreateExtensionArtifact( String groupId, String artifactId, VersionRange versionRange )
    {
        return XcreateArtifact( groupId, artifactId, versionRange, "jar", null, Artifact.SCOPE_RUNTIME, null );
    }

    private Artifact XcreateArtifact( String groupId, String artifactId, String version, String scope, String type,
                                     String classifier, String inheritedScope )
    {
        VersionRange versionRange = null;
        if ( version != null )
        {
            versionRange = VersionRange.createFromVersion( version );
        }
        return XcreateArtifact( groupId, artifactId, versionRange, type, classifier, scope, inheritedScope );
    }

    private Artifact XcreateArtifact( String groupId, String artifactId, VersionRange versionRange, String type,
                                     String classifier, String scope, String inheritedScope )
    {
        return XcreateArtifact( groupId, artifactId, versionRange, type, classifier, scope, inheritedScope, false );
    }

    private Artifact XcreateArtifact( String groupId, String artifactId, VersionRange versionRange, String type,
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
}
