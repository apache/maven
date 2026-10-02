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
import java.nio.file.StandardCopyOption;

import org.junit.jupiter.api.Test;

/**
 * Verifies that cross-lifecycle executions in {@code pluginManagement} require an explicit plugin declaration.
 *
 * @see <a href="https://github.com/apache/maven/issues/6918">MNG-5359</a>
 * @since 4.1.0
 */
class MavenITmng5359PluginManagementExecutionTest extends AbstractMavenIntegrationTestCase {

    @Test
    void testManagedExecutionNotActivatedWithoutDeclaration() throws Exception {
        Path testDir = prepareProject("management-only");
        // The clean plugin is introduced by the clean lifecycle, but the managed execution targets package.
        Verifier verifier = newVerifier(testDir);
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.addCliArgument("package");
        verifier.execute();
        verifier.verifyErrorFreeLog();
        verifier.verifyFileNotPresent("target/managed-clean.txt");
    }

    @Test
    void testManagedExecutionActivatedWithExplicitDeclaration() throws Exception {
        Path testDir = prepareProject("explicit-plugin");
        Verifier verifier = newVerifier(testDir);
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.addCliArgument("-Pactivate-clean-plugin");
        verifier.addCliArgument("package");
        verifier.execute();
        verifier.verifyErrorFreeLog();
        verifier.verifyFilePresent("target/managed-clean.txt");
    }

    private Path prepareProject(String scenario) throws Exception {
        Path testDir = extractResources("mng-5359");
        Path project = testDir.resolve(scenario);
        Files.createDirectories(project);
        Files.copy(testDir.resolve("pom.xml"), project.resolve("pom.xml"), StandardCopyOption.REPLACE_EXISTING);
        return project;
    }
}
