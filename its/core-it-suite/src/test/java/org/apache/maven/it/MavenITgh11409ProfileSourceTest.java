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
 * This is a test for <a href="https://github.com/apache/maven/issues/11409">GH-11409</a>.
 * Verifies that profiles activated in parent POMs are correctly reported with the parent POM
 * as the source, not the child project.
 *
 * @since 4.0.0
 */
class MavenITgh11409ProfileSourceTest extends AbstractMavenIntegrationTestCase {

    /**
     * Verify that help:active-profiles reports correct source for profiles activated in parent POM.
     *
     * @throws Exception in case of failure
     */
    @Test
    void testProfileSourceInMultiModuleProject() throws Exception {
        File testDir = extractResources("/gh-11409");

        Verifier verifier = newVerifier(new File(testDir, "subproject").getAbsolutePath());
        verifier.addCliArgument("help:active-profiles");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        // Verify that the parent profile is reported with the parent as the source
        // Note: Profile sources use groupId:artifactId:version format (without packaging)
        verifier.verifyTextInLog("parent-profile (source: test.gh11409:parent:1.0-SNAPSHOT)");

        // Verify that the child profile is reported with the child as the source
        verifier.verifyTextInLog("child-profile (source: test.gh11409:subproject:1.0-SNAPSHOT)");
    }
}

