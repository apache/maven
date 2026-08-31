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
 * Verifies that the BOM consumer POM contains only the dependency management
 * entries explicitly declared in the BOM module, not entries inherited from the
 * parent POM.
 * <p>
 * Reproducer for <a href="https://github.com/apache/maven/issues/12640">#12640</a>:
 * when the parent POM defines dependency management entries (or imports BOMs),
 * those entries were leaking into the BOM's consumer POM because
 * {@code DefaultConsumerPomBuilder.buildBom()} used the effective model which
 * includes inherited dependency management.
 *
 * @since 4.0.0
 */
class MavenITgh12640BomInheritedDepMgmtTest extends AbstractMavenIntegrationTestCase {

    /**
     * Verify that the BOM consumer POM contains only entries declared in the BOM,
     * not entries inherited from the parent's dependency management.
     * <p>
     * The parent defines {@code ext-lib-a} and {@code ext-lib-b} in its
     * dependencyManagement. The BOM declares only {@code mod-1} and {@code mod-2}.
     * The consumer POM must NOT contain {@code ext-lib-a} or {@code ext-lib-b}.
     */
    @Test
    void testBomConsumerPomExcludesInheritedDepMgmt() throws Exception {
        Path basedir = extractResources("/gh-12640-bom-inherited-depmgmt");

        Verifier verifier = newVerifier(basedir);
        verifier.deleteArtifacts("org.apache.maven.its.gh12640");
        verifier.addCliArguments("install");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        // Read the consumer POM that was installed to the local repo
        Path consumerPomPath =
                verifier.getArtifactPath("org.apache.maven.its.gh12640", "bom", "1.0.0-SNAPSHOT", "pom");

        assertTrue(Files.exists(consumerPomPath), "Consumer POM not found at " + consumerPomPath);

        String content = Files.readString(consumerPomPath);

        // 1. Packaging must be "pom" (not "bom")
        assertTrue(content.contains("<packaging>pom</packaging>"), "Consumer POM packaging should be 'pom'");

        // 2. Must contain the declared entries: mod-1 and mod-2
        assertTrue(
                content.contains("<artifactId>mod-1</artifactId>"),
                "Consumer POM must contain mod-1.\nActual:\n" + content);
        assertTrue(
                content.contains("<artifactId>mod-2</artifactId>"),
                "Consumer POM must contain mod-2.\nActual:\n" + content);

        // 3. Must NOT contain inherited entries from the parent's dependency management
        assertFalse(
                content.contains("<artifactId>ext-lib-a</artifactId>"),
                "Consumer POM must NOT contain parent's ext-lib-a.\nActual:\n" + content);
        assertFalse(
                content.contains("<artifactId>ext-lib-b</artifactId>"),
                "Consumer POM must NOT contain parent's ext-lib-b.\nActual:\n" + content);

        // 4. Must have resolved versions (not raw property references)
        assertFalse(
                content.contains("${"),
                "Consumer POM must not contain unresolved property references.\nActual:\n" + content);

        // 5. Versions must be inferred from the reactor (1.0.0-SNAPSHOT)
        assertTrue(
                content.contains("<version>1.0.0-SNAPSHOT</version>"),
                "Consumer POM must contain resolved reactor version.\nActual:\n" + content);
    }

    /**
     * Same test with flatten enabled — the behaviour should be the same for BOMs.
     */
    @Test
    void testBomConsumerPomWithFlattenExcludesInheritedDepMgmt() throws Exception {
        Path basedir = extractResources("/gh-12640-bom-inherited-depmgmt");

        Verifier verifier = newVerifier(basedir);
        verifier.deleteArtifacts("org.apache.maven.its.gh12640");
        verifier.addCliArguments("install", "-Dmaven.consumer.pom.flatten=true");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        Path consumerPomPath =
                verifier.getArtifactPath("org.apache.maven.its.gh12640", "bom", "1.0.0-SNAPSHOT", "pom");

        assertTrue(Files.exists(consumerPomPath), "Consumer POM not found at " + consumerPomPath);

        String content = Files.readString(consumerPomPath);

        // Must contain declared entries
        assertTrue(content.contains("<artifactId>mod-1</artifactId>"), "Must contain mod-1");
        assertTrue(content.contains("<artifactId>mod-2</artifactId>"), "Must contain mod-2");

        // Must NOT contain inherited entries
        assertFalse(
                content.contains("<artifactId>ext-lib-a</artifactId>"),
                "Consumer POM must NOT contain parent's ext-lib-a.\nActual:\n" + content);
        assertFalse(
                content.contains("<artifactId>ext-lib-b</artifactId>"),
                "Consumer POM must NOT contain parent's ext-lib-b.\nActual:\n" + content);
    }
}
