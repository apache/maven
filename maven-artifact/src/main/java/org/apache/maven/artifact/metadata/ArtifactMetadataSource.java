package org.apache.maven.artifact.metadata;

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
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.versioning.ArtifactVersion;

import java.util.List;

/**
 * Provides some metadata operations, like querying the remote repository for a list of versions available for an
 * artifact.
 * 
 * @author <a href="mailto:jason@maven.org">Jason van Zyl </a>
 * @version $Id$
 */
public interface ArtifactMetadataSource
{
    String ROLE = ArtifactMetadataSource.class.getName();

    ResolutionGroup retrieve( Artifact artifact, ArtifactRepository localRepository, List remoteRepositories )
        throws ArtifactMetadataRetrievalException;

    /**
     * Get a list of available versions for an artifact in the remote repository
     * 
     * @param artifact artifact we are interested in. Only <code>groupid</code> and <code>artifactId</code>
     *                 are needed, for instance the following code will work 
     *                 <code>artifactFactory.createProjectArtifact( "org.apache.maven", "maven", "" )</code>
     * @param localRepository local repository
     * @param remoteRepositories remote repositories, {@link List} $lt; {@link ArtifactRepository} >
     * @return {@link List} $lt; {@link ArtifactVersion} >
     * @throws ArtifactMetadataRetrievalException
     */
    List retrieveAvailableVersions( Artifact artifact, ArtifactRepository localRepository, List remoteRepositories )
        throws ArtifactMetadataRetrievalException;
}