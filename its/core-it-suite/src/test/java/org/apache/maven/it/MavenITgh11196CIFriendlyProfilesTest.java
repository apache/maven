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
import java.util.Properties;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * This is a test set for <a href="https://github.com/apache/maven/issues/11196">#11196</a>.
 * It verifies that changes to ${revision} in profiles propagate to the final project version.
 *
 * @author Apache Maven Team
 * @since 4.0.0-rc-4
 *
 */
class MavenITgh11196CIFriendlyProfilesTest extends AbstractMavenIntegrationTestCase {

    /**
     * Verify that CI-friendly version resolution works correctly with profile properties.
     * Without profile activation, the version should be "0.2.0+dev".
     *
     * @throws Exception in case of failure
     */
    @Test
    void testCiFriendlyVersionWithoutProfile() throws Exception {
        File testDir = extractResources("/gh-11196-ci-friendly-profiles");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        Properties props = verifier.loadProperties("target/versions.properties");
        assertEquals("0.2.0+dev", props.getProperty("project.version"));
        assertEquals("0.2.0+dev", props.getProperty("project.properties.revision"));
        assertEquals("0.2.0", props.getProperty("project.properties.baseVersion"));
    }

    /**
     * Verify that CI-friendly version resolution works correctly with profile properties.
     * With the releaseBuild profile activated, the version should be "0.2.0" (without +dev).
     *
     * @throws Exception in case of failure
     */
    @Test
    void testCiFriendlyVersionWithReleaseProfile() throws Exception {
        File testDir = extractResources("/gh-11196-ci-friendly-profiles");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.addCliArgument("-PreleaseBuild");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        Properties props = verifier.loadProperties("target/release-profile.properties");
        assertEquals("0.2.0", props.getProperty("project.version"));
        assertEquals("0.2.0", props.getProperty("project.properties.revision"));
        assertEquals("0.2.0", props.getProperty("project.properties.baseVersion"));
    }
}
