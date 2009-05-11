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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.InvalidRepositoryException;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryFactory;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.Repository;
import org.apache.maven.model.RepositoryPolicy;
import org.apache.maven.wagon.authentication.AuthenticationInfo;
import org.apache.maven.wagon.proxy.ProxyInfo;
import org.apache.maven.wagon.repository.RepositoryPermissions;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;

/**
 * @author Jason van Zyl
 */
@Component( role = RepositorySystem.class, hint = "default" )
public class LegacyRepositorySystem
    implements RepositorySystem
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
    private MirrorBuilder mirrorBuilder;

    private Map<String, ProxyInfo> proxies = new HashMap<String, ProxyInfo>();

    private Map<String, AuthenticationInfo> authenticationInfoMap = new HashMap<String, AuthenticationInfo>();

    private Map<String, RepositoryPermissions> serverPermissionsMap = new HashMap<String, RepositoryPermissions>();

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

        return artifactFactory.createDependencyArtifact( d.getGroupId(), d.getArtifactId(), versionRange, d.getType(), d.getClassifier(), d.getScope() );
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

    public ArtifactRepository buildArtifactRepository( Repository repo )
        throws InvalidRepositoryException
    {
        if ( repo != null )
        {
            String id = repo.getId();
            
            String url = repo.getUrl();
            
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
    
    public ArtifactRepository createDefaultLocalRepository()
        throws InvalidRepositoryException
    {
        return createLocalRepository( RepositorySystem.defaultUserLocalRepository );
    }
    
    public ArtifactRepository createLocalRepository( File localRepository )
        throws InvalidRepositoryException
    {
        try
        {
            return createRepository( localRepository.toURI().toURL().toString(),
                                     RepositorySystem.DEFAULT_LOCAL_REPO_ID, true,
                                     ArtifactRepositoryPolicy.UPDATE_POLICY_ALWAYS, true,
                                     ArtifactRepositoryPolicy.UPDATE_POLICY_ALWAYS,
                                     ArtifactRepositoryPolicy.CHECKSUM_POLICY_IGNORE );
        }
        catch ( MalformedURLException e )
        {
            throw new InvalidRepositoryException( "Error creating local repository.", RepositorySystem.DEFAULT_LOCAL_REPO_ID, e );
        }
    }

    public ArtifactRepository createDefaultRemoteRepository()
        throws InvalidRepositoryException
    {
        return createRepository( RepositorySystem.DEFAULT_REMOTE_REPO_URL, RepositorySystem.DEFAULT_REMOTE_REPO_ID,
                                 true, ArtifactRepositoryPolicy.UPDATE_POLICY_NEVER, false,
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

    private ArtifactRepository createRepository( String url, String repositoryId, boolean releases,
                                                 String releaseUpdates, boolean snapshots, String snapshotUpdates,
                                                 String checksumPolicy )
    {
        ArtifactRepositoryPolicy snapshotsPolicy = new ArtifactRepositoryPolicy( snapshots, snapshotUpdates, checksumPolicy );

        ArtifactRepositoryPolicy releasesPolicy = new ArtifactRepositoryPolicy( releases, releaseUpdates, checksumPolicy );

        return artifactRepositoryFactory.createArtifactRepository( repositoryId, url, defaultArtifactRepositoryLayout, snapshotsPolicy, releasesPolicy );
    }

    public ArtifactResolutionResult resolve( ArtifactResolutionRequest request )
    {                
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

    /*
    public void addAuthenticationInfo( String repositoryId, String username, String password, String privateKey, String passphrase )
    {
        AuthenticationInfo authInfo = new AuthenticationInfo();
        authInfo.setUserName( username );
        authInfo.setPassword( password );
        authInfo.setPrivateKey( privateKey );
        authInfo.setPassphrase( passphrase );

        authenticationInfoMap.put( repositoryId, authInfo );

        wagonManager.addAuthenticationInfo( repositoryId, username, password, privateKey, passphrase );
    }
    */

    /*
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
    */

    // Mirror 

    public void addMirror( String id, String mirrorOf, String url )
    {
        mirrorBuilder.addMirror( id, mirrorOf, url );
    }

    public List<ArtifactRepository> getMirrors( List<ArtifactRepository> repositories )
    {
        return mirrorBuilder.getMirrors( repositories );
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
}
