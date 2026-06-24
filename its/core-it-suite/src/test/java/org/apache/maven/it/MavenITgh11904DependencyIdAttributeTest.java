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
import java.util.List;
import java.util.stream.Stream;

import org.apache.maven.api.model.Model;
import org.apache.maven.model.v4.MavenStaxReader;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration test for <a href="https://github.com/apache/maven/pull/11904">GH-11904</a>.
 * Verifies that dependency {@code id} attributes are expanded into individual
 * GAV elements in the consumer POM and that no {@code id} attribute remains.
 */
class MavenITgh11904DependencyIdAttributeTest extends AbstractMavenIntegrationTestCase {

    MavenITgh11904DependencyIdAttributeTest() {
        super("[4.0.0-rc-3-SNAPSHOT,)");
    }

    @Test
    void testConsumerPomDoesNotContainIdAttributes() throws Exception {
        Path basedir = extractResources("/gh-11904-dependency-id-attribute");

        Verifier verifier = newVerifier(basedir);
        verifier.addCliArguments("install");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        // Verify consumer POM for the child module
        Path consumerPomPath = verifier.getArtifactPath(
                "org.apache.maven.its.gh-11904", "child", "1.0.0-SNAPSHOT", "pom");
        assertTrue(Files.exists(consumerPomPath), "consumer POM should exist at " + consumerPomPath);

        // Verify raw XML does not contain any id= attribute on dependency or exclusion elements
        List<String> consumerPomLines;
        try (Stream<String> lines = Files.lines(consumerPomPath)) {
            consumerPomLines = lines.toList();
        }
        assertTrue(
                consumerPomLines.stream()
                        .noneMatch(line -> line.contains("<dependency ") && line.contains("id=")),
                "Consumer POM should not contain 'id' attribute on <dependency> elements");
        assertTrue(
                consumerPomLines.stream()
                        .noneMatch(line -> line.contains("<exclusion ") && line.contains("id=")),
                "Consumer POM should not contain 'id' attribute on <exclusion> elements");

        // Parse and verify the consumer POM model
        Model consumerModel;
        try (Reader r = Files.newBufferedReader(consumerPomPath)) {
            consumerModel = new MavenStaxReader().read(r);
        }

        // Verify dependency was expanded correctly
        assertNotNull(consumerModel.getDependencies(), "Consumer POM should have dependencies");
        assertEquals(1, consumerModel.getDependencies().size(), "Consumer POM should have one dependency");

        var dep = consumerModel.getDependencies().get(0);
        assertNull(dep.getId(), "Dependency id attribute should be null in consumer POM");
        assertEquals("org.apache.maven.its.gh-11904", dep.getGroupId());
        assertEquals("lib-a", dep.getArtifactId());
        assertEquals("1.0.0-SNAPSHOT", dep.getVersion());
    }
}
