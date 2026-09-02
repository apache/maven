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
import java.nio.file.Files;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration test for the {@code ToolchainPluginStrategy} in {@code mvnup}.
 * <p>
 * Verifies that running {@code mvn --up apply} on a project with an old
 * {@code --source} level (unsupported by the running JDK) automatically adds
 * the {@code maven-toolchains-plugin} with the {@code select-jdk-toolchain}
 * goal and the correct {@code <version>} constraint, and that a second run
 * is idempotent.
 * <p>
 * Uses {@code mvn --up} instead of the {@code mvnup} script directly.
 * The {@code --up} flag is handled by the {@code mvn} launcher script
 * which sets {@code -Dmaven.mainClass=MavenUpCling}, and {@code MavenCling}
 * (the default entry point since MNG-2520) delegates to it.
 *
 * @since 4.0.0-rc-7
 */
public class MavenITMvnupToolchainPluginStrategyTest extends AbstractMavenIntegrationTestCase {

    public MavenITMvnupToolchainPluginStrategyTest() {
        super(ALL_MAVEN_VERSIONS);
    }

    /**
     * Verify that mvnup adds the maven-toolchains-plugin with select-jdk-toolchain
     * goal and version constraint when the project's source level is unsupported
     * by the running JDK, and that a second run is idempotent.
     */
    @Test
    public void testMvnupAddsToolchainsPluginForOldSourceLevel() throws Exception {
        File testDir = extractResources("/mvnup-toolchain-plugin-strategy");

        // First run — should add the plugin
        // Use "mvn --up" to invoke mvnup via the MavenCling delegation (MNG-2520)
        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setForkJvm(true);
        verifier.setLogFileName("first-run.txt");
        verifier.addCliArgument("--up");
        verifier.addCliArgument("apply");
        verifier.addCliArgument("-d");
        verifier.addCliArgument(testDir.getAbsolutePath());
        verifier.execute();
        verifier.verifyErrorFreeLog();

        // Verify mvnup reported adding the plugin
        verifier.verifyTextInLog("Added maven-toolchains-plugin with select-jdk-toolchain goal");

        // Verify the POM was modified to include the toolchains plugin
        String pomContent = Files.readString(testDir.toPath().resolve("pom.xml"));
        assertTrue(
                pomContent.contains("maven-toolchains-plugin"),
                "POM should contain maven-toolchains-plugin after mvnup apply");
        assertTrue(
                pomContent.contains("select-jdk-toolchain"),
                "POM should contain select-jdk-toolchain goal after mvnup apply");
        assertTrue(
                pomContent.contains("<version>(,8]</version>"),
                "POM should contain version constraint (,8] for source 5 after mvnup apply");
        assertTrue(
                pomContent.contains("<version>3.2.0</version>"),
                "POM should contain plugin version 3.2.0 (minimum for select-jdk-toolchain goal)");

        // Second run — should be idempotent (skip, already present)
        verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setForkJvm(true);
        verifier.setLogFileName("second-run.txt");
        verifier.addCliArgument("--up");
        verifier.addCliArgument("apply");
        verifier.addCliArgument("-d");
        verifier.addCliArgument(testDir.getAbsolutePath());
        verifier.execute();
        verifier.verifyErrorFreeLog();
        verifier.verifyTextInLog("maven-toolchains-plugin with select-jdk-toolchain goal already configured");
    }
}
