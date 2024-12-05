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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-2790">MNG-2790</a>.
 *
 * @author Benjamin Bentmann
 *
 */
public class MavenITmng2790LastUpdatedMetadataTest extends AbstractMavenIntegrationTestCase {

    public MavenITmng2790LastUpdatedMetadataTest() {
        super("(2.0.4,)");
    }

    /**
     * Verify that the field lastUpdated of existing local repo metadata is updated upon install of new a snapshot.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitMNG2790() throws Exception {
        File testDir = extractResources("/mng-2790");

        Date now = new Date();

        /*
         * Phase 1: Install initial snapshot into local repo.
         */
        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.deleteArtifacts("org.apache.maven.its.mng2790");
        verifier.setAutoclean(false);
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        File metadataArtifactVersionFile =
                new File(verifier.getArtifactMetadataPath("org.apache.maven.its.mng2790", "project", "1.0-SNAPSHOT"));
        File metadataArtifactFile =
                new File(verifier.getArtifactMetadataPath("org.apache.maven.its.mng2790", "project"));

        Date artifactVersionLastUpdated1 = getLastUpdated(metadataArtifactVersionFile);
        Date artifactLastUpdated1 = getLastUpdated(metadataArtifactFile);

        // sanity check: timestamps shouldn't differ by more than 10 min from now (i.e. timezone is UTC)
        assertTrue(
                Math.abs(artifactVersionLastUpdated1.getTime() - now.getTime()) < 10 * 60 * 1000,
                artifactVersionLastUpdated1 + " ~ " + now);
        assertTrue(
                Math.abs(artifactLastUpdated1.getTime() - now.getTime()) < 10 * 60 * 1000,
                artifactLastUpdated1 + " ~ " + now);

        // enforce some advance of time
        Thread.sleep(1000);

        /*
         * Phase 2: Re-install snapshot and check for proper timestamp update in local metadata.
         */
        verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        Date artifactVersionLastUpdated2 = getLastUpdated(metadataArtifactVersionFile);
        Date artifactLastUpdated2 = getLastUpdated(metadataArtifactFile);

        // check that new timestamps are strictly later than from original install
        assertTrue(
                artifactVersionLastUpdated2.after(artifactVersionLastUpdated1),
                artifactVersionLastUpdated1 + " < " + artifactVersionLastUpdated2);
        assertTrue(
                artifactLastUpdated2.after(artifactLastUpdated1), artifactLastUpdated1 + " < " + artifactLastUpdated2);
    }

    private Date getLastUpdated(File metadataFile) throws Exception {
        String xml = Files.readString(metadataFile.toPath());
        String timestamp = xml.replaceAll("(?s)\\A.*<lastUpdated>\\s*([0-9]++)\\s*</lastUpdated>.*\\z", "$1");
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return format.parse(timestamp);
    }
}
