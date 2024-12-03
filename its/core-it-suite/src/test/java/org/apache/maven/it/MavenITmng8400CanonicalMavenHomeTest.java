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

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import org.apache.maven.shared.verifier.util.ResourceExtractor;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-8400">MNG-8400</a>.
 */
class MavenITmng8400CanonicalMavenHomeTest extends AbstractMavenIntegrationTestCase {

    MavenITmng8400CanonicalMavenHomeTest() {
        super("[4.0.0-rc-1,)");
    }

    /**
     *  Verify that properties are aligned (all use canonical maven home)
     */
    @Test
    void testIt() throws Exception {
        Path basedir = ResourceExtractor.simpleExtractResources(getClass(), "/mng-8400")
                .getAbsoluteFile()
                .toPath();
        Path tempDir = basedir.resolve("tmp");
        Files.createDirectories(tempDir);

        Path linkedMavenHome = tempDir.resolve("linked-maven-home");

        Path mavenHome = Paths.get(System.getProperty("maven.home"));
        Files.createSymbolicLink(linkedMavenHome, mavenHome);

        System.setProperty("maven.home", linkedMavenHome.toString());
        Verifier verifier = newVerifier(basedir.toString());
        verifier.addCliArgument("--raw-streams");
        verifier.addCliArgument("--quiet");
        verifier.addCliArgument("-DforceStdout");
        verifier.addCliArgument("-DasProperties");
        verifier.addCliArgument("eu.maveniverse.maven.plugins:toolbox:0.5.2:gav-dump");
        // TODO: fork until new entry point CLIng is used
        verifier.setForkJvm(true);
        verifier.execute();

        String dump = verifier.loadLogContent();
        Properties props = new Properties();
        props.load(new ByteArrayInputStream(dump.getBytes(StandardCharsets.UTF_8)));

        Path installationSettingsXml = Paths.get(props.getProperty("maven.settings"));
        Path installationToolchainsXml = Paths.get(props.getProperty("maven.toolchains"));

        assertEquals(installationToolchainsXml.getParent(), installationSettingsXml.getParent());
    }
}
