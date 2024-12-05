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
import java.nio.file.Files;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MavenITmng5742BuildExtensionClassloaderTest extends AbstractMavenIntegrationTestCase {

    public MavenITmng5742BuildExtensionClassloaderTest() {
        super("(3.2.5,)");
    }

    @Test
    public void testBuildExtensionClassloader() throws Exception {
        File testDir = extractResources("/mng-5742-build-extension-classloader");
        File pluginDir = new File(testDir, "plugin");
        File projectDir = new File(testDir, "project");

        Verifier verifier;

        // install the test plugin
        verifier = newVerifier(pluginDir.getAbsolutePath());
        verifier.addCliArgument("install");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        // build the test project
        verifier = newVerifier(projectDir.getAbsolutePath());
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();
        verifier.verifyFilePresent("target/execution-success.txt");

        String actual = Files.readString(new File(projectDir, "target/execution-success.txt").toPath());
        assertEquals("executed", actual);
    }
}
