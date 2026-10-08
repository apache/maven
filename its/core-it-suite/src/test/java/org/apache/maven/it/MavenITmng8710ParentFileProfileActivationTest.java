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

import org.junit.jupiter.api.Test;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-8710">MNG-8710</a>.
 *
 * A file-based profile defined in a parent POM that interpolates a parent property in
 * {@code <exists>} must activate for child modules (using each child's basedir) and inject
 * the profile's build plugins into the child's effective model.
 */
class MavenITmng8710ParentFileProfileActivationTest extends AbstractMavenIntegrationTestCase {

    @Test
    void testParentFileProfileAppliesToChild() throws Exception {
        Path testDir = extractResources("mng-8710");

        Verifier verifier = newVerifier(testDir);
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.deleteDirectory("my-lib/target");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        verifier.verifyFilePresent("target/profile-activated.txt");
        verifier.verifyFilePresent("my-lib/target/profile-activated.txt");

        verifier.verifyTextInLog("Profile activated in my-app: Test 1");
        verifier.verifyTextInLog("Profile activated in my-lib: Test 2");
    }
}
