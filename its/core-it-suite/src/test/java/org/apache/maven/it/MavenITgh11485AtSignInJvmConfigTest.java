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
import java.util.Properties;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * This is a test set for <a href="https://github.com/apache/maven/issues/11485">GH-11485</a>:
 * Verify that @ character in .mvn/jvm.config values is handled correctly.
 * This is important for Jenkins workspaces like workspace/project_PR-350@2
 */
public class MavenITgh11485AtSignInJvmConfigTest extends AbstractMavenIntegrationTestCase {

    @Test
    public void testAtSignInJvmConfig() throws Exception {
        File testDir = extractResources("/gh-11485-at-sign");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.addCliArgument(
                "-Dexpression.outputFile=" + new File(testDir, "target/pom.properties").getAbsolutePath());
        verifier.setForkJvm(true); // custom .mvn/jvm.config
        // Enable debug logging for launcher script to diagnose jvm.config parsing issues
        verifier.setEnvironmentVariable("MAVEN_DEBUG_SCRIPT", "1");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        Properties props = verifier.loadProperties("target/pom.properties");
        String expectedPath = testDir.getAbsolutePath().replace('\\', '/');
        assertEquals(
                expectedPath + "/workspace@2/test",
                props.getProperty("project.properties.pathWithAtProp").replace('\\', '/'),
                "Path with @ character should be preserved");
        assertEquals(
                "value@test",
                props.getProperty("project.properties.propWithAtProp"),
                "Property value with @ character should be preserved");
    }

    @Test
    public void testAtSignInCommandLineProperty() throws Exception {
        File testDir = extractResources("/gh-11485-at-sign");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.addCliArgument(
                "-Dexpression.outputFile=" + new File(testDir, "target/pom.properties").getAbsolutePath());
        verifier.setForkJvm(true); // custom .mvn/jvm.config
        // Pass a path with @ character via command line (simulating Jenkins workspace)
        String jenkinsPath = testDir.getAbsolutePath().replace('\\', '/') + "/jenkins.workspace/proj@2";
        verifier.addCliArgument("-Dcmdline.path=" + jenkinsPath);
        verifier.addCliArgument("-Dcmdline.value=test@value");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        Properties props = verifier.loadProperties("target/pom.properties");
        assertEquals(
                jenkinsPath,
                props.getProperty("project.properties.cmdlinePath").replace('\\', '/'),
                "Command-line path with @ character should be preserved");
        assertEquals(
                "test@value",
                props.getProperty("project.properties.cmdlineValue"),
                "Command-line value with @ character should be preserved");
    }
}

