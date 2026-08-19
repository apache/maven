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

import org.junit.jupiter.api.Test;

/**
 * A plugin declared without a version must not resolve to a pre-release.
 * <p>
 * Repository metadata calls the newest non-snapshot version the RELEASE version, even when that
 * version is an alpha, beta, milestone or rc. Maven Central is in exactly that state whenever a
 * plugin's next major line is published as a beta while its stable line is the one users want --
 * selecting the pre-release then hands the build a plugin compiled against an unstable API.
 * <p>
 * The staged repository of this test offers a stable {@code 1.0} and a pre-release
 * {@code 2.0-beta-1} and names the pre-release as {@code <release>}. Maven has to pick {@code 1.0}.
 */
class MavenITgh12770UnversionedPluginPrereleaseTest extends AbstractMavenIntegrationTestCase {

    @Test
    void stableVersionIsPreferredOverPreRelease() throws Exception {
        // File on maven-4.0.x, Path on master -- `var` keeps this source portable across both lines.
        var testDir = extractResources("/gh-12770-unversioned-plugin-prerelease");

        Verifier verifier = newVerifier(testDir.toString());
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.deleteArtifacts("org.apache.maven.its.prerelease");
        verifier.filterFile("settings-template.xml", "settings.xml");
        verifier.addCliArgument("--settings");
        verifier.addCliArgument("settings.xml");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        verifier.verifyFilePresent("target/touch-stable.txt");
        verifier.verifyFileNotPresent("target/touch-prerelease.txt");
    }
}
