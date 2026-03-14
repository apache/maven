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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * This is a test set for <a href="https://github.com/apache/maven/issues/11456">GH-11456</a>.
 * @since 4.1.0
 */
class MavenITgh11456MixinsConsumerPomTest extends AbstractMavenIntegrationTestCase {

    /**
     * Verify that Maven fails when a POM has non-empty mixins without flattening being enabled.
     */
    @Test
    void testMixinsWithoutFlattening() throws Exception {
        Path basedir = extractResources("/gh-11456-mixins-consumer-pom/non-flattened").toPath();

        Verifier verifier = newVerifier(basedir.toString());
        verifier.addCliArgument("-Dmaven.repo.local=" + basedir.resolve("repo"));
        verifier.addCliArgument("package");
        try {
            verifier.execute();
        } catch (VerificationException e) {
            // Expected to fail due to mixins without flattening
        }

        verifier.verifyTextInLog("cannot be created because the POM contains mixins");
        verifier.verifyTextInLog("maven.consumer.pom.flatten=true");
    }

    /**
     * Verify that Maven succeeds when mixins are used with flattening enabled.
     */
    @Test
    void testMixinsWithFlattening() throws Exception {
        Path basedir = extractResources("/gh-11456-mixins-consumer-pom/flattened").toPath();

        Verifier verifier = newVerifier(basedir.toString());
        verifier.addCliArgument("-Dmaven.repo.local=" + basedir.resolve("repo").toString());
        verifier.addCliArgument("package");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        // Verify consumer POM was created
        Path consumerPom = basedir.resolve(Paths.get(
                "target",
                "project-local-repo",
                "org.apache.maven.its.gh11456",
                "flattened",
                "1.0",
                "flattened-1.0-consumer.pom"));
        assertTrue(Files.exists(consumerPom), "consumer pom not found at " + consumerPom);

        // Verify mixins are removed from consumer POM
        Model consumerPomModel;
        try (Reader r = Files.newBufferedReader(consumerPom)) {
            consumerPomModel = new MavenStaxReader().read(r);
        }
        assertTrue(
                consumerPomModel.getMixins().isEmpty(),
                "Mixins should be removed from consumer POM when flattening is enabled");
    }

    /**
     * Verify that Maven succeeds when mixins are used with flattening enabled.
     */
    @Test
    void testMixinsWithPreserveModelVersion() throws Exception {
        Path basedir = extractResources("/gh-11456-mixins-consumer-pom/preserve-model-version").toPath();

        Verifier verifier = newVerifier(basedir.toString());
        verifier.addCliArgument("-Dmaven.repo.local=" + basedir.resolve("repo").toString());
        verifier.addCliArgument("package");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        // Verify consumer POM was created
        Path consumerPom = basedir.resolve(Paths.get(
                "target",
                "project-local-repo",
                "org.apache.maven.its.gh11456",
                "preserve-model-version",
                "1.0",
                "preserve-model-version-1.0-consumer.pom"));
        assertTrue(Files.exists(consumerPom), "consumer pom not found at " + consumerPom);

        // Verify mixins are removed from consumer POM
        Model consumerPomModel;
        try (Reader r = Files.newBufferedReader(consumerPom)) {
            consumerPomModel = new MavenStaxReader().read(r);
        }
        assertFalse(
                consumerPomModel.getMixins().isEmpty(),
                "Mixins should be kept in consumer POM when preserveModelVersion is enabled");
    }
}

