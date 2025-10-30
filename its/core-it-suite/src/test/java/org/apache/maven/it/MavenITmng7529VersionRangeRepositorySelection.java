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

import org.junit.jupiter.api.Test;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-7529">MNG-7529</a>.
 */
public class MavenITmng7529VersionRangeRepositorySelection extends AbstractMavenIntegrationTestCase {

    /**
     * Test dependency resolution from a version range using multiple remote repositories
     * with snapshot or release enabled.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testit() throws Exception {
        Path testDir = extractResources("/mng-7529");

        // First, build the test plugin
        Verifier verifier = newVerifier(testDir.resolve("mng7529-plugin").getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.addCliArgument("install");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        // Then, run the test project that uses the plugin
        verifier = newVerifier(testDir.toString());
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.deleteArtifacts("org.apache.maven.its.mng7529");
        verifier.addCliArgument("--settings");
        verifier.addCliArgument("settings.xml");

        verifier.filterFile("settings-template.xml", "settings.xml");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();
    }
}
