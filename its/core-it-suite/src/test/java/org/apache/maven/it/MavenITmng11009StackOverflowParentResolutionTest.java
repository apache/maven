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
 * This is a test set for <a href="https://github.com/apache/maven/issues/11009">Issue #11009</a>.
 *
 * @author Guillaume Nodet
 */
public class MavenITmng11009StackOverflowParentResolutionTest extends AbstractMavenIntegrationTestCase {

    /**
     * Test that circular parent resolution doesn't cause a StackOverflowError during project model building.
     * This reproduces the issue where:
     * - Root pom.xml has parent with relativePath="parent"
     * - parent/pom.xml has parent without relativePath (defaults to "../pom.xml")
     * - This creates a circular parent resolution that causes stack overflow in hashCode calculation
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testStackOverflowInParentResolution() throws Exception {
        File testDir = extractResources("/mng-11009-stackoverflow-parent-resolution");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteArtifacts("org.apache.maven.its.mng11009");

        // This should fail gracefully with a meaningful error message, not with StackOverflowError
        try {
            verifier.addCliArgument("validate");
            verifier.execute();
            // If we get here without StackOverflowError, the fix is working
            // The build may still fail with a different error (circular dependency), but that's expected
        } catch (Exception e) {
            // Check that it's not a StackOverflowError
            String errorMessage = e.getMessage();
            if (errorMessage != null && errorMessage.contains("StackOverflowError")) {
                throw new AssertionError("Build failed with StackOverflowError, which should be fixed", e);
            }
            // Other errors are acceptable as the POM structure is intentionally problematic
        }

        // The main goal is to not get a StackOverflowError
        // We expect some kind of circular dependency error instead
    }
}
