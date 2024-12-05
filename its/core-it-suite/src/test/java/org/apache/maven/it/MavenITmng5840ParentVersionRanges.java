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

public class MavenITmng5840ParentVersionRanges extends AbstractMavenIntegrationTestCase {
    public MavenITmng5840ParentVersionRanges() {
        super("[3.3,)");
    }

    @Test
    public void testParentRangeRelativePathPointsToWrongVersion() throws Exception {
        File testDir = extractResources("/mng-5840-relative-path-range-negative");

        Verifier verifier = newVerifier(new File(testDir, "parent-1").getAbsolutePath());
        verifier.addCliArgument("install");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        verifier = newVerifier(new File(testDir, "child").getAbsolutePath());
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();
    }

    @Test
    public void testParentRangeRelativePathPointsToCorrectVersion() throws Exception {
        File testDir = extractResources("/mng-5840-relative-path-range-positive");

        Verifier verifier = newVerifier(new File(testDir, "parent-1").getAbsolutePath());
        verifier.addCliArgument("install");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        verifier = newVerifier(new File(testDir, "child").getAbsolutePath());
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();
    }
}
