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
package org.apache.maven.artifact.installer;

import javax.inject.Inject;

import java.io.File;

import org.apache.maven.artifact.AbstractArtifactComponentTestCase;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.session.scope.internal.SessionScope;
import org.junit.jupiter.api.Test;

import static org.codehaus.plexus.testing.PlexusExtension.getBasedir;
import static org.mockito.Mockito.mock;

/**
 */
@Deprecated
class ArtifactInstallerTest extends AbstractArtifactComponentTestCase {
    @Inject
    private ArtifactInstaller artifactInstaller;

    @Inject
    private SessionScope sessionScope;

    protected String component() {
        return "installer";
    }

    @Test
    void testArtifactInstallation() throws Exception {
        sessionScope.enter();
        try {
            sessionScope.seed(MavenSession.class, mock(MavenSession.class));

            String artifactBasedir = new File(getBasedir(), "src/test/resources/artifact-install").getAbsolutePath();

            Artifact artifact = createArtifact("artifact", "1.0");

            File source = new File(artifactBasedir, "artifact-1.0.jar");

            artifactInstaller.install(source, artifact, localRepository());

            assertLocalArtifactPresent(artifact);
        } finally {
            sessionScope.exit();
        }
    }
}
