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

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verify that the consumer POM builder correctly resolves properties from
 * OS-activated profiles in parent POMs when those properties are used in
 * child dependency artifactId coordinates.
 * <p>
 * This simulates the Apache Hop scenario where the root POM uses OS-activated
 * profiles to set platform-specific dependency artifactIds (e.g., SWT) and
 * dependency management entries that reference those properties.
 * <p>
 * The consumer POM builder must resolve these properties before coordinate
 * validation, otherwise it fails with:
 * <pre>
 *   'dependencies.dependency.artifactId' ... with value '${swt.artifactId}'
 *   does not match a valid coordinate id pattern.
 * </pre>
 *
 * @see <a href="https://github.com/apache/maven/issues/13004">GH-13004</a>
 */
class MavenITgh13004ConsumerPomProfileArtifactIdTest extends AbstractMavenIntegrationTestCase {

    MavenITgh13004ConsumerPomProfileArtifactIdTest() {
        super("[4.0.0-rc-7,)");
    }

    /**
     * Test that the build succeeds when a parent POM defines OS-activated profiles
     * with properties used in child dependency artifactIds. The default (non-flattened)
     * consumer POM preserves the parent reference and profile activation, so consumers
     * can resolve the property through the parent.
     */
    @Test
    void testConsumerPomResolvesOsProfilePropertyInArtifactId() throws Exception {
        Path basedir = extractResources("/gh-13004-consumer-pom-profile-artifactid")
                .getAbsoluteFile()
                .toPath();

        Verifier verifier = newVerifier(basedir.toString());
        verifier.addCliArgument("install");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        // Verify the parent consumer POM preserves the OS-activated profiles
        Path parentConsumerPom = Path.of(verifier.getArtifactPath(
                "org.apache.maven.its.gh13004", "parent", "1.0-SNAPSHOT", "pom"));
        assertTrue(Files.exists(parentConsumerPom), "Parent consumer POM should exist");
        String parentContent = Files.readString(parentConsumerPom);
        assertTrue(
                parentContent.contains("<family>unix</family>") || parentContent.contains("<family>windows</family>"),
                "Parent consumer POM should preserve OS-activation profiles");
        assertTrue(
                parentContent.contains("platform.artifactId"),
                "Parent consumer POM should preserve the profile property definition");
    }

    /**
     * Test that flattened consumer POM generation succeeds and fully resolves the
     * profile property in the dependency artifactId. With flattening enabled, the
     * effective (interpolated) model is used, so the consumer POM must contain the
     * resolved artifactId, not the raw ${platform.artifactId} reference.
     */
    @Test
    void testFlattenedConsumerPomResolvesOsProfilePropertyInArtifactId() throws Exception {
        Path basedir = extractResources("/gh-13004-consumer-pom-profile-artifactid")
                .getAbsoluteFile()
                .toPath();

        Verifier verifier = newVerifier(basedir.toString());
        verifier.addCliArgument("-Dmaven.consumer.pom.flatten=true");
        verifier.addCliArgument("install");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        // With flattening, the consumer POM uses the effective model:
        // ${platform.artifactId} must be resolved to "lib"
        Path childConsumerPom = Path.of(verifier.getArtifactPath(
                "org.apache.maven.its.gh13004", "child", "1.0-SNAPSHOT", "pom"));
        assertTrue(Files.exists(childConsumerPom), "Child consumer POM should exist");
        String childContent = Files.readString(childConsumerPom);
        assertFalse(
                childContent.contains("${platform.artifactId}"),
                "Flattened consumer POM should not contain unresolved ${platform.artifactId}");
        assertTrue(
                childContent.contains("<artifactId>lib</artifactId>"),
                "Flattened consumer POM should contain the resolved artifactId 'lib'");
    }
}
