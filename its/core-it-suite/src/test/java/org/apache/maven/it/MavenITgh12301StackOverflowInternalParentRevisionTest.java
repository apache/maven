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
 * This is a test set for <a href="https://github.com/apache/maven/issues/12301">Issue #12301</a>.
 * Verifies that a project with an internal parent in a subdirectory using CI-friendly
 * {@code ${revision}} and a {@code .mvn/} root marker does not cause a StackOverflowError.
 */
public class MavenITgh12301StackOverflowInternalParentRevisionTest extends AbstractMavenIntegrationTestCase {

    @Test
    public void testNoStackOverflowWithInternalParentAndRevision() throws Exception {
        File testDir = extractResources("/gh-12301-stackoverflow-internal-parent");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteArtifacts("org.apache.maven.its.gh12301");
        verifier.addCliArgument("-Drevision=1.0-SNAPSHOT");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();
    }
}
