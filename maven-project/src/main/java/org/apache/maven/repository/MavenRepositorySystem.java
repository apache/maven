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

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.InvalidRepositoryException;
import org.apache.maven.artifact.metadata.ArtifactMetadataRetrievalException;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.metadata.ResolutionGroup;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DeploymentRepository;
import org.apache.maven.model.Model;
import org.apache.maven.model.Repository;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.artifact.InvalidDependencyVersionException;
import org.apache.maven.wagon.events.TransferListener;

/**
 * @author Jason van Zyl
 */
public interface MavenRepositorySystem
    extends ArtifactMetadataSource // This needs to be removed
{
    // Artifact creation: This needs to be reduced to fewer, if not one, method. 

    Artifact createArtifact( String groupId, String artifactId, String version, String scope, String type );

    Artifact createArtifactWithClassifier( String groupId, String artifactId, String version, String type, String classifier );

    Artifact createBuildArtifact( String groupId, String artifactId, String version, String packaging );

    Artifact createProjectArtifact( String groupId, String artifactId, String metaVersionId );

    Artifact createPluginArtifact( String groupId, String artifactId, String version );

    Artifact createExtensionArtifact( String groupId, String artifactId, String version );

    Artifact createParentArtifact( String groupId, String artifactId, String version );

    Artifact createDependencyArtifact( String groupId, String artifactId, String version, String type, String classifier, String scope, boolean optional );

    Artifact createDependencyArtifact( String groupId, String artifactId, String version, String type, String classifier, String scope, String inheritedScope );
    
    Set<Artifact> createArtifacts( List<Dependency> dependencies, String inheritedScope, ArtifactFilter dependencyFilter, MavenProject project )
        throws InvalidDependencyVersionException;

    // Repository creation

    // maven model
    List<ArtifactRepository> buildArtifactRepositories( List<Repository> repositories )
        throws InvalidRepositoryException;

    // maven model
    ArtifactRepository buildArtifactRepository( Repository repository )
        throws InvalidRepositoryException;
        
    ArtifactRepository createLocalRepository( String url, String repositoryId )
        throws IOException;

    ArtifactRepository createRepository( String url, String repositoryId );

    ArtifactRepository createRepository( String url, String repositoryId, ArtifactRepositoryPolicy snapshotsPolicy, ArtifactRepositoryPolicy releasesPolicy );

    void setGlobalUpdatePolicy( String policy );

    void setGlobalChecksumPolicy( String policy );

    // Artifact resolution

    ArtifactResolutionResult resolve( ArtifactResolutionRequest request );

    // This can be reduced to the request/result
    void resolve( Artifact artifact, ArtifactRepository localRepository, List<ArtifactRepository> remoteRepositories )
        throws ArtifactResolutionException, ArtifactNotFoundException;

    void findModelFromRepository( Artifact artifact, List remoteArtifactRepositories, ArtifactRepository localRepository )
        throws ProjectBuildingException;

    // Version retrieval or metadata operations

    ResolutionGroup retrieve( Artifact artifact, ArtifactRepository localRepository, List<ArtifactRepository> remoteRepositories )
        throws ArtifactMetadataRetrievalException;

    List<ArtifactVersion> retrieveAvailableVersions( Artifact artifact, ArtifactRepository localRepository, List<ArtifactRepository> remoteRepositories )
        throws ArtifactMetadataRetrievalException;
    
    // These are only showing up in tests, not called from anywhere else in the core
    public List<ArtifactVersion> retrieveAvailableVersionsFromDeploymentRepository( Artifact artifact, ArtifactRepository localRepository, ArtifactRepository remoteRepository )
        throws ArtifactMetadataRetrievalException;

    public Artifact retrieveRelocatedArtifact( Artifact artifact, ArtifactRepository localRepository, List<ArtifactRepository> remoteRepositories )
        throws ArtifactMetadataRetrievalException;
        
    // Mirrors

    ArtifactRepository getMirrorRepository( ArtifactRepository repository );

    ArtifactRepository getMirror( ArtifactRepository originalRepository );

    boolean matchPattern( ArtifactRepository originalRepository, String pattern );

    boolean isExternalRepo( ArtifactRepository originalRepository );

    void addMirror( String id, String mirrorOf, String url );

    // Network enablement
    
    void setOnline( boolean online );

    boolean isOnline();
    
    // This doesn't belong here
    void setInteractive( boolean interactive );

    void setDownloadMonitor( TransferListener downloadMonitor );

    void addProxy( String protocol, String host, int port, String username, String password, String nonProxyHosts );

    void addAuthenticationInfo( String repositoryId, String username, String password, String privateKey, String passphrase );

    void addPermissionInfo( String repositoryId, String filePermissions, String directoryPermissions );
}
