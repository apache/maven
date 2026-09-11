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

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration test for the {@code WorkspaceReader} SPI in maven-api-spi (MNG-8766).
 *
 * <p>Verifies that SPI workspace readers are:
 * <ol>
 *   <li>Discovered via DI and active during the build session</li>
 *   <li>Consulted for regular artifact resolution (dependency resolution, model building, etc.)</li>
 * </ol>
 *
 * <p>The {@code PluginRealmCache.invalidate(Artifact)} SPI allows IDE integrators to purge
 * stale plugin realms when workspace artifacts are rebuilt, rather than opting out of plugin
 * resolution entirely.
 *
 * @since 4.1.0
 */
class MavenITmng8766SpiWorkspaceReaderTest extends AbstractMavenIntegrationTestCase {

    @Test
    void testSpiWorkspaceReaderDiscoveredAndConsulted() throws Exception {
        Path testDir = extractResources("mng-8766-spi-workspace-reader");

        // First, install the extension
        Verifier verifier = newVerifier(testDir.resolve("extension"));
        verifier.addCliArgument("install");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        // Run the project that uses the extension — process-resources triggers artifact resolution
        verifier = newVerifier(testDir.resolve("project"));
        verifier.addCliArgument("process-resources");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        // Verify the SPI workspace reader was created (proves DI discovery works)
        verifier.verifyTextInLog("[SPI-WR] created");

        // Verify the SPI workspace reader was consulted during artifact resolution
        // (findArtifact is called for dependencies, model resolution, etc.)
        List<String> logLines = verifier.loadLogLines();
        boolean hasFindArtifactCalls =
                logLines.stream().anyMatch(line -> line.contains("[SPI-WR] findArtifact("));
        assertTrue(
                hasFindArtifactCalls,
                "SPI workspace reader should be consulted during artifact resolution");
    }
}
