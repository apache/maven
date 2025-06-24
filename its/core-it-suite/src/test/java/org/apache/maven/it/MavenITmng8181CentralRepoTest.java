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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-8181">MNG-8181</a>.
 */
public class MavenITmng8181CentralRepoTest extends AbstractMavenIntegrationTestCase {
    public MavenITmng8181CentralRepoTest() {
        super("[4.0.0-beta-4,)");
    }

    /**
     *  Verify that the central url can be overridden by a user property.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitModel() throws Exception {
        File testDir = extractResources("/mng-8181-central-repo");

        Verifier verifier = newVerifier(testDir.getAbsolutePath(), null);
        verifier.setAutoclean(false);
        verifier.addCliArgument("--install-settings=install-settings.xml");
        verifier.addCliArgument("--settings=settings.xml");
        verifier.addCliArgument("-Dmaven.repo.local=" + testDir.toPath().resolve("target/local-repo"));
        verifier.addCliArgument("-Dmaven.repo.local.tail=target/null");
        verifier.addCliArgument("-Dmaven.repo.central=http://repo1.maven.org/");
        verifier.addCliArgument("validate");
        verifier.setHandleLocalRepoTail(false); // we want isolation to have Maven fail due non-HTTPS repo
        assertThrows(VerificationException.class, verifier::execute);
        verifier.verifyTextInLog("central (http://repo1.maven.org/, default, releases)");
    }
}
