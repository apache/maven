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

public class MavenITmng5753CustomMojoExecutionConfiguratorTest extends AbstractMavenIntegrationTestCase {

    public MavenITmng5753CustomMojoExecutionConfiguratorTest() {
        super("[3.3.0-alpha,)");
    }

    @Test
    public void testCustomMojoExecutionConfigurator() throws Exception {
        File testDir = extractResources("/mng-5753-custom-mojo-execution-configurator");
        File pluginDir = new File(testDir, "plugin");
        File projectDir = new File(testDir, "project");

        Verifier verifier;

        // install the test plugin
        verifier = newVerifier(pluginDir.getAbsolutePath());
        verifier.addCliArgument("install");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        File configurationFile = new File(projectDir, "configuration.txt");
        configurationFile.delete();

        // build the test project
        verifier = newVerifier(projectDir.getAbsolutePath());
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        verifier.verifyFilePresent(configurationFile.getCanonicalPath());
        //
        // The <name/> element in the original configuration is "ORIGINAL". We want to assert that our
        // custom MojoExecutionConfigurator made the transformation of the element from "ORIGINAL" to "TRANSFORMED"
        //
        String actual = Files.readString(configurationFile.toPath());
        assertEquals("TRANSFORMED", actual);
    }
}
