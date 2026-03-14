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
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * This is a test set for BOM consumer POM issues.
 * Verifies that:
 * 1. BOM packaging is transformed to POM in consumer POMs (not "bom" which is invalid in Maven 4.0.0)
 * 2. Dependency versions are preserved in dependencyManagement when using flatten=true
 *
 * @since 4.0.0
 */
class MavenITgh11427BomConsumerPomTest extends AbstractMavenIntegrationTestCase {

    /**
     * Verify BOM consumer POM without flattening has correct packaging.
     */
    @Test
    void testBomConsumerPomWithoutFlatten() throws Exception {
        Path basedir = extractResources("/gh-11427-bom-consumer-pom")
                .getAbsoluteFile()
                .toPath();

        Verifier verifier = newVerifier(basedir.toString());
        verifier.addCliArguments("install");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        Path consumerPomPath = Paths.get(
                verifier.getArtifactPath("org.apache.maven.its.gh-11427", "bom", "1.0.0-SNAPSHOT", "pom"));

        assertTrue(Files.exists(consumerPomPath), "consumer pom not found at " + consumerPomPath);

        List<String> consumerPomLines;
        try (Stream<String> lines = Files.lines(consumerPomPath)) {
            consumerPomLines = lines.toList();
        }

        // Verify packaging is "pom" not "bom"
        assertTrue(
                consumerPomLines.stream().anyMatch(s -> s.contains("<packaging>pom</packaging>")),
                "Consumer pom should have <packaging>pom</packaging>");
        assertFalse(
                consumerPomLines.stream().anyMatch(s -> s.contains("<packaging>bom</packaging>")),
                "Consumer pom should NOT have <packaging>bom</packaging>");

        // Verify dependencyManagement is present
        assertTrue(
                consumerPomLines.stream().anyMatch(s -> s.contains("<dependencyManagement>")),
                "Consumer pom should have dependencyManagement");
    }

    /**
     * Verify BOM consumer POM with flattening has correct packaging and versions.
     */
    @Test
    void testBomConsumerPomWithFlatten() throws Exception {
        Path basedir = extractResources("/gh-11427-bom-consumer-pom")
                .getAbsoluteFile()
                .toPath();

        Verifier verifier = newVerifier(basedir.toString());
        verifier.addCliArguments("install", "-Dmaven.consumer.pom.flatten=true");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        Path consumerPomPath = Paths.get(
                verifier.getArtifactPath("org.apache.maven.its.gh-11427", "bom", "1.0.0-SNAPSHOT", "pom"));

        assertTrue(Files.exists(consumerPomPath), "consumer pom not found at " + consumerPomPath);

        List<String> consumerPomLines;
        try (Stream<String> lines = Files.lines(consumerPomPath)) {
            consumerPomLines = lines.toList();
        }

        // Verify packaging is "pom" not "bom"
        assertTrue(
                consumerPomLines.stream().anyMatch(s -> s.contains("<packaging>pom</packaging>")),
                "Consumer pom should have <packaging>pom</packaging>");
        assertFalse(
                consumerPomLines.stream().anyMatch(s -> s.contains("<packaging>bom</packaging>")),
                "Consumer pom should NOT have <packaging>bom</packaging>");

        // Verify dependencyManagement is present
        assertTrue(
                consumerPomLines.stream().anyMatch(s -> s.contains("<dependencyManagement>")),
                "Consumer pom should have dependencyManagement");

        // Verify versions are present in dependencies
        String content = String.join("\n", consumerPomLines);
        assertTrue(
                content.contains("<version>1.0.0-SNAPSHOT</version>") || content.contains("<version>${"),
                "Consumer pom should have version for module dependency");
        assertTrue(
                content.contains("<version>4.13.2</version>"),
                "Consumer pom should have version for junit dependency");
    }
}

