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

/**
 * This is a test set for <a href="https://github.com/apache/maven/issues/11314">GH-11314</a>.
 *
 * Verifies that V3 Mojos can be injected with v3 API beans that are bridged from v4 API
 * implementations. Specifically tests the case where a plugin needs to inject ToolchainFactory
 * with a named qualifier.
 *
 * This IT manually manages {@code .mvn} directories, so instructs Verifier to NOT create any.
 *
 * @see <a href="https://github.com/apache/maven-toolchains-plugin/issues/128">maven-toolchains-plugin#128</a>
 */
public class MavenITgh11314PluginInjectionTest extends AbstractMavenIntegrationTestCase {

    /**
     * Verify that V3 Mojos can be injected with v3 ToolchainFactory which is bridged from
     * the v4 ToolchainFactory implementation. This test reproduces the issue where a plugin
     * with a field requiring injection of ToolchainFactory with @Named("jdk") fails with
     * NullInjectedIntoNonNullable error.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testV3MojoWithMavenContainerInjection() throws Exception {
        File testDir = extractResources("/gh-11314-v3-mojo-injection");

        // Before, build and install the parent POM
        Verifier parentVerifier = newVerifier(testDir.getAbsolutePath(), false);
        parentVerifier.addCliArgument("-N");
        parentVerifier.addCliArgument("install");
        parentVerifier.execute();
        parentVerifier.verifyErrorFreeLog();

        // First, build and install the test plugin
        File pluginDir = new File(testDir, "plugin");
        Verifier pluginVerifier = newVerifier(pluginDir.getAbsolutePath(), false);
        pluginVerifier.addCliArgument("install");
        pluginVerifier.execute();
        pluginVerifier.verifyErrorFreeLog();

        // Now run the test project that uses the plugin
        File consumerDir = new File(testDir, "consumer");
        Verifier verifier = newVerifier(consumerDir.getAbsolutePath(), false);
        verifier.addCliArguments("test:test-goal");
        verifier.execute();
        verifier.verifyErrorFreeLog();
    }
}
