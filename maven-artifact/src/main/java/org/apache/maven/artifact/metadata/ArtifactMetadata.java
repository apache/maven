package org.apache.maven.artifact.metadata;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.manager.WagonManager;
import org.apache.maven.artifact.repository.ArtifactRepository;

/**
 * Contains metadata about an artifact, and methods to retrieve/store it from an artifact repository.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id$
 * @todo merge with artifactmetadatasource
 * @todo retrieval exception not appropriate for store
 * @todo not happy about the store/retrieve methods - they use "this"
 */
public interface ArtifactMetadata
{
    /**
     * Store the metadata in the local repository.
     *
     * @param localRepository the local repository
     */
    void storeInLocalRepository( ArtifactRepository localRepository )
        throws ArtifactMetadataRetrievalException;

    /**
     * Retrieve the metadata from the remote repository into the local repository.
     *
     * @param remoteRepository the remote repository
     * @param wagonManager     the wagon manager to use to retrieve the metadata
     */
    public void retrieveFromRemoteRepository( ArtifactRepository remoteRepository, WagonManager wagonManager )
        throws ArtifactMetadataRetrievalException;

    /**
     * Get the associated artifact.
     *
     * @return the artifact
     */
    Artifact getArtifact();

    /**
     * Get the filename of this metadata.
     *
     * @return the filename
     */
    String getFilename();
}
