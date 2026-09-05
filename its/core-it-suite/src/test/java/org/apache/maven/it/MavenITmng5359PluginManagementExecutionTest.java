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
 * Verifies that executions declared only in {@code pluginManagement} are not activated by default lifecycle bindings.
 *
 * @see <a href="https://github.com/apache/maven/issues/6918">MNG-5359</a>
 * @since 4.1.0
 */
class MavenITmng5359PluginManagementExecutionTest extends AbstractMavenIntegrationTestCase {

    @Test
    void testManagedExecutionRequiresPluginDeclaration() throws Exception {
        Path testDir = extractResources("mng-5359");

        Verifier verifier = newVerifier(testDir);
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.addCliArgument("package");
        verifier.execute();
        verifier.verifyErrorFreeLog();
        verifier.verifyFileNotPresent("target/managed-clean.txt");

        verifier = newVerifier(testDir);
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.addCliArgument("-Pactivate-clean-plugin");
        verifier.addCliArgument("package");
        verifier.execute();
        verifier.verifyErrorFreeLog();
        verifier.verifyFilePresent("target/managed-clean.txt");
    }
}
