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
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Properties;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-1751">MNG-1751</a>.
 *
 * @author Benjamin Bentmann
 */
public class MavenITmng1751ForcedMetadataUpdateDuringDeploymentTest extends AbstractMavenIntegrationTestCase {

    public MavenITmng1751ForcedMetadataUpdateDuringDeploymentTest() {
        super("[3.0-beta-1,)");
    }

    /**
     * Verify that deployment always updates the metadata even if its remote timestamp currently refers to
     * the future.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testit() throws Exception {
        File testDir = extractResources("/mng-1751");

        File dir = new File(testDir, "repo/org/apache/maven/its/mng1751/dep/0.1-SNAPSHOT");
        File templateMetadataFile = new File(dir, "template-metadata.xml");
        File metadataFile = new File(dir, "maven-metadata.xml");
        Files.copy(templateMetadataFile.toPath(), metadataFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        String checksum = ItUtils.calcHash(metadataFile, "SHA-1");
        Files.writeString(metadataFile.toPath().getParent().resolve(metadataFile.getName() + ".sha1"), checksum);

        // phase 1: deploy a new snapshot, this should update the metadata despite its future timestamp
        Verifier verifier = newVerifier(new File(testDir, "dep").getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteArtifacts("org.apache.maven.its.mng1751");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        // phase 2: resolve snapshot, if the previous deployment didn't update the metadata, we get the wrong file
        verifier = newVerifier(new File(testDir, "test").getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteArtifacts("org.apache.maven.its.mng1751");
        verifier.filterFile("settings-template.xml", "settings.xml");
        verifier.addCliArgument("--settings");
        verifier.addCliArgument("settings.xml");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        Properties checksums = verifier.loadProperties("target/checksum.properties");
        String sha1 = checksums.getProperty("dep-0.1-SNAPSHOT.jar", "").toLowerCase(java.util.Locale.ENGLISH);
        assertEquals(40, sha1.length(), sha1);
        assertNotEquals("fc081cd365b837dcb01eb9991f21c409b155ea5c", sha1);
    }
}
