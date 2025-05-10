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
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-8527">MNG-8527</a>.
 */
class MavenITmng8527ConsumerPomTest extends AbstractMavenIntegrationTestCase {

    MavenITmng8527ConsumerPomTest() {
        super("[4.0.0-rc-3-SNAPSHOT,)");
    }

    /**
     *  Verify project is buildable.
     */
    @Test
    void testIt() throws Exception {
        Path basedir =
                extractResources("/mng-8527-consumer-pom").getAbsoluteFile().toPath();

        Verifier verifier = newVerifier(basedir.toString());
        verifier.addCliArgument("install");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        Path consumerPomPath =
                Path.of(verifier.getArtifactPath("org.apache.maven.its.mng-8527", "child", "1.0.0-SNAPSHOT", "pom"));
        Path buildPomPath = Path.of(
                verifier.getArtifactPath("org.apache.maven.its.mng-8527", "child", "1.0.0-SNAPSHOT", "pom", "build"));

        assertTrue(Files.exists(consumerPomPath), "consumer pom not found at " + consumerPomPath);
        assertTrue(Files.exists(buildPomPath), "consumer pom not found at " + consumerPomPath);

        List<String> consumerPomLines;
        try (Stream<String> lines = Files.lines(consumerPomPath)) {
            consumerPomLines = lines.toList();
        }
        assertTrue(
                consumerPomLines.stream().noneMatch(s -> s.contains("<parent>")),
                "Consumer pom should not have any <parent> element");
        assertTrue(
                consumerPomLines.stream().anyMatch(s -> s.contains("<organization>")),
                "Consumer pom should have an <organization> element");
        assertEquals(
                2,
                consumerPomLines.stream()
                        .filter(s -> s.contains("<dependency>"))
                        .count(),
                "Consumer pom should have two dependencies");

        List<String> buildPomLines;
        try (Stream<String> lines = Files.lines(buildPomPath)) {
            buildPomLines = lines.toList();
        }
        assertTrue(
                buildPomLines.stream().anyMatch(s -> s.contains("<parent>")),
                "Build pom should have a <parent> element");
        assertTrue(
                buildPomLines.stream().noneMatch(s -> s.contains("<organization>")),
                "Build pom should not have an <organization> element");
        assertEquals(
                2,
                buildPomLines.stream().filter(s -> s.contains("<dependency>")).count(),
                "Build pom should have two dependencies");
    }
}
