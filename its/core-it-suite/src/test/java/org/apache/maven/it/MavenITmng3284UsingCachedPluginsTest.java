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

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-3284">MNG-3284</a>:
 * that explicitly defined plugins are used, not the one that is cached.
 */
public class MavenITmng3284UsingCachedPluginsTest extends AbstractMavenIntegrationTestCase {

    public MavenITmng3284UsingCachedPluginsTest() {
        super("[2.1.0-M2,)");
    }

    /**
     * Verify that the effective plugin versions used for a project are not influenced by other instances of this
     * plugin in the reactor, i.e. each module gets exactly the plugin version it declares.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitMNG3284() throws Exception {
        File testDir = extractResources("/mng-3284");

        /*
         * Phase 1: Ensure both plugin versions are already in the local repo. This is a crucial prerequisite for the
         * test because downloading the plugins just-in-time during the test build would trigger a timestamp-based
         * reloading of the plugin container by the DefaultPluginManager in Maven 2.x, thereby hiding the bug we want
         * to expose here.
         */
        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteArtifacts("org.apache.maven.its.mng3284");
        verifier.filterFile("settings-template.xml", "settings.xml");
        verifier.addCliArgument("--settings");
        verifier.addCliArgument("settings.xml");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        /*
         * Phase 2: Now that the plugin versions have been downloaded to the local repo, run the actual test.
         */
        verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteDirectory("mod-a/target");
        verifier.deleteDirectory("mod-b/target");
        verifier.addCliArgument("--settings");
        verifier.addCliArgument("settings.xml");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        verifier.verifyFilePresent("mod-a/target/version-0.1.txt");
        verifier.verifyFileNotPresent("mod-a/target/version-0.2.txt");
        verifier.verifyFilePresent("mod-b/target/version-0.2.txt");
        verifier.verifyFileNotPresent("mod-b/target/version-0.1.txt");
    }
}
