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

import org.apache.maven.shared.verifier.Verifier;
import org.apache.maven.shared.verifier.util.ResourceExtractor;
import org.junit.jupiter.api.Test;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-3951">MNG-3951</a>.
 *
 * @author Benjamin Bentmann
 *
 */
public class MavenITmng3951AbsolutePathsTest extends AbstractMavenIntegrationTestCase {

    public MavenITmng3951AbsolutePathsTest() {
        super("(2.0.10,2.1.0-M1),(2.1.0-M1,)");
    }

    /**
     * Test that the paths retrieved from the core are always absolute, in particular the drive-relative paths on
     * Windows must be properly resolved.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitMNG3951() throws Exception {
        File testDir = ResourceExtractor.simpleExtractResources(getClass(), "/mng-3951");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());

        /*
         * Cut off anything before the first file separator from the local repo path. This is harmless on a Unix-like
         * filesystem but will make the path drive-relative on Windows so we can check how Maven handles it.
         */
        String repoDir = new File(verifier.getLocalRepository()).getAbsolutePath();
        if (getRoot(new File(repoDir)).equals(getRoot(testDir))) {
            // NOTE: We can only test the local repo if it resides on the same drive as the test
            verifier.setLocalRepo(repoDir.substring(repoDir.indexOf(File.separator)));
        }

        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        verifier.verifyFilePresent("target/path.properties");
        Properties props = verifier.loadProperties("target/path.properties");

        assertCanonicalFileEquals(
                new File(testDir, "tmp").getAbsoluteFile(), new File(props.getProperty("fileParams.0")));
        assertCanonicalFileEquals(
                new File(getRoot(testDir), "tmp").getAbsoluteFile(), new File(props.getProperty("fileParams.1")));
        assertCanonicalFileEquals(new File(repoDir), new File(props.getProperty("stringParams.0")));
    }

    private static File getRoot(File path) {
        File root = path;
        for (File dir = path; dir != null; dir = dir.getParentFile()) {
            root = dir;
        }
        return root;
    }
}
