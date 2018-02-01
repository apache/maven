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
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.UnsupportedProtocolException;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.events.TransferListener;
import org.apache.maven.wagon.repository.Repository;

/**
 * WagonManager
 */
public interface WagonManager
{
    @Deprecated
    Wagon getWagon( String protocol )
        throws UnsupportedProtocolException;

    @Deprecated
    Wagon getWagon( Repository repository )
        throws UnsupportedProtocolException, WagonConfigurationException;

    //
    // Retriever
    //
    void getArtifact( Artifact artifact, ArtifactRepository repository, TransferListener transferListener,
                      boolean force )
        throws TransferFailedException, ResourceDoesNotExistException;

    void getArtifact( Artifact artifact, List<ArtifactRepository> remoteRepositories,
                      TransferListener transferListener, boolean force )
        throws TransferFailedException, ResourceDoesNotExistException;

    void getRemoteFile( ArtifactRepository repository, File destination, String remotePath,
                        TransferListener downloadMonitor, String checksumPolicy, boolean force )
        throws TransferFailedException, ResourceDoesNotExistException;

    void getArtifactMetadata( ArtifactMetadata metadata, ArtifactRepository remoteRepository, File destination,
                              String checksumPolicy )
        throws TransferFailedException, ResourceDoesNotExistException;

    void getArtifactMetadataFromDeploymentRepository( ArtifactMetadata metadata, ArtifactRepository remoteRepository,
                                                      File file, String checksumPolicyWarn )
        throws TransferFailedException, ResourceDoesNotExistException;

    //
    // Deployer
    //
    void putArtifact( File source, Artifact artifact, ArtifactRepository deploymentRepository,
                      TransferListener downloadMonitor )
        throws TransferFailedException;

    void putRemoteFile( ArtifactRepository repository, File source, String remotePath,
                        TransferListener downloadMonitor )
        throws TransferFailedException;

    void putArtifactMetadata( File source, ArtifactMetadata artifactMetadata, ArtifactRepository repository )
        throws TransferFailedException;
}
