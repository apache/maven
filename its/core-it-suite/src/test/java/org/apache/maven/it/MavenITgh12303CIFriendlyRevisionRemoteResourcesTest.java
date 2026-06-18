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
 * Verify that CI-friendly {@code ${revision}} versions are properly interpolated
 * when the {@code maven-remote-resources-plugin} resolves the project's own artifact
 * through the legacy resolver path.
 *
 * @see <a href="https://github.com/apache/maven/issues/12303">gh-12303</a>
 */
class MavenITgh12303CIFriendlyRevisionRemoteResourcesTest extends AbstractMavenIntegrationTestCase {

    MavenITgh12303CIFriendlyRevisionRemoteResourcesTest() {
        super("[4.0.0-rc-3,)");
    }

    @Test
    void testCiFriendlyRevisionWithRemoteResources() throws Exception {
        File testDir = extractResources("/gh-12303-ci-friendly-revision-remote-resources");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.deleteArtifacts("org.apache.maven.its.gh12303");
        verifier.filterFile("settings-template.xml", "settings.xml");
        verifier.addCliArgument("--settings");
        verifier.addCliArgument("settings.xml");
        verifier.addCliArgument("process-resources");
        verifier.execute();
        verifier.verifyErrorFreeLog();
    }
}
