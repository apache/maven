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
 * This is a test for
 * <a href="https://github.com/apache/maven/issues/12534">MNG-12534</a>.
 * <p>
 * Verifies that {@code afterLinks} in a V2 plugin descriptor
 * ({@code http://maven.apache.org/PLUGIN/2.0.0}) are correctly loaded
 * and processed by the build plan executor without errors.
 * <p>
 * The test plugin carries a handcrafted V2 plugin.xml with a
 * {@code <afterLink>} of type PROJECT. The build plan executor
 * must parse the descriptor, create the ordering edges, and
 * execute the mojo successfully.
 */
class MavenITmng12534AfterAnnotationTest extends AbstractMavenIntegrationTestCase {

    /**
     * Verify that a plugin with {@code afterLinks} in its V2 descriptor
     * is correctly loaded and the mojo executes under the concurrent builder.
     */
    @Test
    void testAfterLinksLoadedAndMojoExecutes() throws Exception {
        Path testDir = extractResources("/mng-12534-after-annotation");

        // Step 1: install the test plugin with a handcrafted V2 plugin descriptor
        Verifier pluginVerifier = newVerifier(testDir.resolve("plugin"));
        pluginVerifier.addCliArgument("install");
        pluginVerifier.execute();
        pluginVerifier.verifyErrorFreeLog();

        // Step 2: build the consumer project using the concurrent builder
        Verifier consumerVerifier = newVerifier(testDir.resolve("consumer"));
        consumerVerifier.addCliArgument("-b");
        consumerVerifier.addCliArgument("concurrent");
        consumerVerifier.addCliArgument("compile");
        consumerVerifier.execute();
        consumerVerifier.verifyErrorFreeLog();

        // Verify the mojo actually executed
        consumerVerifier.verifyTextInLog("[MNG-12534] touch goal executed - afterLinks wired correctly");
        consumerVerifier.verifyFilePresent("target/touch.txt");
    }
}
