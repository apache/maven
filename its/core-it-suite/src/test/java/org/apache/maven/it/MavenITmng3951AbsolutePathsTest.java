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

import java.nio.file.Path;
import java.util.Properties;
import org.junit.jupiter.api.Test;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-3951">MNG-3951</a>.
 *
 * @author Benjamin Bentmann
 *
 */
public class MavenITmng3951AbsolutePathsTest extends AbstractMavenIntegrationTestCase {

    /**
     * Test that the paths retrieved from the core are always absolute, in particular the drive-relative paths on
     * Windows must be properly resolved.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitMNG3951() throws Exception {
        Path testDir = extractResources("mng-3951");

        Verifier verifier = newVerifier(testDir);

        /*
         * Cut off anything before the first file separator from the local repo path. This is harmless on a Unix-like
         * filesystem but will make the path drive-relative on Windows so we can check how Maven handles it.
         */
        Path repoDir = verifier.getLocalRepository();
        if (getRoot(repoDir).equals(getRoot(testDir))) {
            verifier.addCliArgument("-Dmaven.repo.local=" + repoDir.subpath(1, repoDir.getNameCount()));
        }

        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        verifier.verifyFilePresent("target/path.properties");
        Properties props = verifier.loadProperties("target/path.properties");

        ItUtils.assertCanonicalFileEquals(
                testDir.resolve("tmp"), Path.of(props.getProperty("fileParams.0")));
        ItUtils.assertCanonicalFileEquals(
                getRoot(testDir).resolve("tmp"), Path.of(props.getProperty("fileParams.1")));
        ItUtils.assertCanonicalFileEquals(repoDir, Path.of(props.getProperty("stringParams.0")));
    }

    private static Path getRoot(Path path) {
        Path root = path;
        for (Path dir = path; dir != null; dir = dir.getParent()) {
            root = dir;
        }
        return root;
    }
}
