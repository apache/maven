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
package org.apache.maven.repository.legacy.resolver.transform;

import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.deployer.ArtifactDeploymentException;
import org.apache.maven.artifact.installer.ArtifactInstallationException;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.RepositoryRequest;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;

/**
 * Manages multiple ArtifactTransformation instances and applies them in succession.
 */
public interface ArtifactTransformationManager {
    String ROLE = ArtifactTransformationManager.class.getName();

    /**
     * Take in a artifact and return the transformed artifact for locating in the remote repository. If no
     * transformation has occurred the original artifact is returned.
     *
     * @param artifact           Artifact to be transformed.
     * @param request the repositories to check
     */
    void transformForResolve(Artifact artifact, RepositoryRequest request)
            throws ArtifactResolutionException, ArtifactNotFoundException;

    /**
     * Take in a artifact and return the transformed artifact for locating in the remote repository. If no
     * transformation has occurred the original artifact is returned.
     *
     * @param artifact           Artifact to be transformed.
     * @param remoteRepositories the repositories to check
     * @param localRepository    the local repository
     */
    void transformForResolve(
            Artifact artifact, List<ArtifactRepository> remoteRepositories, ArtifactRepository localRepository)
            throws ArtifactResolutionException, ArtifactNotFoundException;

    /**
     * Take in a artifact and return the transformed artifact for locating in the local repository. If no
     * transformation has occurred the original artifact is returned.
     *
     * @param artifact        Artifact to be transformed.
     * @param localRepository the local repository it will be stored in
     */
    void transformForInstall(Artifact artifact, ArtifactRepository localRepository)
            throws ArtifactInstallationException;

    /**
     * Take in a artifact and return the transformed artifact for distributing to a remote repository. If no
     * transformation has occurred the original artifact is returned.
     *
     * @param artifact         Artifact to be transformed.
     * @param remoteRepository the repository to deploy to
     * @param localRepository  the local repository the metadata is stored in
     */
    void transformForDeployment(
            Artifact artifact, ArtifactRepository remoteRepository, ArtifactRepository localRepository)
            throws ArtifactDeploymentException;

    List<ArtifactTransformation> getArtifactTransformations();
}
