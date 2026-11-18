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

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration test for modular plugin JPMS support.
 * <p>
 * Verifies that a plugin declaring {@code <modular>true</modular>} in its
 * descriptor is loaded in its own JPMS {@link ModuleLayer} and can
 * {@code requires org.apache.maven.api} from the api layer.
 */
class MavenITghModularPluginTest extends AbstractMavenIntegrationTestCase {

    @Test
    void testModularPluginLoadsInJpmsLayer() throws Exception {
        Path testDir = extractResources("/gh-modular-plugin");

        // Step 1: build and install the modular plugin
        Verifier pluginVerifier = newVerifier(testDir.resolve("plugin"));
        pluginVerifier.getSystemProperties().put("maven.compiler.release", "17");
        pluginVerifier.getSystemProperties().remove("maven.compiler.source");
        pluginVerifier.getSystemProperties().remove("maven.compiler.target");
        pluginVerifier.addCliArgument("install");
        pluginVerifier.execute();
        pluginVerifier.verifyErrorFreeLog();

        // Verify the annotation processor generated <modular>true</modular>
        pluginVerifier.verifyFilePresent("target/classes/META-INF/maven/plugin.xml");
        String pluginXml = Files.readString(testDir.resolve("plugin/target/classes/META-INF/maven/plugin.xml"));
        assertTrue(pluginXml.contains("<modular>true</modular>"),
                "plugin.xml should contain <modular>true</modular>");

        // Step 2: run the consumer project — exercises the modular plugin
        Verifier consumerVerifier = newVerifier(testDir.resolve("consumer"));
        consumerVerifier.addCliArgument("compile");
        consumerVerifier.execute();
        consumerVerifier.verifyErrorFreeLog();

        // Verify plugin executed in a named module with a ModuleLayer
        consumerVerifier.verifyTextInLog("[modular-plugin] Executed in module: org.apache.maven.its.modular");
        consumerVerifier.verifyTextInLog("[modular-plugin] Module layer present: true");

        // Verify the touch file was created with module info
        Path touchFile = testDir.resolve("consumer/target/touch.txt");
        assertTrue(Files.exists(touchFile), "touch.txt should exist");
        String content = Files.readString(touchFile);
        assertTrue(content.contains("module=org.apache.maven.its.modular"),
                "touch.txt should contain module name, got: " + content);
    }
}
