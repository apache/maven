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

public class MavenITmng6127PluginExecutionConfigurationInterferenceTest extends AbstractMavenIntegrationTestCase {

    public MavenITmng6127PluginExecutionConfigurationInterferenceTest() {
        super("[3.5.1,)");
    }

    @Test
    public void testCustomMojoExecutionConfigurator() throws Exception {
        File testDir = extractResources("/mng-6127-plugin-execution-configuration-interference");
        File pluginDir = new File(testDir, "plugin");
        File projectDir = new File(testDir, "project");
        File modAprojectDir = new File(projectDir, "mod-a");
        File modBprojectDir = new File(projectDir, "mod-b");
        File modCprojectDir = new File(projectDir, "mod-c");

        Verifier verifier;

        // install the test plugin
        verifier = newVerifier(pluginDir.getAbsolutePath());
        verifier.addCliArgument("install");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        File modAconfigurationFile = new File(modAprojectDir, "configuration.txt");
        File modBconfigurationFile = new File(modBprojectDir, "configuration.txt");
        File modCconfigurationFile = new File(modCprojectDir, "configuration.txt");
        modAconfigurationFile.delete();
        modBconfigurationFile.delete();
        modCconfigurationFile.delete();

        // build the test project
        verifier = newVerifier(projectDir.getAbsolutePath());
        verifier.addCliArgument("verify");
        verifier.addCliArgument("-X");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        verifier.verifyFilePresent(modAconfigurationFile.getCanonicalPath());
        String modAactual = Files.readString(modAconfigurationFile.toPath());
        assertEquals("name=mod-a, secondName=second from components.xml", modAactual);

        verifier.verifyFilePresent(modBconfigurationFile.getCanonicalPath());
        String modBactual = Files.readString(modBconfigurationFile.toPath());
        assertEquals("name=mod-b, secondName=second from components.xml", modBactual);

        verifier.verifyFilePresent(modCconfigurationFile.getCanonicalPath());
        String modCactual = Files.readString(modCconfigurationFile.toPath());
        assertEquals("secondName=second from components.xml", modCactual);
    }
}
