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

public class MavenITmng6981ProjectListShouldIncludeChildrenTest extends AbstractMavenIntegrationTestCase {

    private static final String RESOURCE_PATH = "/mng-6981-pl-should-include-children";

    public MavenITmng6981ProjectListShouldIncludeChildrenTest() {
        super("[4.0.0-alpha-1,)");
    }

    @Test
    public void testProjectListShouldIncludeChildrenByDefault() throws Exception {
        final File testDir = extractResources(RESOURCE_PATH);
        Verifier verifier = newVerifier(testDir.getAbsolutePath());

        verifier.addCliArgument("-pl");
        verifier.addCliArgument(":module-a");
        verifier.addCliArgument("compile");
        verifier.execute();
        verifier.verifyTextInLog("Building module-a-1 1.0");
    }

    /**
     * Since --pl's behavior is changed, make sure the alternative for building a pom without its children still works.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testFileSwitchAllowsExcludeOfChildren() throws Exception {
        final File testDir = extractResources(RESOURCE_PATH);
        Verifier verifier = newVerifier(testDir.getAbsolutePath());

        verifier.addCliArgument("-f");
        verifier.addCliArgument("module-a");
        verifier.addCliArgument("--non-recursive");
        verifier.setLogFileName("log-non-recursive.txt");
        verifier.addCliArgument("compile");
        verifier.execute();
        verifier.verifyTextNotInLog("Building module-a-1 1.0");
    }
}
