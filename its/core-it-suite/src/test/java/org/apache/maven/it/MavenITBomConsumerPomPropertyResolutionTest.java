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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that the BOM consumer POM resolves property-referenced dependency
 * versions, especially when the property is defined in a parent POM.
 * <p>
 * Reproducer for the issue reported by Karl Heinz Marbaise during the
 * Maven 4.0.0-rc-6 vote: when a BOM's {@code <dependencyManagement>} uses a
 * property like {@code ${junit.version}} for an external dependency version,
 * and that property is defined in the parent POM, the consumer POM leaves the
 * property reference unresolved. This happens because
 * {@code DefaultConsumerPomBuilder.buildBomWithoutFlatten()} uses
 * {@code getRawModel()} (no interpolation) and then {@code transformBom()}
 * strips the parent and properties — leaving dangling {@code ${...}} references.
 *
 * @see <a href="https://lists.apache.org/thread/2s2myrg0zzk0q5z9kjhm8pwsntxmtnxh">Vote thread</a>
 * @since 4.0.0
 */
class MavenITBomConsumerPomPropertyResolutionTest extends AbstractMavenIntegrationTestCase {

    MavenITBomConsumerPomPropertyResolutionTest() {
        super("[4.0.0-rc-4,)");
    }

    /**
     * Verify that the BOM consumer POM (default, no flatten) resolves
     * property-referenced versions in dependencyManagement.
     * <p>
     * The parent POM defines {@code junit.version=4.13.2}. The BOM child
     * references it as {@code ${junit.version}}. The consumer POM must
     * contain the resolved literal {@code 4.13.2}, not the raw property
     * expression, because the consumer POM has no parent or properties
     * section to resolve it from.
     */
    @Test
    void testBomConsumerPomResolvesParentProperties() throws Exception {
        Path basedir =
                extractResources("/bom-consumer-pom-property-resolution").getAbsoluteFile().toPath();

        Verifier verifier = newVerifier(basedir.toString());
        verifier.deleteArtifacts("org.apache.maven.its.bom-property");
        verifier.addCliArguments("install");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        // Read the consumer POM that was installed to the local repo
        Path consumerPomPath = Paths.get(
                verifier.getArtifactPath("org.apache.maven.its.bom-property", "bom", "1.0.0-SNAPSHOT", "pom"));

        assertTrue(Files.exists(consumerPomPath), "Consumer POM not found at " + consumerPomPath);

        List<String> lines;
        try (Stream<String> stream = Files.lines(consumerPomPath)) {
            lines = stream.toList();
        }
        String content = String.join("\n", lines);

        // 1. Packaging must be "pom" (not "bom")
        assertTrue(
                content.contains("<packaging>pom</packaging>"),
                "Consumer POM packaging should be 'pom', not 'bom'");

        // 2. The consumer POM strips parent and properties, so inherited
        //    fields must be inlined: groupId and version must be present
        assertTrue(
                content.contains("<groupId>org.apache.maven.its.bom-property</groupId>"),
                "Consumer POM must have groupId inlined (parent is stripped).\nActual:\n" + content);
        assertTrue(
                content.contains("<version>1.0.0-SNAPSHOT</version>"),
                "Consumer POM must have version inlined (parent is stripped).\nActual:\n" + content);

        // 3. The key assertion: no unresolved ${...} property references
        //    anywhere in the consumer POM. The parent and properties sections
        //    are removed, so any remaining ${...} would be unresolvable.
        assertFalse(
                content.contains("${"),
                "Consumer POM must not contain unresolved property references.\nActual:\n" + content);

        // 4. Specifically: junit version must be the resolved literal "4.13.2"
        assertTrue(
                content.contains("<version>4.13.2</version>"),
                "Consumer POM must contain resolved junit version '4.13.2'.\nActual:\n" + content);
    }

    /**
     * Verify that the BOM consumer POM with flatten=true also resolves
     * property-referenced versions (this path uses getEffectiveModel()
     * and should already work).
     */
    @Test
    void testBomConsumerPomWithFlattenResolvesParentProperties() throws Exception {
        Path basedir =
                extractResources("/bom-consumer-pom-property-resolution").getAbsoluteFile().toPath();

        Verifier verifier = newVerifier(basedir.toString());
        verifier.deleteArtifacts("org.apache.maven.its.bom-property");
        verifier.addCliArguments("install", "-Dmaven.consumer.pom.flatten=true");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        Path consumerPomPath = Paths.get(
                verifier.getArtifactPath("org.apache.maven.its.bom-property", "bom", "1.0.0-SNAPSHOT", "pom"));

        assertTrue(Files.exists(consumerPomPath), "Consumer POM not found at " + consumerPomPath);

        List<String> lines;
        try (Stream<String> stream = Files.lines(consumerPomPath)) {
            lines = stream.toList();
        }
        String content = String.join("\n", lines);

        // Packaging must be "pom"
        assertTrue(content.contains("<packaging>pom</packaging>"), "Consumer POM packaging should be 'pom'");

        // No unresolved property references
        assertFalse(
                content.contains("${"),
                "Consumer POM must not contain unresolved property references.\nActual:\n" + content);

        // junit version must be the resolved literal
        assertTrue(
                content.contains("<version>4.13.2</version>"),
                "Consumer POM must contain resolved junit version '4.13.2'.\nActual:\n" + content);
    }

    /**
     * Same test as {@link #testBomConsumerPomResolvesParentProperties} but verifies
     * the consumer POM that gets written to the project-local repo (not user-home),
     * using the "-consumer" classifier POM.
     */
    @Test
    void testConsumerPomInProjectLocalRepo() throws Exception {
        Path basedir =
                extractResources("/bom-consumer-pom-property-resolution").getAbsoluteFile().toPath();

        Verifier verifier = newVerifier(basedir.toString());
        verifier.deleteArtifacts("org.apache.maven.its.bom-property");
        verifier.addCliArguments("install");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        // Check the consumer POM in the project-local-repo
        Path consumerPom = basedir.resolve(Path.of(
                "target",
                "project-local-repo",
                "org.apache.maven.its.bom-property",
                "bom",
                "1.0.0-SNAPSHOT",
                "bom-1.0.0-SNAPSHOT-consumer.pom"));

        assertTrue(Files.exists(consumerPom), "Consumer POM not found at " + consumerPom);

        String content = Files.readString(consumerPom);

        // Must not contain raw property references
        assertFalse(
                content.contains("${"),
                "Consumer POM must not contain unresolved property references.\nActual:\n" + content);

        // Must contain the resolved version
        assertTrue(
                content.contains("<version>4.13.2</version>"),
                "Consumer POM must contain resolved junit version '4.13.2'.\nActual:\n" + content);

        // Must have packaging=pom
        assertEquals(-1, content.indexOf("<packaging>bom</packaging>"), "Consumer POM must not have bom packaging");
        assertTrue(content.contains("<packaging>pom</packaging>"), "Consumer POM must have pom packaging");
    }
}
