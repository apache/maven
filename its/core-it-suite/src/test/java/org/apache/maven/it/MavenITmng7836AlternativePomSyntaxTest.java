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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MavenITmng7836AlternativePomSyntaxTest extends AbstractMavenIntegrationTestCase {

    protected MavenITmng7836AlternativePomSyntaxTest() {
        // New feature in alpha-8-SNAPSHOT
        super("(4.0.0-alpha-7,)");
    }

    @Test
    void testAlternativeSyntax() throws Exception {
        File testDir = extractResources("/mng-7836-alternative-pom-syntax");

        final Verifier pluginVerifier = newVerifier(new File(testDir, "maven-hocon-extension").getPath());
        pluginVerifier.addCliArgument("clean");
        pluginVerifier.addCliArgument("install");
        pluginVerifier.addCliArgument("-V");
        pluginVerifier.execute();
        pluginVerifier.verifyErrorFreeLog();

        final Verifier consumerVerifier = newVerifier(new File(testDir, "simple").getPath());
        consumerVerifier.addCliArgument("clean");
        consumerVerifier.addCliArgument("install");
        consumerVerifier.addCliArgument("-Drat.skip=true");
        consumerVerifier.addCliArgument("-V");

        Path consumerPom = Path.of(consumerVerifier.getArtifactPath(
                "org.apache.maven.its.mng-7836", "hocon-simple", "1.0.0-SNAPSHOT", "pom", ""));
        Path buildPom = Path.of(consumerVerifier.getArtifactPath(
                "org.apache.maven.its.mng-7836", "hocon-simple", "1.0.0-SNAPSHOT", "pom", "build"));
        consumerVerifier.deleteArtifacts("org.apache.maven.its.mng-7836", "hocon-simple", "1.0.0-SNAPSHOT");

        consumerVerifier.execute();
        consumerVerifier.verifyErrorFreeLog();

        assertTrue(Files.isRegularFile(consumerPom));
        List<String> consumerPomLines = Files.readAllLines(consumerPom, StandardCharsets.UTF_8);
        assertTrue(consumerPomLines.stream().anyMatch(l -> l.contains("<artifactId>hocon-simple</artifactId>")));

        // The build pom is the original POM, so the hocon file
        assertTrue(Files.isRegularFile(buildPom));
        List<String> buildPomLines = Files.readAllLines(buildPom, StandardCharsets.UTF_8);
        assertTrue(buildPomLines.stream().anyMatch(l -> l.contains("artifactId = hocon-simple")));
    }
}
