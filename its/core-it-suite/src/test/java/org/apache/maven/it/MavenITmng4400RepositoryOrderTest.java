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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4400">MNG-4400</a>.
 *
 * @author Benjamin Bentmann
 */
public class MavenITmng4400RepositoryOrderTest extends AbstractMavenIntegrationTestCase {

    public MavenITmng4400RepositoryOrderTest() {
        super(ALL_MAVEN_VERSIONS);
    }

    /**
     * Verify that repositories declared in the settings.xml are accessed in their declaration order.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitSettingsRepos() throws Exception {
        File testDir = extractResources("/mng-4400");

        Verifier verifier = newVerifier(new File(testDir, "settings").getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteArtifacts("org.apache.maven.its.mng4400");
        verifier.filterFile("settings-template.xml", "settings.xml");
        verifier.addCliArgument("-s");
        verifier.addCliArgument("settings.xml");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        Properties checksums = verifier.loadProperties("target/checksum.properties");
        assertChecksum("d0a4998ff37a55f8de1dffccdff826eca365400f", checksums);
    }

    /**
     * Verify that repositories declared in the POM are accessed in their declaration order.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitPomRepos() throws Exception {
        File testDir = extractResources("/mng-4400");

        Verifier verifier = newVerifier(new File(testDir, "pom").getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteArtifacts("org.apache.maven.its.mng4400");
        verifier.filterFile("pom-template.xml", "pom.xml");
        verifier.addCliArgument("-s");
        verifier.addCliArgument("settings.xml");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        Properties checksums = verifier.loadProperties("target/checksum.properties");
        assertChecksum("d0a4998ff37a55f8de1dffccdff826eca365400f", checksums);
    }

    private void assertChecksum(String checksum, Properties checksums) {
        assertEquals(checksum, checksums.getProperty("dep-0.1.jar").toLowerCase(java.util.Locale.ENGLISH));
    }
}
