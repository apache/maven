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
import java.nio.file.Path;
import java.nio.file.Files;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MavenITmng5742BuildExtensionClassloaderTest extends AbstractMavenIntegrationTestCase {

    @Test
    public void testBuildExtensionClassloader() throws Exception {
        Path testDir = extractResourcesAsPath("/mng-5742-build-extension-classloader");
        Path pluginDir = testDir.resolve("plugin");
        Path projectDir = testDir.resolve("project");

        Verifier verifier;

        // install the test plugin
        verifier = newVerifier(pluginDir.toString());
        verifier.addCliArgument("install");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        // build the test project
        verifier = newVerifier(projectDir.toString());
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();
        verifier.verifyFilePresent("target/execution-success.txt");

        String actual = Files.readString(projectDir.resolve("target/execution-success.txt").toPath());
        assertEquals("executed", actual);
    }
}
