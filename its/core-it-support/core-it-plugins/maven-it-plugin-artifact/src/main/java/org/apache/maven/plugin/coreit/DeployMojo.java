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
package org.apache.maven.plugin.coreit;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.deployer.ArtifactDeployer;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Deploys the project artifacts to the distribution repository. This is the essence of the Maven Deploy Plugin.
 *
 * @author Benjamin Bentmann
 *
 */
@Mojo(name = "deploy", defaultPhase = LifecyclePhase.DEPLOY)
public class DeployMojo extends AbstractRepoMojo {

    /**
     * The distribution repository.
     */
    @Parameter(defaultValue = "${project.distributionManagementArtifactRepository}", readonly = true, required = true)
    private ArtifactRepository deploymentRepository;

    /**
     * The artifact deployer.
     *
     */
    @Component
    private ArtifactDeployer deployer;

    /**
     * Runs this mojo.
     *
     * @throws MojoExecutionException If any artifact could not be installed.
     */
    public void execute() throws MojoExecutionException {
        getLog().info("[MAVEN-CORE-IT-LOG] Deploying project artifacts");

        try {
            if (isPomArtifact()) {
                deployer.deploy(pomFile, mainArtifact, deploymentRepository, localRepository);
            } else {
                deployer.deploy(mainArtifact.getFile(), mainArtifact, deploymentRepository, localRepository);
            }

            if (attachedArtifacts != null) {
                for (Artifact attachedArtifact : attachedArtifacts) {
                    deployer.deploy(
                            attachedArtifact.getFile(), attachedArtifact, deploymentRepository, localRepository);
                }
            }
        } catch (Exception e) {
            throw new MojoExecutionException("Failed to deploy artifacts: " + e.getMessage(), e);
        }
    }
}
