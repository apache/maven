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

import org.apache.maven.shared.verifier.VerificationException;
import org.apache.maven.shared.verifier.Verifier;
import org.apache.maven.shared.verifier.util.ResourceExtractor;
import org.junit.jupiter.api.Test;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-836">MNG-836</a>.
 *
 * @author Benjamin Bentmann
 *
 */
public class MavenITmng0836PluginParentResolutionTest extends AbstractMavenIntegrationTestCase {
    public MavenITmng0836PluginParentResolutionTest() {
        super(ALL_MAVEN_VERSIONS);
    }

    /**
     * Test that parent POMs referenced by a plugin POM can be resolved from ordinary repos, i.e. non-plugin repos.
     * As a motivation for this, imagine the plugin repository hosts only snapshots while the ordinary repository
     * hosts releases and a snapshot plugin might easily use a released parent.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitMNG836() throws Exception {
        File testDir = ResourceExtractor.simpleExtractResources(getClass(), "/mng-0836");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.deleteArtifacts("org.apache.maven.its.mng836");
        verifier.filterFile("settings-template.xml", "settings.xml", "UTF-8");
        verifier.addCliArgument("--settings");
        verifier.addCliArgument("settings.xml");
        // Maven 3.x aims to separate plugins and project dependencies (MNG-4191)
        if (matchesVersionRange("(,3.0-alpha-1),(3.0-alpha-1,3.0-alpha-7)")) {
            verifier.addCliArgument("validate");
            verifier.execute();
            verifier.verifyErrorFreeLog();
        } else {
            try {
                verifier.addCliArgument("validate");
                verifier.execute();
                verifier.verifyErrorFreeLog();
                fail("Plugin parent POM was erroneously resolved from non-plugin repository.");
            } catch (VerificationException e) {
                // expected
            }
        }
    }
}
