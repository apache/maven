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
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MavenITmng6127PluginExecutionConfigurationInterferenceTest extends AbstractMavenIntegrationTestCase {

    @Test
    public void testCustomMojoExecutionConfigurator() throws Exception {
        Path testDir = extractResources("/mng-6127-plugin-execution-configuration-interference");
        Path pluginDir = testDir.resolve("plugin");
        Path projectDir = testDir.resolve("project");
        Path modAprojectDir = projectDir.resolve("mod-a");
        Path modBprojectDir = projectDir.resolve("mod-b");
        Path modCprojectDir = projectDir.resolve("mod-c");

        Verifier verifier;

        // install the test plugin
        verifier = newVerifier(pluginDir);
        verifier.addCliArgument("install");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        Path modAconfigurationFile = modAprojectDir.resolve("configuration.txt");
        Path modBconfigurationFile = modBprojectDir.resolve("configuration.txt");
        Path modCconfigurationFile = modCprojectDir.resolve("configuration.txt");
        Files.delete(modAconfigurationFile);
        Files.delete(modBconfigurationFile);
        Files.delete(modCconfigurationFile);

        // build the test project
        verifier = newVerifier(projectDir);
        verifier.addCliArgument("verify");
        verifier.addCliArgument("-X");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        verifier.verifyFilePresent(modAconfigurationFile.toString());
        String modAactual = Files.readString(modAconfigurationFile);
        assertEquals("name=mod-a, secondName=second from components.xml", modAactual);

        verifier.verifyFilePresent(modBconfigurationFile.toString());
        String modBactual = Files.readString(modBconfigurationFile);
        assertEquals("name=mod-b, secondName=second from components.xml", modBactual);

        verifier.verifyFilePresent(modCconfigurationFile.toString());
        String modCactual = Files.readString(modCconfigurationFile);
        assertEquals("secondName=second from components.xml", modCactual);
    }
}
