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
 * This is a test set for <a href="https://github.com/apache/maven/issues/11378">GH-11378</a>.
 */
public class MavenITgh11378SealedParameterConfigTest extends AbstractMavenIntegrationTestCase {

    @Test
    public void testSealedParameterImplementationCanUseSimpleName() throws Exception {
        Path testDir = extractResources("/gh-11378-sealed-parameter-config");

        Verifier parentVerifier = newVerifier(testDir, false);
        parentVerifier.addCliArgument("-N");
        parentVerifier.addCliArgument("install");
        parentVerifier.execute();
        parentVerifier.verifyErrorFreeLog();

        Verifier pluginVerifier = newVerifier(testDir.resolve("plugin"), false);
        pluginVerifier.getSystemProperties().put("maven.compiler.source", "17");
        pluginVerifier.getSystemProperties().put("maven.compiler.target", "17");
        pluginVerifier.getSystemProperties().put("maven.compiler.release", "17");
        pluginVerifier.addCliArgument("install");
        pluginVerifier.execute();
        pluginVerifier.verifyErrorFreeLog();

        Verifier verifier = newVerifier(testDir.resolve("consumer"), false);
        verifier.addCliArgument("test:test-goal");
        verifier.execute();
        verifier.verifyErrorFreeLog();
        verifier.verifyTextInLog("Configured sealed artifact: local");
    }
}
