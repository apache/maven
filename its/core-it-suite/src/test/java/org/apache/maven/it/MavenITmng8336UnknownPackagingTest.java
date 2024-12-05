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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-8336">MNG-8336</a>.
 */
class MavenITmng8336UnknownPackagingTest extends AbstractMavenIntegrationTestCase {

    MavenITmng8336UnknownPackagingTest() {
        super("[4.0.0-beta-6,)");
    }

    /**
     *  Verify that the build succeeds
     */
    @Test
    void testUnknownPackaging() throws Exception {
        File testDir = ResourceExtractor.simpleExtractResources(getClass(), "/mng-8336-unknown-packaging");

        Verifier verifier = newVerifier(testDir.getAbsolutePath(), "remote");
        verifier.addCliArgument("clean");
        verifier.addCliArgument("org.codehaus.mojo:license-maven-plugin:2.4.0:add-third-party");
        verifier.execute();

        // verify log
        verifier.verifyTextNotInLog("Unable to obtain POM for artifact");
    }
}
