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

import org.junit.jupiter.api.Test;

/**
 * This is a test set for <a href="https://github.com/apache/maven/issues/11381">GH-11381</a>.
 *
 * Verifies that relative targetPath in resources is resolved relative to the output directory
 * (target/classes) and not relative to the project base directory, maintaining Maven 3.x behavior.
 *
 * @since 4.0.0-rc-4
 */
class MavenITgh11381ResourceTargetPathTest extends AbstractMavenIntegrationTestCase {

    /**
     * Verify that resources with relative targetPath are copied to target/classes/targetPath
     * and not to the project root directory.
     *
     * @throws Exception in case of failure
     */
    @Test
    void testRelativeTargetPathInResources() throws Exception {
        File testDir = extractResources("/gh-11381");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.addCliArgument("process-resources");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        // Verify that resources were copied to target/classes/target-dir (Maven 3.x behavior)
        verifier.verifyFilePresent("target/classes/target-dir/test.yml");
        verifier.verifyFilePresent("target/classes/target-dir/subdir/another.yml");

        // Verify that resources were NOT copied to the project root target-dir directory
        verifier.verifyFileNotPresent("target-dir/test.yml");
        verifier.verifyFileNotPresent("target-dir/subdir/another.yml");
    }
}

