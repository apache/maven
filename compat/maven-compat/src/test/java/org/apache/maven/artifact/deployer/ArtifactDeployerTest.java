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

import javax.inject.Inject;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.apache.maven.artifact.AbstractArtifactComponentTestCase;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.session.scope.internal.SessionScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.codehaus.plexus.testing.PlexusExtension.getBasedir;
import static org.mockito.Mockito.mock;

/**
 */
@Deprecated
class ArtifactDeployerTest extends AbstractArtifactComponentTestCase {
    @Inject
    private ArtifactDeployer artifactDeployer;

    @Inject
    private SessionScope sessionScope;

    protected String component() {
        return "deployer";
    }

    @Test
    void artifactInstallation() throws Exception {
        sessionScope.enter();
        try {
            sessionScope.seed(MavenSession.class, mock(MavenSession.class));

            String artifactBasedir = new File(getBasedir(), "src/test/resources/artifact-install").getAbsolutePath();

            Artifact artifact = createArtifact("artifact", "1.0");

            File file = new File(artifactBasedir, "artifact-1.0.jar");
            assertThat(new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8).trim()).isEqualTo("dummy");

            artifactDeployer.deploy(file, artifact, remoteRepository(), localRepository());

            ArtifactRepository remoteRepository = remoteRepository();
            File deployedFile = new File(remoteRepository.getBasedir(), remoteRepository.pathOf(artifact));
            assertThat(deployedFile.exists()).isTrue();
            assertThat(new String(Files.readAllBytes(deployedFile.toPath()), StandardCharsets.UTF_8).trim()).isEqualTo("dummy");
        } finally {
            sessionScope.exit();
        }
    }
}
