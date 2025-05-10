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

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-8477">MNG-8477</a>.
 */
class MavenITmng8523ModelPropertiesTest extends AbstractMavenIntegrationTestCase {

    MavenITmng8523ModelPropertiesTest() {
        super("[4.0.0-rc-3-SNAPSHOT,)");
    }

    /**
     *  Verify project is buildable.
     */
    @Test
    void testIt() throws Exception {
        Path basedir =
                extractResources("/mng-8523-model-properties").getAbsoluteFile().toPath();

        Verifier verifier = newVerifier(basedir.toString());
        verifier.addCliArguments("install", "-DmavenVersion=4.0.0-rc-2");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        Path consumerPomPath =
                Path.of(verifier.getArtifactPath("org.apache.maven.its.mng-8523", "jar", "1.0.0-SNAPSHOT", "pom"));
        assertTrue(Files.exists(consumerPomPath), "consumer pom not found at " + consumerPomPath);

        List<String> consumerPomLines;
        try (Stream<String> lines = Files.lines(consumerPomPath)) {
            consumerPomLines = lines.toList();
        }
        assertTrue(
                consumerPomLines.stream().noneMatch(s -> s.contains("${mavenVersion}")),
                "Consumer pom should not have any <parent> element");
        assertTrue(
                consumerPomLines.stream().anyMatch(s -> s.contains("4.0.0-rc-2")),
                "Consumer pom should not have any <parent> element");
    }
}
