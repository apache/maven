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
import java.util.Collections;

import org.junit.jupiter.api.Test;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-3139">MNG-3139</a>.
 *
 * @author Benjamin Bentmann
 *
 */
public class MavenITmng3139UseCachedMetadataOfBlacklistedRepoTest extends AbstractMavenIntegrationTestCase {

    public MavenITmng3139UseCachedMetadataOfBlacklistedRepoTest() {
        super("[2.0.11,2.1.0-M1),[2.1.0,)");
    }

    /**
     * Test that locally cached metadata of blacklisted repositories is consulted to resolve metaversions.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitMNG3139() throws Exception {
        File testDir = extractResources("/mng-3139");

        // phase 1: get the metadata into the local repo

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteArtifacts("org.apache.maven.its.mng3139");

        verifier.filterFile("settings-template.xml", "settings.xml");
        verifier.setLogFileName("log1.txt");
        verifier.addCliArgument("--settings");
        verifier.addCliArgument("settings.xml");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        // phase 2: trigger blacklisting of repo (by invalid URL) and check previously downloaded metadata is still used

        verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setAutoclean(false);

        verifier.filterFile(
                "settings-template.xml",
                "settings.xml",
                "UTF-8",
                Collections.singletonMap("@baseurl@", "http://localhost:63412"));
        verifier.setLogFileName("log2.txt");
        verifier.addCliArgument("--settings");
        verifier.addCliArgument("settings.xml");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();
    }
}
