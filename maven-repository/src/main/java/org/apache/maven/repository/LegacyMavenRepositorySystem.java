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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.artifact.InvalidRepositoryException;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.manager.WagonManager;
import org.apache.maven.artifact.metadata.ArtifactMetadataRetrievalException;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.metadata.ResolutionGroup;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryFactory;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.repository.DefaultArtifactRepository;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.resolver.filter.AndArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ExcludesArtifactFilter;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Exclusion;
import org.apache.maven.model.Repository;
import org.apache.maven.model.RepositoryPolicy;
import org.apache.maven.wagon.authentication.AuthenticationInfo;
import org.apache.maven.wagon.events.TransferListener;
import org.apache.maven.wagon.proxy.ProxyInfo;
import org.apache.maven.wagon.repository.RepositoryPermissions;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.StringUtils;

/**
 * @author Jason van Zyl
 */
@Component(role = MavenRepositorySystem.class)
public class LegacyMavenRepositorySystem
    implements MavenRepositorySystem
{
    @Requirement
    private ArtifactFactory artifactFactory;

    @Requirement
    private ArtifactResolver artifactResolver;

    @Requirement
    private ArtifactRepositoryFactory artifactRepositoryFactory;

    @Requirement
    private ArtifactRepositoryLayout defaultArtifactRepositoryLayout;

    @Requirement
    private WagonManager wagonManager;

    @Requirement
    private ArtifactMetadataSource artifactMetadataSource;

    @Requirement
    private Logger logger;

    private static HashMap<String, Artifact> cache = new HashMap<String, Artifact>();

    // Artifact Creation

    public Artifact createArtifact( String groupId, String artifactId, String version, String scope, String type )
    {
        return artifactFactory.createArtifact( groupId, artifactId, version, scope, type );
    }

    public Artifact createArtifactWithClassifier( String groupId, String artifactId, String version, String type, String classifier )
    {
        return artifactFactory.createArtifactWithClassifier( groupId, artifactId, version, type, classifier );
    }

    public Artifact createBuildArtifact( String groupId, String artifactId, String version, String packaging )
    {
        return artifactFactory.createBuildArtifact( groupId, artifactId, version, packaging );
    }

    public Artifact createProjectArtifact( String groupId, String artifactId, String metaVersionId )
    {
        return artifactFactory.createProjectArtifact( groupId, artifactId, metaVersionId );
    }

    public Artifact createDependencyArtifact( String groupId, String artifactId, String version, String type, String classifier, String scope, boolean optional )
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

        return artifactFactory.createDependencyArtifact( groupId, artifactId, versionRange, type, classifier, scope );
    }

    public Artifact createDependencyArtifact( String groupId, String artifactId, String version, String type, String classifier, String scope, String inheritedScope )
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

        return artifactFactory.createDependencyArtifact( groupId, artifactId, versionRange, type, classifier, scope, inheritedScope );
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

    public Artifact createPluginArtifact( String groupId, String artifactId, String version )
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

        return artifactFactory.createPluginArtifact( groupId, artifactId, versionRange );
    }

    /**
     * @return {@link Set} &lt; {@link Artifact} >
     * @todo desperately needs refactoring. It's just here because it's implementation is maven-project specific
     */
    public Set<Artifact> createArtifacts( List<Dependency> dependencies, String inheritedScope, ArtifactFilter dependencyFilter, MavenRepositoryWrapper reactor )
        throws VersionNotFoundException
    {
        Set<Artifact> projectArtifacts = new LinkedHashSet<Artifact>( dependencies.size() );

        for ( Iterator<Dependency> i = dependencies.iterator(); i.hasNext(); )
        {
            Dependency d = i.next();

            String scope = d.getScope();

            if ( StringUtils.isEmpty( scope ) )
            {
                scope = Artifact.SCOPE_COMPILE;

                d.setScope( scope );
            }

            VersionRange versionRange;
            try
            {
                versionRange = VersionRange.createFromVersionSpec( d.getVersion() );
            }
            catch ( InvalidVersionSpecificationException e )
            {
                throw new VersionNotFoundException( reactor.getId(), d, reactor.getFile(), e );
            }
            Artifact artifact = artifactFactory.createDependencyArtifact( d.getGroupId(), d.getArtifactId(),
                                                                          versionRange, d.getType(), d.getClassifier(),
                                                                          scope, inheritedScope, d.isOptional() );

            if ( Artifact.SCOPE_SYSTEM.equals( scope ) )
            {
                artifact.setFile( new File( d.getSystemPath() ) );
            }

            ArtifactFilter artifactFilter = dependencyFilter;

            if ( ( artifact != null ) && ( ( artifactFilter == null ) || artifactFilter.include( artifact ) ) )
            {
                if ( ( d.getExclusions() != null ) && !d.getExclusions().isEmpty() )
                {
                    List<String> exclusions = new ArrayList<String>();
                    for ( Iterator<Exclusion> j = d.getExclusions().iterator(); j.hasNext(); )
                    {
                        Exclusion e = j.next();
                        exclusions.add( e.getGroupId() + ":" + e.getArtifactId() );
                    }

                    ArtifactFilter newFilter = new ExcludesArtifactFilter( exclusions );

                    if ( artifactFilter != null )
                    {
                        AndArtifactFilter filter = new AndArtifactFilter();
                        filter.add( artifactFilter );
                        filter.add( newFilter );
                        artifactFilter = filter;
                    }
                    else
                    {
                        artifactFilter = newFilter;
                    }
                }

                artifact.setDependencyFilter( artifactFilter );

                if ( reactor != null )
                {
                    artifact = reactor.find( artifact );
                }

                projectArtifacts.add( artifact );
            }
        }

        return projectArtifacts;
    }    
    
    public List<ArtifactVersion> retrieveAvailableVersions( Artifact artifact, ArtifactRepository localRepository, List<ArtifactRepository> remoteRepositories )
        throws ArtifactMetadataRetrievalException
    {
        return artifactMetadataSource.retrieveAvailableVersions( artifact, localRepository, remoteRepositories );
    }

    public ResolutionGroup retrieve( Artifact artifact, ArtifactRepository localRepository, List<ArtifactRepository> remoteRepositories )
        throws ArtifactMetadataRetrievalException
    {
        return artifactMetadataSource.retrieve( artifact, localRepository, remoteRepositories );
    }

    // ----------------------------------------------------------------------------
    // Code snagged from ProjectUtils: this will have to be moved somewhere else
    // but just trying to collect it all in one place right now.
    // ----------------------------------------------------------------------------

    public List<ArtifactRepository> buildArtifactRepositories( List<Repository> repositories )
        throws InvalidRepositoryException
    {
        List<ArtifactRepository> repos = new ArrayList<ArtifactRepository>();

        for ( Repository mavenRepo : repositories )
        {
            ArtifactRepository artifactRepo = buildArtifactRepository( mavenRepo );

            if ( !repos.contains( artifactRepo ) )
            {
                repos.add( artifactRepo );
            }
        }

        return repos;
    }

    public ArtifactRepository buildArtifactRepository( Repository repo )
        throws InvalidRepositoryException
    {
        if ( repo != null )
        {
            String id = repo.getId();
            String url = repo.getUrl();
            /*
            MNG-4050: Temporarily disabled this check since it is breaking the bootstrap unit tests on commons-parent pom
             */
            /*
            if ( id == null || id.trim().length() < 1 )
            {
                throw new InvalidRepositoryException( "Repository ID must not be empty (URL is: " + url + ").", url );
            }

            if ( url == null || url.trim().length() < 1 )
            {
                throw new InvalidRepositoryException( "Repository URL must not be empty (ID is: " + id + ").", id );
            }
            */
            ArtifactRepositoryPolicy snapshots = buildArtifactRepositoryPolicy( repo.getSnapshots() );

            ArtifactRepositoryPolicy releases = buildArtifactRepositoryPolicy( repo.getReleases() );

            return artifactRepositoryFactory.createArtifactRepository( id, url, repo.getLayout(), snapshots, releases );
        }
        else
        {
            return null;
        }
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

    // From MavenExecutionRequestPopulator

    public ArtifactRepository createLocalRepository( String url, String repositoryId )
        throws IOException
    {
        return createRepository( canonicalFileUrl( url ), repositoryId );
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

    public ArtifactRepository createRepository( String url, String repositoryId )
    {
        // snapshots vs releases
        // offline = to turning the update policy off

        //TODO: we'll need to allow finer grained creation of repositories but this will do for now

        String updatePolicyFlag = ArtifactRepositoryPolicy.UPDATE_POLICY_ALWAYS;

        String checksumPolicyFlag = ArtifactRepositoryPolicy.CHECKSUM_POLICY_WARN;

        ArtifactRepositoryPolicy snapshotsPolicy = new ArtifactRepositoryPolicy( true, updatePolicyFlag, checksumPolicyFlag );

        ArtifactRepositoryPolicy releasesPolicy = new ArtifactRepositoryPolicy( true, updatePolicyFlag, checksumPolicyFlag );

        return artifactRepositoryFactory.createArtifactRepository( repositoryId, url, defaultArtifactRepositoryLayout, snapshotsPolicy, releasesPolicy );
    }

    public void setGlobalUpdatePolicy( String policy )
    {
        artifactRepositoryFactory.setGlobalUpdatePolicy( policy );
    }

    public void setGlobalChecksumPolicy( String policy )
    {
        artifactRepositoryFactory.setGlobalChecksumPolicy( policy );
    }

    // Taken from RepositoryHelper

    public void findModelFromRepository( Artifact artifact, List remoteArtifactRepositories, ArtifactRepository localRepository )
        throws InvalidRepositoryException, ArtifactResolutionException, ArtifactNotFoundException    
    {

        if ( cache.containsKey( artifact.getId() ) )
        {
            artifact.setFile( cache.get( artifact.getId() ).getFile() );
        }

        String projectId = safeVersionlessKey( artifact.getGroupId(), artifact.getArtifactId() );
        remoteArtifactRepositories = normalizeToArtifactRepositories( remoteArtifactRepositories, projectId );

        Artifact projectArtifact;

        // if the artifact is not a POM, we need to construct a POM artifact based on the artifact parameter given.
        if ( "pom".equals( artifact.getType() ) )
        {
            projectArtifact = artifact;
        }
        else
        {
            logger.debug( "Attempting to build MavenProject instance for Artifact (" + artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + artifact.getVersion() + ") of type: "
                + artifact.getType() + "; constructing POM artifact instead." );

            projectArtifact = artifactFactory.createProjectArtifact( artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion(), artifact.getScope() );
        }

        artifactResolver.resolve( projectArtifact, remoteArtifactRepositories, localRepository );

        File file = projectArtifact.getFile();
        artifact.setFile( file );
        cache.put( artifact.getId(), artifact );
    }

    private List normalizeToArtifactRepositories( List remoteArtifactRepositories, String projectId )
        throws InvalidRepositoryException
    {
        List normalized = new ArrayList( remoteArtifactRepositories.size() );

        boolean normalizationNeeded = false;
        for ( Iterator it = remoteArtifactRepositories.iterator(); it.hasNext(); )
        {
            Object item = it.next();

            if ( item instanceof ArtifactRepository )
            {
                normalized.add( item );
            }
            else if ( item instanceof Repository )
            {
                Repository repo = (Repository) item;
                item = buildArtifactRepository( repo );

                normalized.add( item );
                normalizationNeeded = true;
            }
            else
            {
                throw new InvalidRepositoryException( projectId, "Error building artifact repository from non-repository information item: " + item );
            }
        }

        if ( normalizationNeeded )
        {
            return normalized;
        }
        else
        {
            return remoteArtifactRepositories;
        }
    }

    private String safeVersionlessKey( String groupId, String artifactId )
    {
        String gid = groupId;

        if ( StringUtils.isEmpty( gid ) )
        {
            gid = "unknown";
        }

        String aid = artifactId;

        if ( StringUtils.isEmpty( aid ) )
        {
            aid = "unknown";
        }

        return ArtifactUtils.versionlessKey( gid, aid );
    }

    /**
     * Resolves the specified artifact
     * 
     * @param artifact the artifact to resolve
     * @throws IOException if there is a problem resolving the artifact
     */
    public void resolve( Artifact artifact, ArtifactRepository localRepository, List<ArtifactRepository> remoteRepositories )
        throws ArtifactResolutionException, ArtifactNotFoundException
    {
        artifactResolver.resolve( artifact, remoteRepositories, localRepository );
    }

    public ArtifactResolutionResult resolve( ArtifactResolutionRequest request )
    {
        return artifactResolver.resolve( request );
    }

    // ------------------------------------------------------------------------
    // Extracted from DefaultWagonManager
    // ------------------------------------------------------------------------

    private static final String WILDCARD = "*";

    private static final String EXTERNAL_WILDCARD = "external:*";

    private static int anonymousMirrorIdSeed = 0;

    private boolean online = true;

    private boolean interactive = true;

    private TransferListener downloadMonitor;

    private Map<String, ProxyInfo> proxies = new HashMap<String, ProxyInfo>();

    private Map<String, AuthenticationInfo> authenticationInfoMap = new HashMap<String, AuthenticationInfo>();

    private Map<String, RepositoryPermissions> serverPermissionsMap = new HashMap<String, RepositoryPermissions>();

    //used LinkedMap to preserve the order.
    private Map<String, ArtifactRepository> mirrors = new LinkedHashMap<String, ArtifactRepository>();

    public ArtifactRepository getMirrorRepository( ArtifactRepository repository )
    {
        ArtifactRepository mirror = getMirror( repository );
        if ( mirror != null )
        {
            String id = mirror.getId();
            if ( id == null )
            {
                // TODO: this should be illegal in settings.xml
                id = repository.getId();
            }

            logger.debug( "Using mirror: " + mirror.getId() + " for repository: " + repository.getId() + "\n(mirror url: " + mirror.getUrl() + ")" );
            repository = artifactRepositoryFactory.createArtifactRepository( id, mirror.getUrl(), repository.getLayout(), repository.getSnapshots(), repository.getReleases() );
        }
        return repository;
    }

    /**
     * This method finds a matching mirror for the selected repository. If there is an exact match,
     * this will be used. If there is no exact match, then the list of mirrors is examined to see if
     * a pattern applies.
     * 
     * @param originalRepository See if there is a mirror for this repository.
     * @return the selected mirror or null if none are found.
     */
    public ArtifactRepository getMirror( ArtifactRepository originalRepository )
    {
        ArtifactRepository selectedMirror = mirrors.get( originalRepository.getId() );
        if ( null == selectedMirror )
        {
            // Process the patterns in order. First one that matches wins.
            Set<String> keySet = mirrors.keySet();
            if ( keySet != null )
            {
                for ( String pattern : keySet )
                {
                    if ( matchPattern( originalRepository, pattern ) )
                    {
                        selectedMirror = mirrors.get( pattern );
                    }
                }
            }

        }
        return selectedMirror;
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
    public boolean matchPattern( ArtifactRepository originalRepository, String pattern )
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
                    if ( originalId.equals( repo.substring( 1 ) ) )
                    {
                        // explicitly exclude. Set result and stop processing.
                        result = false;
                        break;
                    }
                }
                // check for exact match
                else if ( originalId.equals( repo ) )
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
    public boolean isExternalRepo( ArtifactRepository originalRepository )
    {
        try
        {
            URL url = new URL( originalRepository.getUrl() );
            return !( url.getHost().equals( "localhost" ) || url.getHost().equals( "127.0.0.1" ) || url.getProtocol().equals( "file" ) );
        }
        catch ( MalformedURLException e )
        {
            // bad url just skip it here. It should have been validated already, but the wagon lookup will deal with it
            return false;
        }
    }

    public void addMirror( String id, String mirrorOf, String url )
    {
        if ( id == null )
        {
            id = "mirror-" + anonymousMirrorIdSeed++;
            logger.warn( "You are using a mirror that doesn't declare an <id/> element. Using \'" + id + "\' instead:\nId: " + id + "\nmirrorOf: " + mirrorOf + "\nurl: " + url + "\n" );
        }

        ArtifactRepository mirror = new DefaultArtifactRepository( id, url, null );

        mirrors.put( mirrorOf, mirror );
    }

    public void setOnline( boolean online )
    {
        this.online = online;
    }

    public boolean isOnline()
    {
        return online;
    }

    public void setInteractive( boolean interactive )
    {
        this.interactive = interactive;
    }

    public void setDownloadMonitor( TransferListener downloadMonitor )
    {
        this.downloadMonitor = downloadMonitor;
    }

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
    }

    public void addAuthenticationInfo( String repositoryId, String username, String password, String privateKey, String passphrase )
    {
        AuthenticationInfo authInfo = new AuthenticationInfo();
        authInfo.setUserName( username );
        authInfo.setPassword( password );
        authInfo.setPrivateKey( privateKey );
        authInfo.setPassphrase( passphrase );

        authenticationInfoMap.put( repositoryId, authInfo );
    }

    public void addPermissionInfo( String repositoryId, String filePermissions, String directoryPermissions )
    {
        RepositoryPermissions permissions = new RepositoryPermissions();

        boolean addPermissions = false;

        if ( filePermissions != null )
        {
            permissions.setFileMode( filePermissions );
            addPermissions = true;
        }

        if ( directoryPermissions != null )
        {
            permissions.setDirectoryMode( directoryPermissions );
            addPermissions = true;
        }

        if ( addPermissions )
        {
            serverPermissionsMap.put( repositoryId, permissions );
        }
    }

    // These two methods are here so that the ArtifactMetadataSource is implemented so that I can pass this into an ArtifactResolutionRequest.
    // Intermediate measure before separating the RepositorySystem out into its own module.

    public List<ArtifactVersion> retrieveAvailableVersionsFromDeploymentRepository( Artifact artifact, ArtifactRepository localRepository, ArtifactRepository remoteRepository )
        throws ArtifactMetadataRetrievalException
    {
        return artifactMetadataSource.retrieveAvailableVersionsFromDeploymentRepository( artifact, localRepository, remoteRepository );
    }

    public Artifact retrieveRelocatedArtifact( Artifact artifact, ArtifactRepository localRepository, List<ArtifactRepository> remoteRepositories )
        throws ArtifactMetadataRetrievalException
    {
        return artifactMetadataSource.retrieveRelocatedArtifact( artifact, localRepository, remoteRepositories );
    }

    // Test for this stuff

    /*
     
    public void testAddMirrorWithNullRepositoryId()
    {
        wagonManager.addMirror( null, "test", "http://www.nowhere.com/" );
    }
    
    public void testGetArtifactSha1MissingMd5Present()
        throws IOException, UnsupportedProtocolException, TransferFailedException, ResourceDoesNotExistException
    {
        Artifact artifact = createTestPomArtifact( "target/test-data/get-remote-artifact" );

        ArtifactRepository repo = createStringRepo();

        StringWagon wagon = (StringWagon) wagonManager.getWagon( "string" );
        wagon.addExpectedContent( repo.getLayout().pathOf( artifact ), "expected" );
        wagon.addExpectedContent( repo.getLayout().pathOf( artifact ) + ".md5", "bad_checksum" );
        
        wagonManager.getArtifact( artifact, repo, true );

        assertTrue( artifact.getFile().exists() );
    }
    
    public void testExternalURL()
    {
        DefaultWagonManager mgr = new DefaultWagonManager();
        assertTrue( mgr.isExternalRepo( getRepo( "foo", "http://somehost" ) ) );
        assertTrue( mgr.isExternalRepo( getRepo( "foo", "http://somehost:9090/somepath" ) ) );
        assertTrue( mgr.isExternalRepo( getRepo( "foo", "ftp://somehost" ) ) );
        assertTrue( mgr.isExternalRepo( getRepo( "foo", "http://192.168.101.1" ) ) );
        assertTrue( mgr.isExternalRepo( getRepo( "foo", "http://" ) ) );
        // these are local
        assertFalse( mgr.isExternalRepo( getRepo( "foo", "http://localhost:8080" ) ) );
        assertFalse( mgr.isExternalRepo( getRepo( "foo", "http://127.0.0.1:9090" ) ) );
        assertFalse( mgr.isExternalRepo( getRepo( "foo", "file://localhost/somepath" ) ) );
        assertFalse( mgr.isExternalRepo( getRepo( "foo", "file://localhost/D:/somepath" ) ) );
        assertFalse( mgr.isExternalRepo( getRepo( "foo", "http://localhost" ) ) );
        assertFalse( mgr.isExternalRepo( getRepo( "foo", "http://127.0.0.1" ) ) );
        assertFalse( mgr.isExternalRepo( getRepo( "foo", "file:///somepath" ) ) );
        assertFalse( mgr.isExternalRepo( getRepo( "foo", "file://D:/somepath" ) ) );

        // not a proper url so returns false;
        assertFalse( mgr.isExternalRepo( getRepo( "foo", "192.168.101.1" ) ) );
        assertFalse( mgr.isExternalRepo( getRepo( "foo", "" ) ) );
    }

    public void testMirrorLookup()
    {
        wagonManager.addMirror( "a", "a", "http://a" );
        wagonManager.addMirror( "b", "b", "http://b" );

        ArtifactRepository repo = null;
        repo = wagonManager.getMirrorRepository( getRepo( "a", "http://a.a" ) );
        assertEquals( "http://a", repo.getUrl() );

        repo = wagonManager.getMirrorRepository( getRepo( "b", "http://a.a" ) );
        assertEquals( "http://b", repo.getUrl() );

        repo = wagonManager.getMirrorRepository( getRepo( "c", "http://c.c" ) );
        assertEquals( "http://c.c", repo.getUrl() );

    }

    public void testMirrorWildcardLookup()
    {
        wagonManager.addMirror( "a", "a", "http://a" );
        wagonManager.addMirror( "b", "b", "http://b" );
        wagonManager.addMirror( "c", "*", "http://wildcard" );

        ArtifactRepository repo = null;
        repo = wagonManager.getMirrorRepository( getRepo( "a", "http://a.a" ) );
        assertEquals( "http://a", repo.getUrl() );

        repo = wagonManager.getMirrorRepository( getRepo( "b", "http://a.a" ) );
        assertEquals( "http://b", repo.getUrl() );

        repo = wagonManager.getMirrorRepository( getRepo( "c", "http://c.c" ) );
        assertEquals( "http://wildcard", repo.getUrl() );

    }

    public void testMirrorStopOnFirstMatch()
    {
        //exact matches win first
        wagonManager.addMirror( "a2", "a,b", "http://a2" );
        wagonManager.addMirror( "a", "a", "http://a" );
        //make sure repeated entries are skipped
        wagonManager.addMirror( "a", "a", "http://a3" );
        
        wagonManager.addMirror( "b", "b", "http://b" );
        wagonManager.addMirror( "c", "d,e", "http://de" );
        wagonManager.addMirror( "c", "*", "http://wildcard" );
        wagonManager.addMirror( "c", "e,f", "http://ef" );
        
    

        ArtifactRepository repo = null;
        repo = wagonManager.getMirrorRepository( getRepo( "a", "http://a.a" ) );
        assertEquals( "http://a", repo.getUrl() );

        repo = wagonManager.getMirrorRepository( getRepo( "b", "http://a.a" ) );
        assertEquals( "http://b", repo.getUrl() );

        repo = wagonManager.getMirrorRepository( getRepo( "c", "http://c.c" ) );
        assertEquals( "http://wildcard", repo.getUrl() );
        
        repo = wagonManager.getMirrorRepository( getRepo( "d", "http://d" ) );
        assertEquals( "http://de", repo.getUrl() );
        
        repo = wagonManager.getMirrorRepository( getRepo( "e", "http://e" ) );
        assertEquals( "http://de", repo.getUrl() );
        
        repo = wagonManager.getMirrorRepository( getRepo( "f", "http://f" ) );
        assertEquals( "http://wildcard", repo.getUrl() );

    }

    
    public void testPatterns()
    {
        DefaultWagonManager mgr = new DefaultWagonManager();

        assertTrue( mgr.matchPattern( getRepo( "a" ), "*" ) );
        assertTrue( mgr.matchPattern( getRepo( "a" ), "*," ) );
        assertTrue( mgr.matchPattern( getRepo( "a" ), ",*," ) );
        assertTrue( mgr.matchPattern( getRepo( "a" ), "*," ) );

        assertTrue( mgr.matchPattern( getRepo( "a" ), "a" ) );
        assertTrue( mgr.matchPattern( getRepo( "a" ), "a," ) );
        assertTrue( mgr.matchPattern( getRepo( "a" ), ",a," ) );
        assertTrue( mgr.matchPattern( getRepo( "a" ), "a," ) );

        assertFalse( mgr.matchPattern( getRepo( "b" ), "a" ) );
        assertFalse( mgr.matchPattern( getRepo( "b" ), "a," ) );
        assertFalse( mgr.matchPattern( getRepo( "b" ), ",a" ) );
        assertFalse( mgr.matchPattern( getRepo( "b" ), ",a," ) );

        assertTrue( mgr.matchPattern( getRepo( "a" ), "a,b" ) );
        assertTrue( mgr.matchPattern( getRepo( "b" ), "a,b" ) );

        assertFalse( mgr.matchPattern( getRepo( "c" ), "a,b" ) );

        assertTrue( mgr.matchPattern( getRepo( "a" ), "*" ) );
        assertTrue( mgr.matchPattern( getRepo( "a" ), "*,b" ) );
        assertTrue( mgr.matchPattern( getRepo( "a" ), "*,!b" ) );

        assertFalse( mgr.matchPattern( getRepo( "a" ), "*,!a" ) );
        assertFalse( mgr.matchPattern( getRepo( "a" ), "!a,*" ) );

        assertTrue( mgr.matchPattern( getRepo( "c" ), "*,!a" ) );
        assertTrue( mgr.matchPattern( getRepo( "c" ), "!a,*" ) );

        assertFalse( mgr.matchPattern( getRepo( "c" ), "!a,!c" ) );
        assertFalse( mgr.matchPattern( getRepo( "d" ), "!a,!c*" ) );
    }

    public void testPatternsWithExternal()
    {
        DefaultWagonManager mgr = new DefaultWagonManager();

        assertTrue( mgr.matchPattern( getRepo( "a", "http://localhost" ), "*" ) );
        assertFalse( mgr.matchPattern( getRepo( "a", "http://localhost" ), "external:*" ) );

        assertTrue( mgr.matchPattern( getRepo( "a", "http://localhost" ), "external:*,a" ) );
        assertFalse( mgr.matchPattern( getRepo( "a", "http://localhost" ), "external:*,!a" ) );
        assertTrue( mgr.matchPattern( getRepo( "a", "http://localhost" ), "a,external:*" ) );
        assertFalse( mgr.matchPattern( getRepo( "a", "http://localhost" ), "!a,external:*" ) );

        assertFalse( mgr.matchPattern( getRepo( "c", "http://localhost" ), "!a,external:*" ) );
        assertTrue( mgr.matchPattern( getRepo( "c", "http://somehost" ), "!a,external:*" ) );
    }
     
     */
}
