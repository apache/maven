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
        // note: intentionally bad URL, we just want tu ensure that this bad URL is used
        verifier.addCliArgument("-Dmaven.repo.central=https://repo1.maven.org");
        verifier.addCliArgument("validate");
        verifier.setHandleLocalRepoTail(false); // we want isolation to have Maven fail due bad URL
        assertThrows(VerificationException.class, verifier::execute);
        // error is
        // PluginResolutionException: Plugin eu.maveniverse.maven.mimir:extension3:XXX or one of its dependencies could
        // not be resolved:
        //	 Could not find artifact eu.maveniverse.maven.mimir:extension3:jar:XXX in central (https://repo1.maven.org)
        verifier.verifyTextInLog("central (https://repo1.maven.org)");
    }
}
