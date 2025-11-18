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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * With the build-consumer the pom.xml will be adjusted during the process.
 * <ul>
 *   <li>CLI-friendly versions will be resolved</li>
 *   <li>parents can omit the version if the relative path points to the correct parent</li>
 *   <li>dependencies can omit the version if it is part of the reactor</li>
 * </ul>
 *
 * During install the pom will be cleaned up
 * <ul>
 *   <li>the modules will be removed</li>
 *   <li>the relativePath will be removed</li>
 * </ul>
 *
 * <a href="https://issues.apache.org/jira/browse/MNG-6656">MNG-6656</a>.
 *
 */
public class MavenITmng6656BuildConsumer extends AbstractMavenIntegrationTestCase {

    /**
     * Verifies:
     * <ul>
     *   <li>preserve license</li>
     *   <li>consistent line separators</li>
     *   <li>resolved project versions (at least 2 levels deep) in parent and dependencies</li>
     *   <li>removal of modules in aggregators</li>
     * </ul>
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testPublishedPoms() throws Exception {
        Path testDir = extractResources("mng-6656-buildconsumer");

        Verifier verifier = newVerifier(testDir);
        verifier.setAutoclean(false);
        verifier.addCliArgument("-Dchangelist=MNG6656");

        verifier.addCliArguments("install", "-Dmaven.consumer.pom.flatten=true");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        assertTextEquals(
                testDir.resolve("expected/parent.pom"),
                Path.of(verifier.getArtifactPath(
                        "org.sonatype.mavenbook.multi", "parent", "0.9-MNG6656-SNAPSHOT", "pom")));

        assertTextEquals(
                testDir.resolve("expected/parent-build.pom"),
                Path.of(verifier.getArtifactPath(
                        "org.sonatype.mavenbook.multi", "parent", "0.9-MNG6656-SNAPSHOT", "pom", "build")));

        assertTextEquals(
                testDir.resolve("expected/simple-parent.pom"),
                Path.of(verifier.getArtifactPath(
                        "org.sonatype.mavenbook.multi", "simple-parent", "0.9-MNG6656-SNAPSHOT", "pom")));

        assertTextEquals(
                testDir.resolve("expected/simple-weather.pom"),
                Path.of(verifier.getArtifactPath(
                        "org.sonatype.mavenbook.multi", "simple-weather", "0.9-MNG6656-SNAPSHOT", "pom")));

        assertTextEquals(
                testDir.resolve("expected/simple-weather-build.pom"),
                Path.of(verifier.getArtifactPath(
                        "org.sonatype.mavenbook.multi", "simple-weather", "0.9-MNG6656-SNAPSHOT", "pom", "build")));

        assertTextEquals(
                testDir.resolve("expected/simple-webapp.pom"),
                Path.of(verifier.getArtifactPath(
                        "org.sonatype.mavenbook.multi", "simple-webapp", "0.9-MNG6656-SNAPSHOT", "pom")));

        assertTextEquals(
                testDir.resolve("expected/simple-webapp-build.pom"),
                Path.of(verifier.getArtifactPath(
                        "org.sonatype.mavenbook.multi", "simple-webapp", "0.9-MNG6656-SNAPSHOT", "pom", "build")));
    }

    static void assertTextEquals(Path file1, Path file2) throws IOException {
        assertEquals(
                String.join(
                        "\n",
                        Files.readAllLines(file1).stream()
                                .map(String::trim)
                                .toList()),
                String.join(
                        "\n",
                        Files.readAllLines(file2).stream()
                                .map(String::trim)
                                .toList()),
                "pom files differ " + file1 + " " + file2);
    }
}
