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
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-6558">MNG-6558</a>.
 */
public class MavenITmng6558ToolchainsBuildingEventTest extends AbstractMavenIntegrationTestCase {

    public MavenITmng6558ToolchainsBuildingEventTest() {
        super("[3.6.1,)");
    }

    /**
     * Verify that <code>ToolchainsBuildingRequest</code> and <code>ToolchainsBuildingResult</code> events are sent to event spy.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testit() throws Exception {
        File testDir = extractResources("/mng-6558");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setForkJvm(true); // maven.ext.class.path used
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.addCliArgument("-Dmaven.ext.class.path=spy-0.1.jar");
        verifier.addCliArgument("-X");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        List<String> lines = verifier.loadLines("target/spy.log");
        assertTrue(lines.get(0).startsWith("init"), lines.toString());
        assertTrue(lines.get(lines.size() - 1).startsWith("close"), lines.toString());
        assertTrue(
                lines.contains(
                        matchesVersionRange("[4.0.0-beta-5,)")
                                ? "event: org.apache.maven.api.services.ToolchainsBuilderRequest$ToolchainsBuilderRequestBuilder$DefaultToolchainsBuilderRequest"
                                : "event: org.apache.maven.toolchain.building.DefaultToolchainsBuildingRequest"),
                lines.toString());
        assertTrue(
                lines.contains(
                        matchesVersionRange("[4.0.0-beta-5,)")
                                ? "event: org.apache.maven.internal.impl.DefaultToolchainsBuilder$DefaultToolchainsBuilderResult"
                                : "event: org.apache.maven.toolchain.building.DefaultToolchainsBuildingResult"),
                lines.toString());
    }
}
