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

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.maven.api.model.Model;
import org.apache.maven.model.v4.MavenStaxReader;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Integration test for <a href="https://github.com/apache/maven/issues/11772">GH-11772</a>.
 * <p>
 * Verifies that when a POM-packaged project (parent) uses model version 4.1.0 features
 * (like profile condition activation), Maven generates dual consumer POMs:
 * <ul>
 *   <li>Main POM (no classifier): 4.0.0-compatible, 4.1.0 features stripped</li>
 *   <li>Consumer classifier POM: 4.1.0 full-fidelity, parent references consumer classifier</li>
 * </ul>
 */
class MavenITgh11772DualConsumerPomTest extends AbstractMavenIntegrationTestCase {

    MavenITgh11772DualConsumerPomTest() {
        super("[4.0.0-rc-2,)");
    }

    private static final String GROUP_ID = "org.apache.maven.its.gh11772";

    @Test
    void testDualConsumerPomsForParent() throws Exception {
        Path basedir = extractResources("/gh-11772-dual-consumer-pom").toPath();

        Verifier verifier = newVerifier(basedir.toString(), null);
        verifier.addCliArguments("install");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        // Verify parent POM artifacts in project-local-repo
        Path parentDir = basedir.resolve(Paths.get(
                "target",
                "project-local-repo",
                "org.apache.maven.its.gh11772",
                "parent",
                "1.0.0-SNAPSHOT"));

        Path parentMainPom = parentDir.resolve("parent-1.0.0-SNAPSHOT.pom");
        Path parentConsumerPom = parentDir.resolve("parent-1.0.0-SNAPSHOT-consumer.pom");
        Path parentBuildPom = parentDir.resolve("parent-1.0.0-SNAPSHOT-build.pom");

        assertTrue("Parent main POM should exist", Files.exists(parentMainPom));
        assertTrue("Parent consumer POM should exist", Files.exists(parentConsumerPom));
        assertTrue("Parent build POM should exist", Files.exists(parentBuildPom));

        // Main POM should be 4.0.0-compatible
        Model mainModel;
        try (Reader r = Files.newBufferedReader(parentMainPom)) {
            mainModel = new MavenStaxReader().read(r);
        }
        assertEquals("4.0.0", mainModel.getModelVersion());

        // Main POM should NOT have condition activation (stripped for 4.0.0 compat)
        for (var profile : mainModel.getProfiles()) {
            if (profile.getActivation() != null) {
                assertNull(
                        "Main POM profiles should not have condition activation",
                        profile.getActivation().getCondition());
            }
        }

        // Consumer POM should be 4.1.0 full-fidelity
        Model consumerModel;
        try (Reader r = Files.newBufferedReader(parentConsumerPom)) {
            consumerModel = new MavenStaxReader().read(r);
        }
        assertEquals("4.1.0", consumerModel.getModelVersion());

        // Verify child POM
        Path childDir = basedir.resolve(Paths.get(
                "target",
                "project-local-repo",
                "org.apache.maven.its.gh11772",
                "child",
                "1.0.0-SNAPSHOT"));
        Path childMainPom = childDir.resolve("child-1.0.0-SNAPSHOT.pom");
        assertTrue("Child main POM should exist", Files.exists(childMainPom));

        Model childModel;
        try (Reader r = Files.newBufferedReader(childMainPom)) {
            childModel = new MavenStaxReader().read(r);
        }
        assertNotNull("Child POM should have a parent reference", childModel.getParent());
        assertEquals(GROUP_ID, childModel.getParent().getGroupId());
        assertEquals("parent", childModel.getParent().getArtifactId());
        assertNull(
                "Child POM parent should not have a classifier",
                childModel.getParent().getClassifier());
    }
}
