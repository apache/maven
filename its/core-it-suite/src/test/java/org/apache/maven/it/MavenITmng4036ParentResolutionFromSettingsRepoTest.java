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

import org.apache.maven.shared.verifier.Verifier;
import org.apache.maven.shared.verifier.util.ResourceExtractor;
import org.junit.jupiter.api.Test;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4036">MNG-4036</a>.
 *
 * @author Benjamin Bentmann
 */
public class MavenITmng4036ParentResolutionFromSettingsRepoTest extends AbstractMavenIntegrationTestCase {

    public MavenITmng4036ParentResolutionFromSettingsRepoTest() {
        super(ALL_MAVEN_VERSIONS);
    }

    /**
     * Verify that a parent POM is downloaded from a default-style remote repo defined in the settings.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitDefaultLayout() throws Exception {
        File testDir = ResourceExtractor.simpleExtractResources(getClass(), "/mng-4036/default");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.filterFile("settings.xml", "settings.xml", "UTF-8");
        verifier.deleteArtifacts("org.apache.maven.its.mng4036");
        verifier.addCliArgument("-s");
        verifier.addCliArgument("settings.xml");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        verifier.verifyArtifactPresent("org.apache.maven.its.mng4036", "parent", "0.2", "pom");
    }
}
