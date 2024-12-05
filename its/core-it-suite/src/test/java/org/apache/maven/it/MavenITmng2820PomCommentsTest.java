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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-2820">MNG-2820</a>.
 *
 * @author Benjamin Bentmann
 */
public class MavenITmng2820PomCommentsTest extends AbstractMavenIntegrationTestCase {

    public MavenITmng2820PomCommentsTest() {
        super("[2.0.5,)");
    }

    /**
     * Verify that installed/deployed POMs retain any XML-comments like license headers.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testit() throws Exception {
        File testDir = extractResources("/mng-2820");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.deleteArtifacts("org.apache.maven.its.mng2820");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        File installed = new File(verifier.getArtifactPath("org.apache.maven.its.mng2820", "test", "0.1", "pom"));
        assertPomComments(installed);

        File deployed = new File(testDir, "target/repo/org/apache/maven/its/mng2820/test/0.1/test-0.1.pom");
        assertPomComments(deployed);
    }

    private void assertPomComments(File pomFile) throws Exception {
        String pom = Files.readString(pomFile.toPath());
        assertPomComment(pom, "DOCUMENT-COMMENT-PRE-1");
        assertPomComment(pom, "DOCUMENT-COMMENT-PRE-2");
        assertPomComment(pom, "DOCUMENT-COMMENT-POST-1");
        assertPomComment(pom, "DOCUMENT-COMMENT-POST-2");
        assertPomComment(pom, "MODEL-COMMENT");
        assertPomComment(pom, "INLINE-COMMENT-1");
        assertPomComment(pom, "INLINE-COMMENT-2");
        assertPomComment(pom, "INLINE-COMMENT-3");
    }

    private void assertPomComment(String pom, String comment) throws Exception {
        assertTrue(pom.contains(comment), "Missing comment: " + comment);
    }
}
