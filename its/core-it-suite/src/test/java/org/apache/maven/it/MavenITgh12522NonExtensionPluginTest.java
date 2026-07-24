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

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * This is a test for <a href="https://github.com/apache/maven/issues/12522">GH-12522</a>.
 *
 * Verifies that plugins that are NOT configured with {@code <extensions>true</extensions>}
 * do not cause build failures when they ship JSR330 components whose internal dependencies
 * cannot be resolved in Maven's container. Prior to the fix, Sisu's live-injected
 * {@code List<ProjectExecutionListener>} would pick up such components and attempt to
 * provision them, causing {@code Unable to provision} errors that aborted the build.
 *
 * The reproducer uses {@code tycho-bnd-plugin} which ships a {@code ProjectExecutionListener}
 * ({@code BndProjectExecutionListener}) as a {@code @Named} JSR330 component but is not
 * configured as an extension in the POM.
 */
class MavenITgh12522NonExtensionPluginTest extends AbstractMavenIntegrationTestCase {

    /**
     * Verify that a build using a plugin with JSR330 components (tycho-bnd-plugin)
     * that is NOT marked as an extension completes successfully. The plugin's
     * {@code ProjectExecutionListener} should be silently skipped rather than
     * causing the build to fail with a provisioning error.
     */
    @Test
    void testNonExtensionPluginComponentsNotPickedUp() throws Exception {
        Path testDir = extractResources("gh-12522-non-extension-plugin");

        Verifier verifier = newVerifier(testDir.toString(), "remote");
        verifier.addCliArgument("process-classes");
        verifier.execute();
        verifier.verifyErrorFreeLog();
    }
}
