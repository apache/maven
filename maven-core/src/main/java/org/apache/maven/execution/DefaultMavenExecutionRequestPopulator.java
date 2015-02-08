package org.apache.maven.execution;

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

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.maven.RepositoryUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.InvalidRepositoryException;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.repository.MavenArtifactRepository;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout2;
import org.apache.maven.bridge.MavenRepositorySystem;
import org.apache.maven.repository.RepositorySystem;
//
// All of this needs to go away and be couched in terms of the execution request
//
import org.apache.maven.settings.Mirror;
import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.Repository;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.SettingsUtils;
//
// Settings in core
//
import org.apache.maven.toolchain.model.PersistedToolchains;
import org.apache.maven.toolchain.model.ToolchainModel;
import org.codehaus.plexus.util.StringUtils;

@Named
public class DefaultMavenExecutionRequestPopulator
    implements MavenExecutionRequestPopulator
{

    private static final String WILDCARD = "*";

    private static final String EXTERNAL_WILDCARD = "external:*";

    private final Map<String, ArtifactRepositoryLayout> layouts;
        
    @Inject
    public DefaultMavenExecutionRequestPopulator( RepositorySystem repositorySystem, Map<String, ArtifactRepositoryLayout> layouts )
    {
        this.layouts = layouts;
    }

    @Override
    public MavenExecutionRequest populateFromSettings( MavenExecutionRequest request, Settings settings )
        throws MavenExecutionRequestPopulationException
    {
        if ( settings == null )
        {
            return request;
        }

        request.setOffline( settings.isOffline() );

        request.setInteractiveMode( settings.isInteractiveMode() );

        request.setPluginGroups( settings.getPluginGroups() );

        request.setLocalRepositoryPath( settings.getLocalRepository() );

        for ( Server server : settings.getServers() )
        {
            server = server.clone();

            request.addServer( server );
        }

        //  <proxies>
        //    <proxy>
        //      <active>true</active>
        //      <protocol>http</protocol>
        //      <host>proxy.somewhere.com</host>
        //      <port>8080</port>
        //      <username>proxyuser</username>
        //      <password>somepassword</password>
        //      <nonProxyHosts>www.google.com|*.somewhere.com</nonProxyHosts>
        //    </proxy>
        //  </proxies>

        for ( Proxy proxy : settings.getProxies() )
        {
            if ( !proxy.isActive() )
            {
                continue;
            }

            proxy = proxy.clone();

            request.addProxy( proxy );
        }

        // <mirrors>
        //   <mirror>
        //     <id>nexus</id>
        //     <mirrorOf>*</mirrorOf>
        //     <url>http://repository.sonatype.org/content/groups/public</url>
        //   </mirror>
        // </mirrors>

        for ( Mirror mirror : settings.getMirrors() )
        {
            mirror = mirror.clone();

            request.addMirror( mirror );
        }

        request.setActiveProfiles( settings.getActiveProfiles() );

        for ( org.apache.maven.settings.Profile rawProfile : settings.getProfiles() )
        {
            request.addProfile( SettingsUtils.convertFromSettingsProfile( rawProfile ) );

            if ( settings.getActiveProfiles().contains( rawProfile.getId() ) )
            {
                List<Repository> remoteRepositories = rawProfile.getRepositories();
                for ( Repository remoteRepository : remoteRepositories )
                {
                    try
                    {
                        request.addRemoteRepository( MavenRepositorySystem.buildArtifactRepository( remoteRepository ) );
                    }
                    catch ( InvalidRepositoryException e )
                    {
                        // do nothing for now
                    }
                }

                List<Repository> pluginRepositories = rawProfile.getPluginRepositories();
                for ( Repository pluginRepository : pluginRepositories )
                {
                    try
                    {
                        request.addPluginArtifactRepository( MavenRepositorySystem.buildArtifactRepository( pluginRepository ) );
                    }
                    catch ( InvalidRepositoryException e )
                    {
                        // do nothing for now
                    }
                }
            }
        }

        return request;
    }

    @Override
    public MavenExecutionRequest populateFromToolchains( MavenExecutionRequest request, PersistedToolchains toolchains )
        throws MavenExecutionRequestPopulationException
    {
        if ( toolchains != null )
        {
            Map<String, List<ToolchainModel>> groupedToolchains = new HashMap<String, List<ToolchainModel>>( 2 );

            for ( ToolchainModel model : toolchains.getToolchains() )
            {
                if ( !groupedToolchains.containsKey( model.getType() ) )
                {
                    groupedToolchains.put( model.getType(), new ArrayList<ToolchainModel>() );
                }

                groupedToolchains.get( model.getType() ).add( model );
            }

            request.setToolchains( groupedToolchains );
        }
        return request;
    }
    
    @Override
    public MavenExecutionRequest populateDefaults( MavenExecutionRequest request )
        throws MavenExecutionRequestPopulationException
    {
        baseDirectory( request );

        localRepository( request );

        populateDefaultPluginGroups( request );

        injectDefaultRepositories( request );

        injectDefaultPluginRepositories( request );

        processRepositoriesInSettings( request );

        return request;
    }
    
    //
    //
    //
    
    private void populateDefaultPluginGroups( MavenExecutionRequest request )
    {
        request.addPluginGroup( "org.apache.maven.plugins" );
        request.addPluginGroup( "org.codehaus.mojo" );
    }

    private void injectDefaultRepositories( MavenExecutionRequest request )
        throws MavenExecutionRequestPopulationException
    {
        Set<String> definedRepositories = getRepoIds( request.getRemoteRepositories() );

        if ( !definedRepositories.contains( RepositorySystem.DEFAULT_REMOTE_REPO_ID ) )
        {
            try
            {
                request.addRemoteRepository( createDefaultRemoteRepository( request ) );
            }
            catch ( Exception e )
            {
                throw new MavenExecutionRequestPopulationException( "Cannot create default remote repository.", e );
            }
        }
    }

    private void injectDefaultPluginRepositories( MavenExecutionRequest request )
        throws MavenExecutionRequestPopulationException
    {
        Set<String> definedRepositories = getRepoIds( request.getPluginArtifactRepositories() );

        if ( !definedRepositories.contains( RepositorySystem.DEFAULT_REMOTE_REPO_ID ) )
        {
            try
            {
                request.addPluginArtifactRepository( createDefaultRemoteRepository( request ) );
            }
            catch ( Exception e )
            {
                throw new MavenExecutionRequestPopulationException( "Cannot create default remote repository.", e );
            }
        }
    }

    private ArtifactRepository createDefaultRemoteRepository( MavenExecutionRequest request )
        throws Exception
    {
        return createRepository( request, RepositorySystem.DEFAULT_REMOTE_REPO_URL, RepositorySystem.DEFAULT_REMOTE_REPO_ID,
                                 true, ArtifactRepositoryPolicy.UPDATE_POLICY_DAILY, false,
                                 ArtifactRepositoryPolicy.UPDATE_POLICY_DAILY,
                                 ArtifactRepositoryPolicy.CHECKSUM_POLICY_WARN );
    }
    
    private ArtifactRepository createRepository( MavenExecutionRequest request, String url, String repositoryId, boolean releases,
                                                 String releaseUpdates, boolean snapshots, String snapshotUpdates,
                                                 String checksumPolicy ) throws Exception
    {
        ArtifactRepositoryPolicy snapshotsPolicy =
            new ArtifactRepositoryPolicy( snapshots, snapshotUpdates, checksumPolicy );

        ArtifactRepositoryPolicy releasesPolicy =
            new ArtifactRepositoryPolicy( releases, releaseUpdates, checksumPolicy );

        return createArtifactRepository( repositoryId, url, "default", snapshotsPolicy, releasesPolicy, request );
    }
        
    private Set<String> getRepoIds( List<ArtifactRepository> repositories )
    {
        Set<String> repoIds = new HashSet<String>();

        if ( repositories != null )
        {
            for ( ArtifactRepository repository : repositories )
            {
                repoIds.add( repository.getId() );
            }
        }

        return repoIds;
    }

    private void processRepositoriesInSettings( MavenExecutionRequest request )
        throws MavenExecutionRequestPopulationException
    {
        //
        //    <settings>
        //      <mirrors>
        //        <mirror>
        //          <id>central</id>
        //          <!-- NOTE: We need to try and use the proper host name/ip as Java generally ignores proxies for "localhost" -->
        //          <url>http://10.0.1.34:62247/</url>
        //          <mirrorOf>central</mirrorOf>
        //        </mirror>
        //      </mirrors>
        //      <proxies>
        //        <proxy>
        //          <active>true</active>
        //          <protocol>http</protocol>
        //          <host>localhost</host>
        //          <port>62248</port>
        //          <nonProxyHosts>10.0.1.34</nonProxyHosts>
        //        </proxy>
        //      </proxies>
        //      <profiles>
        //        <profile>
        //          <id>it-defaults</id>
        //          <!-- disable central override and use built-in values -->
        //        </profile>
        //      </profiles>
        //      <activeProfiles>
        //        <activeProfile>it-defaults</activeProfile>
        //      </activeProfiles>
        //    </settings>
        //
        // Turns
        //
        // http://repo1.maven.org/maven2
        //
        // to
        //
        // http://10.0.1.34:62247/
        //
        // Not sure why the DefaultMirrorSelector doesn't do this...
        //
        injectMirror( request, request.getRemoteRepositories(), request.getMirrors() );
        injectMirror( request, request.getPluginArtifactRepositories(), request.getMirrors() );
    }

    private void localRepository( MavenExecutionRequest request )
        throws MavenExecutionRequestPopulationException
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
            request.setLocalRepository( createLocalRepository( request ) );
        }

        if ( request.getLocalRepositoryPath() == null )
        {
            request.setLocalRepositoryPath( new File( request.getLocalRepository().getBasedir() ).getAbsoluteFile() );
        }
    }

    // ------------------------------------------------------------------------
    // Artifact Transfer Mechanism
    // ------------------------------------------------------------------------

    private ArtifactRepository createLocalRepository( MavenExecutionRequest request )
        throws MavenExecutionRequestPopulationException
    {
        String localRepositoryPath = null;

        if ( request.getLocalRepositoryPath() != null )
        {
            localRepositoryPath = request.getLocalRepositoryPath().getAbsolutePath();
        }

        if ( StringUtils.isEmpty( localRepositoryPath ) )
        {
            localRepositoryPath = RepositorySystem.defaultUserLocalRepository.getAbsolutePath();
        }

        try
        {
            return createLocalRepository( request, new File( localRepositoryPath ) );
        }
        catch ( Exception e )
        {
            throw new MavenExecutionRequestPopulationException( "Cannot create local repository.", e );
        }
    }

    private void baseDirectory( MavenExecutionRequest request )
    {
        if ( request.getBaseDirectory() == null && request.getPom() != null )
        {
            request.setBaseDirectory( request.getPom().getAbsoluteFile().getParentFile() );
        }
    }
    
    //
    // Code taken from LegacyRepositorySystem
    //
        
    private ArtifactRepository createLocalRepository( MavenExecutionRequest request, File localRepository )
        throws Exception
    {
        return createRepository( request, "file://" + localRepository.toURI().getRawPath(),
                                 RepositorySystem.DEFAULT_LOCAL_REPO_ID, true,
                                 ArtifactRepositoryPolicy.UPDATE_POLICY_ALWAYS, true,
                                 ArtifactRepositoryPolicy.UPDATE_POLICY_ALWAYS,
                                 ArtifactRepositoryPolicy.CHECKSUM_POLICY_IGNORE );
    }
    
    private void injectMirror( MavenExecutionRequest request, List<ArtifactRepository> repositories, List<Mirror> mirrors )
    {
        if ( repositories != null && mirrors != null )
        {
            for ( ArtifactRepository repository : repositories )
            {
                Mirror mirror = getMirror( repository, mirrors );
                injectMirror( request, repository, mirror );
            }
        }
    }   

    private void injectMirror( MavenExecutionRequest request, ArtifactRepository repository, Mirror mirror )
    {
        if ( mirror != null )
        {
            ArtifactRepository original =
                createArtifactRepository( repository.getId(), repository.getUrl(), repository.getLayout(),
                                          repository.getSnapshots(), repository.getReleases(), request );

            repository.setMirroredRepositories( Collections.singletonList( original ) );

            repository.setId( mirror.getId() );
            repository.setUrl( mirror.getUrl() );

            if ( StringUtils.isNotEmpty( mirror.getLayout() ) )
            {
                repository.setLayout( getLayout( mirror.getLayout() ) );
            }
        }
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
    private static class UnknownRepositoryLayout
        implements ArtifactRepositoryLayout
    {

        private final String id;

        private final ArtifactRepositoryLayout fallback;

        public UnknownRepositoryLayout( String id, ArtifactRepositoryLayout fallback )
        {
            this.id = id;
            this.fallback = fallback;
        }

        @Override
        public String getId()
        {
            return id;
        }

        @Override
        public String pathOf( Artifact artifact )
        {
            return fallback.pathOf( artifact );
        }

        @Override
        public String pathOfLocalRepositoryMetadata( ArtifactMetadata metadata, ArtifactRepository repository )
        {
            return fallback.pathOfLocalRepositoryMetadata( metadata, repository );
        }

        @Override
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
    
    //
    // ArtifactRepositoryFactory
    //
    private ArtifactRepository createArtifactRepository( String id, String url, String layoutId,
                                                        ArtifactRepositoryPolicy snapshots,
                                                        ArtifactRepositoryPolicy releases,
                                                        MavenExecutionRequest request )
        throws Exception
    {
        ArtifactRepositoryLayout layout = layouts.get( layoutId );

        checkLayout( id, layoutId, layout );

        return createArtifactRepository( id, url, layout, snapshots, releases, request );
    }

    private ArtifactRepository createArtifactRepository( String id, String url,
                                                        ArtifactRepositoryLayout repositoryLayout,
                                                        ArtifactRepositoryPolicy snapshots,
                                                        ArtifactRepositoryPolicy releases,
                                                        MavenExecutionRequest request )
    {
        String globalChecksumPolicy = request.getGlobalChecksumPolicy();
      
        if ( snapshots == null )
        {
            snapshots = new ArtifactRepositoryPolicy();
        }

        if ( releases == null )
        {
            releases = new ArtifactRepositoryPolicy();
        }

        if ( globalChecksumPolicy != null )
        {
            snapshots.setChecksumPolicy( globalChecksumPolicy );
            releases.setChecksumPolicy( globalChecksumPolicy );
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
    
    private void checkLayout( String repositoryId, String layoutId, ArtifactRepositoryLayout layout )
        throws Exception
    {
        if ( layout == null )
        {
            throw new Exception( String.format( "Cannot find ArtifactRepositoryLayout instance for: %s %s", layoutId, repositoryId ) );
        }
    }
    
    //
    // MirrorSelector
    //
    private Mirror getMirror( ArtifactRepository repository, List<Mirror> mirrors )
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
     * This method checks if the pattern matches the originalRepository. Valid patterns: * =
     * everything external:* = everything not on the localhost and not file based. repo,repo1 = repo
     * or repo1 *,!repo1 = everything except repo1
     *
     * @param originalRepository to compare for a match.
     * @param pattern used for match. Currently only '*' is supported.
     * @return true if the repository is a match to this pattern.
     */
    private boolean matchPattern( ArtifactRepository originalRepository, String pattern )
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
                repo = repo.trim();
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
    private boolean isExternalRepo( ArtifactRepository originalRepository )
    {
        try
        {
            URL url = new URL( originalRepository.getUrl() );
            return !( url.getHost().equals( "localhost" ) || url.getHost().equals( "127.0.0.1" )
                            || url.getProtocol().equals( "file" ) );
        }
        catch ( MalformedURLException e )
        {
            // bad url just skip it here. It should have been validated already, but the wagon lookup will deal with it
            return false;
        }
    }

    private boolean matchesLayout( ArtifactRepository repository, Mirror mirror )
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
    private boolean matchesLayout( String repoLayout, String mirrorLayout )
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
