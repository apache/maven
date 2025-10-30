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
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Benjamin Bentmann
 *
 */
public class MavenIT0144LifecycleExecutionOrderTest extends AbstractMavenIntegrationTestCase {

    /**
     * Test that the lifecycle phases execute in proper order.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testit0144() throws Exception {
        Path testDir = extractResources("/it0144");

        Verifier verifier = newVerifier(testDir.toString());
        verifier.deleteDirectory("target");
        verifier.setAutoclean(false);
        verifier.addCliArguments("post-clean", "deploy", "site-deploy");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        List<String> expected = new ArrayList<>();

        expected.add("pre-clean");
        expected.add("clean");
        expected.add("post-clean");

        expected.add("validate");
        expected.add("initialize");
        expected.add("generate-sources");
        expected.add("process-sources");
        expected.add("generate-resources");
        expected.add("process-resources");
        expected.add("compile");
        expected.add("process-classes");
        expected.add("generate-test-sources");
        expected.add("process-test-sources");
        expected.add("generate-test-resources");
        expected.add("process-test-resources");
        expected.add("test-compile");
        // Inline version check: (2.0.4,) - current Maven version matches
        // MNG-1508
        expected.add("process-test-classes");
        expected.add("test");
        // Inline version check: (2.1.0-M1,) - current Maven version matches
        // MNG-2097
        expected.add("prepare-package");
        expected.add("package");
        // Inline version check: (2.0.1,) - current Maven version matches
        expected.add("pre-integration-test");
        expected.add("integration-test");
        // Inline version check: (2.0.1,) - current Maven version matches
        expected.add("post-integration-test");
        expected.add("verify");
        expected.add("install");
        expected.add("deploy");

        expected.add("pre-site");
        expected.add("site");
        expected.add("post-site");
        expected.add("site-deploy");

        List<String> phases = verifier.loadLines("target/phases.log");
        assertEquals(expected, phases);
    }
}
