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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-1088">MNG-1088</a>.
 *
 * <p>Verifies that the concurrent builder automatically builds a reactor plugin project up to
 * the READY phase when another project in the reactor uses that plugin, even if the user
 * requested a phase before "compile" (e.g. "initialize").
 *
 * @author Brett Porter
 */
class MavenITmng1088ReactorPluginResolutionTest extends AbstractMavenIntegrationTestCase {

    /**
     * Test that a reactor plugin is automatically compiled and made available when the
     * concurrent builder detects a cross-project plugin dependency, even when the user
     * only requested a lifecycle phase before "compile" (here: "initialize").
     *
     * @throws Exception in case of failure
     */
    @Test
    void testitMNG1088() throws Exception {
        Path testDir = extractResources("mng-1088");

        Verifier verifier = newVerifier(testDir);
        verifier.setAutoclean(false);
        verifier.deleteDirectory("client/target");
        verifier.deleteArtifacts("org.apache.maven.its.mng1088");
        verifier.addCliArgument("-b");
        verifier.addCliArgument("concurrent");
        // NOTE: It's essential part of the test to invoke a phase before "compile"
        verifier.addCliArgument("initialize");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        verifier.verifyFilePresent("client/target/touch.txt");
    }
}
