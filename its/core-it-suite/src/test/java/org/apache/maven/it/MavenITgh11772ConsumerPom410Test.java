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
package org.apache.maven.it;

import java.io.File;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.maven.api.model.Model;
import org.apache.maven.model.v4.MavenStaxReader;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration test for <a href="https://github.com/apache/maven/issues/11772">GH-11772</a>.
 * <p>
 * Verifies that when a parent+child project uses model version 4.1.0 namespace
 * (with subprojects, root), the installed consumer POMs are model version 4.0.0,
 * while the build POMs retain the original 4.1.0 content.
 * <p>
 * This ensures backward compatibility with Maven 3 and Gradle for consumer POMs
 * while Maven 4 builds can resolve the full-fidelity build POM.
 */
class MavenITgh11772ConsumerPom410Test extends AbstractMavenIntegrationTestCase {

    private static final String GROUP_ID = "org.apache.maven.its.gh11772";

    @Test
    void testConsumerPomsAre400BuildPomsAre410() throws Exception {
        File basedir = extractResources("/gh-11772-consumer-pom-410");

        Verifier verifier = newVerifier(basedir.getAbsolutePath());
        verifier.deleteArtifacts(GROUP_ID);
        verifier.addCliArguments("install");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        // Verify parent consumer POM (main artifact) is 4.0.0
        Path parentConsumerPom =
                Path.of(verifier.getArtifactPath(GROUP_ID, "parent", "1.0.0-SNAPSHOT", "pom"));
        assertTrue(Files.exists(parentConsumerPom), "Parent consumer POM should exist");
        Model parentConsumer = readModel(parentConsumerPom);
        assertEquals("4.0.0", parentConsumer.getModelVersion(), "Parent consumer POM should be 4.0.0");

        // Verify parent build POM retains 4.1.0 features
        Path parentBuildPom =
                Path.of(verifier.getArtifactPath(GROUP_ID, "parent", "1.0.0-SNAPSHOT", "pom", "build"));
        assertTrue(Files.exists(parentBuildPom), "Parent build POM should exist");
        Model parentBuild = readModel(parentBuildPom);
        // Build POM should retain subprojects (4.1.0 feature)
        assertNotNull(parentBuild.getSubprojects(), "Build POM should retain subprojects");
        assertTrue(!parentBuild.getSubprojects().isEmpty(), "Build POM should retain subprojects");

        // Verify child consumer POM is 4.0.0
        Path childConsumerPom =
                Path.of(verifier.getArtifactPath(GROUP_ID, "child", "1.0.0-SNAPSHOT", "pom"));
        assertTrue(Files.exists(childConsumerPom), "Child consumer POM should exist");
        Model childConsumer = readModel(childConsumerPom);
        assertEquals("4.0.0", childConsumer.getModelVersion(), "Child consumer POM should be 4.0.0");

        // Child consumer POM should have a parent reference (not flattened by default)
        assertNotNull(childConsumer.getParent(), "Child consumer POM should have a parent reference");
        assertEquals(GROUP_ID, childConsumer.getParent().getGroupId());
        assertEquals("parent", childConsumer.getParent().getArtifactId());

        // Verify child build POM exists
        Path childBuildPom =
                Path.of(verifier.getArtifactPath(GROUP_ID, "child", "1.0.0-SNAPSHOT", "pom", "build"));
        assertTrue(Files.exists(childBuildPom), "Child build POM should exist");
    }

    private static Model readModel(Path pomFile) throws Exception {
        try (Reader r = Files.newBufferedReader(pomFile)) {
            return new MavenStaxReader().read(r);
        }
    }
}
