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
import java.time.Duration;
import java.util.List;

import org.apache.maven.executor.ExecutorRequest;
import org.apache.maven.executor.forked.ForkedMavenExecutor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-8655">MNG-8655</a>.
 */
class MavenITmng8655SettingsParserTest extends AbstractMavenIntegrationTestCase {
    @TempDir
    Path directory;

    @Test
    void extensionClassPathParserAndXmlFallback() throws Exception {
        Path testDir = prepare();
        Path project = testDir.resolve("project");
        Path extension = testDir.resolve("extension/target/settings-parser-0.1.jar");

        Verifier verifier = newVerifier(project);
        verifier.setForkJvm(true);
        verifier.addCliArguments("-Dmaven.ext.class.path=" + extension,
                "-s", "settings.properties", "-Dparser.input=custom-settings", "validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();
        verifier.verifyTextInLog("Building custom-settings 0.1");

        verifier = newVerifier(project);
        verifier.setForkJvm(true);
        verifier.setLogFileName("xml-fallback.txt");
        verifier.addCliArguments("-Dmaven.ext.class.path=" + extension, "-s", "settings.conf", "validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();
        verifier.verifyTextInLog("Building xml-settings 0.1");
    }

    @Test
    void declaredParserCannotReadItsOwnBootstrapSettings() throws Exception {
        Path testDir = prepare();
        Path project = testDir.resolve("project");
        Files.copy(testDir.resolve("extensions.xml"), project.resolve(".mvn/extensions.xml"));
        Verifier verifier = newVerifier(project);
        verifier.setForkJvm(true);
        verifier.addCliArguments("-Dmaven.ext.class.path=" + testDir.resolve("extension/target/settings-parser-0.1.jar"),
                "-s", "settings.properties", "validate");
        assertThrows(VerificationException.class, verifier::execute);
        verifier.verifyTextInLog("Non-parseable settings");
        verifier.verifyTextInLog("settings.properties");
    }

    @Test
    void coreRealmParserReadsBootstrapSettings() throws Exception {
        Path testDir = prepare();
        Path project = testDir.resolve("project");
        Files.copy(testDir.resolve("extensions.xml"), project.resolve(".mvn/extensions.xml"));
        Path installation = directory.resolve("maven");
        ItUtils.copyDirectoryStructure(Path.of(System.getProperty("maven.home")), installation);
        Path libExt = Files.createDirectories(installation.resolve("lib/ext"));
        Files.copy(testDir.resolve("extension/target/settings-parser-0.1.jar"), libExt.resolve("settings-parser.jar"));

        Path localRepository = newVerifier(testDir.resolve("extension")).getLocalRepository();
        var result = new ForkedMavenExecutor(installation).execute(ExecutorRequest.mavenBuilder()
                .cwd(project)
                .userHomeDirectory(Files.createDirectories(directory.resolve("home")))
                .arguments(List.of("-B", "-ntp", "-s", "settings.properties", "-Dparser.input=bootstrap-settings",
                        "-Dmaven.repo.local=" + localRepository, "validate"))
                .skipMavenRc(true)
                .grabOutputAsString(true)
                .executionTimeout(Duration.ofMinutes(2))
                .build());
        String output = result.stdOutString().orElse("") + result.stdErrString().orElse("");
        Files.writeString(testDir.resolve("bootstrap.txt"), output);
        assertTrue(result.success(), output);
        assertTrue(output.contains("Building bootstrap-settings 0.1"), output);
    }

    private Path prepare() throws Exception {
        Path testDir = directory.resolve("test");
        ItUtils.copyDirectoryStructure(extractResources("mng-8655-settings-parser"), testDir);
        Files.createDirectories(testDir.resolve("project/.mvn"));
        Verifier verifier = newVerifier(testDir.resolve("extension"));
        verifier.setLogFileName("extension-install.txt");
        verifier.addCliArguments("-DmavenVersion=" + verifier.getMavenVersion(), "install");
        verifier.execute();
        verifier.verifyErrorFreeLog();
        return testDir;
    }
}
