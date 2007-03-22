package org.apache.maven.artifact.manager;

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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.UnsupportedProtocolException;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.authentication.AuthenticationInfo;
import org.apache.maven.wagon.events.TransferListener;
import org.apache.maven.wagon.proxy.ProxyInfo;
import org.apache.maven.wagon.repository.Repository;
import org.apache.maven.wagon.repository.RepositoryPermissions;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import java.io.File;
import java.util.Collection;
import java.util.List;

/**
 * Manages <a href="http://maven.apache.org/wagon">Wagon</a> related operations in Maven.
 *
 * @author <a href="michal.maczka@dimatics.com">Michal Maczka </a>
 * @version $Id$
 */
public interface WagonManager
{
    String ROLE = WagonManager.class.getName();

    /**
     * Get a Wagon provider that understands the protocol passed as argument.
     * It doesn't configure the Wagon.
     *
     * @param protocol the protocol the {@link Wagon} will handle
     * @return the {@link Wagon} instance able to handle the protocol provided
     * @throws UnsupportedProtocolException if there is no provider able to handle the protocol
     * @deprecated prone to errors. use {@link #getWagon(Repository)} instead.
     */
    Wagon getWagon( String protocol )
        throws UnsupportedProtocolException;

    /**
     * Get a Wagon provider for the provided repository.
     * It will configure the Wagon for that repository.
     *
     * @param repository the repository
     * @return the {@link Wagon} instance that can be used to connect to the repository
     * @throws UnsupportedProtocolException if there is no provider able to handle the protocol
     * @throws WagonConfigurationException  if the wagon can't be configured for the repository
     */
    Wagon getWagon( Repository repository )
        throws UnsupportedProtocolException, WagonConfigurationException;

    void getArtifact( Artifact artifact, List remoteRepositories )
        throws TransferFailedException, ResourceDoesNotExistException;

    void getArtifact( Artifact artifact, ArtifactRepository repository )
        throws TransferFailedException, ResourceDoesNotExistException;

    void putArtifact( File source, Artifact artifact, ArtifactRepository deploymentRepository )
        throws TransferFailedException;

    void putArtifactMetadata( File source, ArtifactMetadata artifactMetadata, ArtifactRepository repository )
        throws TransferFailedException;

    void getArtifactMetadata( ArtifactMetadata metadata, ArtifactRepository remoteRepository, File destination,
                              String checksumPolicy )
        throws TransferFailedException, ResourceDoesNotExistException;

    void setOnline( boolean online );

    boolean isOnline();

    void addProxy( String protocol, String host, int port, String username, String password, String nonProxyHosts );

    void addAuthenticationInfo( String repositoryId, String username, String password, String privateKey,
                                String passphrase );

    void addMirror( String id, String mirrorOf, String url );

    void setDownloadMonitor( TransferListener downloadMonitor );

    void addPermissionInfo( String repositoryId, String filePermissions, String directoryPermissions );

    ProxyInfo getProxy( String protocol );

    AuthenticationInfo getAuthenticationInfo( String id );

    /**
     * Set the configuration for a repository
     *
     * @param repositoryId  id of the repository to set the configuration to
     * @param configuration dom tree of the xml with the configuration for the {@link Wagon}
     */
    void addConfiguration( String repositoryId, Xpp3Dom configuration );

    void setInteractive( boolean interactive );

    void registerWagons( Collection wagons, PlexusContainer extensionContainer );

    void setDefaultRepositoryPermissions( RepositoryPermissions permissions );
}