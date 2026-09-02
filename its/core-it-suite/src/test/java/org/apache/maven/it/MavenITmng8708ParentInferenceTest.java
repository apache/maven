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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-8708">MNG-8708</a>.
 *
 * @since 4.1.0
 */
class MavenITmng8708ParentInferenceTest extends AbstractMavenIntegrationTestCase {

    MavenITmng8708ParentInferenceTest() {
        super("[4.0.0-rc-7,)");
    }

    private static final String PARENT_DECLARATION_WARNING =
            "'parent.relativePath' only specify relativePath or groupId/artifactId in modelVersion 4.1.0";

    @Test
    void testSupportedParentInference() throws Exception {
        File testDir = extractResources("/mng-8708-parent-inference/supported");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();
        verifier.verifyTextNotInLog(PARENT_DECLARATION_WARNING);
    }

    /**
     * Three-level hierarchy: grandparent (with version) → versionless mid parent → child.
     * The child specifies only groupId/artifactId for the mid parent, which itself
     * inherits its version from the grandparent. Exercises the fallback path in
     * {@code inferParentVersion} where the parent model's version comes from its own parent.
     */
    @Test
    void testThreeLevelParentInference() throws Exception {
        File testDir = extractResources("/mng-8708-parent-inference/three-level");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();
        verifier.verifyTextNotInLog(PARENT_DECLARATION_WARNING);
    }

    @Test
    void testExplicitPathAndCoordinatesStillWarn() throws Exception {
        File testDir = extractResources("/mng-8708-parent-inference/explicit-both");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();
        verifier.verifyTextInLog(PARENT_DECLARATION_WARNING);
    }
}
