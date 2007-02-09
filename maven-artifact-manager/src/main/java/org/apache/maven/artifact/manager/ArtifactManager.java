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
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.manager.WagonManager;

import java.io.File;
import java.util.List;

/**
 * Manages artifact get and put related operations in Maven.
 * 
 * Can get artifacts from remote repositories, or place local artifacts into remote repositories.
 *
 * @author <a href="michal.maczka@dimatics.com">Michal Maczka </a>
 * @version $Id$
 */
public interface ArtifactManager
{
    String ROLE = ArtifactManager.class.getName();
    
    /**
     * Flag indicating state of the ArtifactManager connectivity.
     * 
     * @return true means we are online, false means we are offline.
     */
    boolean isOnline();
    
    /**
     * Get the WagonManager that this ArtifactManager is using.
     * 
     * @return the wagon manager in use.
     */
    WagonManager getWagonManager();

    /**
     * Get an artifact from the specified List of remoteRepositories.
     * 
     * @param artifact the artifact to fetch.
     * @param remoteRepositories the list of {@link ArtifactRepository} objects to search.
     * @throws TransferFailedException if the transfer failed.
     * @throws ResourceDoesNotExistException if the resource does not exist on any remote repository.
     */
    void getArtifact( Artifact artifact, List remoteRepositories )
        throws TransferFailedException, ResourceDoesNotExistException;

    /**
     * Get an {@link Artifact} from the specified {@link ArtifactRepository}.
     * 
     * @param artifact the artifact to fetch.
     * @param repository the remote repository to search.
     * @throws TransferFailedException if the transfer failed.
     * @throws ResourceDoesNotExistException if the resource does not exist on the remote repository.
     */
    void getArtifact( Artifact artifact, ArtifactRepository repository )
        throws TransferFailedException, ResourceDoesNotExistException;

    /**
     * Perform a Put of the source {@link File} to the {@link Artifact} on the specified {@link ArtifactRepository} 
     * 
     * @param source the file containing the artifact on the local file system.
     * @param artifact the artifact to put to the remote repository.
     * @param deploymentRepository the remote repository to put the artifact into.
     * @throws TransferFailedException if the transfer failed.
     */
    void putArtifact( File source, Artifact artifact, ArtifactRepository deploymentRepository )
        throws TransferFailedException;

    /**
     * Perform a Put of the source {@link File} to the {@link ArtifactMetadata} on the specified {@link ArtifactRepository}
     * 
     * @param source the file containing the metadata on the local file system.
     * @param artifactMetadata the artifact metadata to put to the remote repository.
     * @param repository the remote repository to put the artifact into.
     * @throws TransferFailedException if the transfer failed.
     */
    void putArtifactMetadata( File source, ArtifactMetadata artifactMetadata, ArtifactRepository repository )
        throws TransferFailedException;

    /**
     * Get the {@link ArtifactMetadata} from the Remote {@link ArtifactRepository}. 
     * 
     * @param metadata the metadata to attempt to fetch.
     * @param remoteRepository the remote repository to find the metadata in.
     * @param destination the destination file on the local file system.
     * @param checksumPolicy the checksum policy to use when fetching file.
     *    Can be either the value {@link ArtifactRepositoryPolicy#CHECKSUM_POLICY_FAIL} or 
     *    {@link ArtifactRepositoryPolicy.CHECKSUM_POLICY_IGNORE}, all other values are treated as
     *    {@link ArtifactRepositoryPolicy.CHECKSUM_POLICY_IGNORE}.
     * @throws TransferFailedException
     * @throws ResourceDoesNotExistException
     */
    void getArtifactMetadata( ArtifactMetadata metadata, ArtifactRepository remoteRepository, File destination,
                              String checksumPolicy )
        throws TransferFailedException, ResourceDoesNotExistException;
}