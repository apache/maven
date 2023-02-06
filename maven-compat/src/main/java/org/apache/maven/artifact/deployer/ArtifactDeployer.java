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
package org.apache.maven.artifact.deployer;

import java.io.File;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;

/**
 * ArtifactDeployer
 */
public interface ArtifactDeployer {
    String ROLE = ArtifactDeployer.class.getName();

    /**
     * Deploy an artifact from a particular directory. The artifact handler is used to determine the
     * filename of the source file.
     *
     * @param basedir the directory where the artifact is stored
     * @param finalName the name of the artifact without extension
     * @param artifact the artifact definition
     * @param deploymentRepository the repository to deploy to
     * @param localRepository the local repository to install into
     * @throws ArtifactDeploymentException if an error occurred deploying the artifact
     * @deprecated to be removed before 2.0 after the install/deploy plugins use the alternate
     *             method
     */
    @Deprecated
    void deploy(
            String basedir,
            String finalName,
            Artifact artifact,
            ArtifactRepository deploymentRepository,
            ArtifactRepository localRepository)
            throws ArtifactDeploymentException;

    /**
     * Deploy an artifact from a particular file.
     *
     * @param source the file to deploy
     * @param artifact the artifact definition
     * @param deploymentRepository the repository to deploy to
     * @param localRepository the local repository to install into
     * @throws ArtifactDeploymentException if an error occurred deploying the artifact
     */
    void deploy(
            File source, Artifact artifact, ArtifactRepository deploymentRepository, ArtifactRepository localRepository)
            throws ArtifactDeploymentException;
}
