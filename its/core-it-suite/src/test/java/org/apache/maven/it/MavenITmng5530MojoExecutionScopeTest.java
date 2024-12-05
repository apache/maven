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

import org.junit.jupiter.api.Test;

public class MavenITmng5530MojoExecutionScopeTest extends AbstractMavenIntegrationTestCase {
    public MavenITmng5530MojoExecutionScopeTest() {
        super("[3.2.1,)");
    }

    @Test
    public void test_copyfiles() throws Exception {
        File testDir = extractResources("/mng-5530-mojo-execution-scope");
        File pluginDir = new File(testDir, "plugin");
        File projectDir = new File(testDir, "basic");

        Verifier verifier;

        // install the test plugin
        verifier = newVerifier(pluginDir.getAbsolutePath());
        verifier.addCliArgument("install");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        // build the test project
        verifier = newVerifier(projectDir.getAbsolutePath());
        verifier.addCliArgument("package");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        // verifier.verifyFilePresent( "target/execution-failure.txt" );
        verifier.verifyFilePresent("target/execution-success.txt");
        verifier.verifyFilePresent("target/execution-before.txt");
    }

    @Test
    public void test_copyfiles_multithreaded() throws Exception {
        File testDir = extractResources("/mng-5530-mojo-execution-scope");
        File pluginDir = new File(testDir, "plugin");
        File projectDir = new File(testDir, "basic");

        Verifier verifier;

        // install the test plugin
        verifier = newVerifier(pluginDir.getAbsolutePath());
        verifier.addCliArgument("install");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        // build the test project
        verifier = newVerifier(projectDir.getAbsolutePath());
        verifier.addCliArgument("--builder");
        verifier.addCliArgument("multithreaded");
        verifier.addCliArgument("-T");
        verifier.addCliArgument("1");
        verifier.addCliArgument("package");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        // verifier.verifyFilePresent( "target/execution-failure.txt" );
        verifier.verifyFilePresent("target/execution-success.txt");
        verifier.verifyFilePresent("target/execution-before.txt");
    }

    @Test
    public void testExtension() throws Exception {
        File testDir = extractResources("/mng-5530-mojo-execution-scope");
        File extensionDir = new File(testDir, "extension");
        File pluginDir = new File(testDir, "extension-plugin");
        File projectDir = new File(testDir, "extension-project");

        Verifier verifier;

        // install the test extension
        verifier = newVerifier(extensionDir.getAbsolutePath());
        verifier.addCliArgument("install");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        // install the test plugin
        verifier = newVerifier(pluginDir.getAbsolutePath());
        verifier.addCliArgument("install");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        // build the test project
        verifier = newVerifier(projectDir.getAbsolutePath());
        verifier.addCliArgument("-Dmaven.ext.class.path=" + new File(extensionDir, "target/classes").getAbsolutePath());
        verifier.setForkJvm(true); // verifier does not support custom realms in embedded mode
        verifier.addCliArgument("package");
        verifier.execute();
        verifier.verifyErrorFreeLog();
    }
}
