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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-8341">MNG-8341</a>.
 */
class MavenITmng8360SubprojectProfileActivationTest extends AbstractMavenIntegrationTestCase {

    MavenITmng8360SubprojectProfileActivationTest() {
        super("[4.0.0-beta-6,)");
    }

    /**
     *  Verify that the build succeeds
     */
    @Test
    void testDeadlock() throws Exception {
        File testDir = ResourceExtractor.simpleExtractResources(getClass(), "/mng-8360");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.addCliArguments("-s", "settings.xml");
        verifier.addCliArguments("-f", "module1");
        verifier.addCliArgument("org.apache.maven.plugins:maven-help-plugin:3.3.0:active-profiles");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        // verify the three profiles have been activated
        verifier.verifyTextInLog("settings-xml-activeProfiles");
        verifier.verifyTextInLog("profile_active_from_mvn_config");
        verifier.verifyTextInLog("profile_active_from_condition");
    }
}
