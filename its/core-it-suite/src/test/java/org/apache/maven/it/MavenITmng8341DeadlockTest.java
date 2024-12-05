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
import org.junit.jupiter.api.Timeout;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-8341">MNG-8341</a>.
 */
class MavenITmng8341DeadlockTest extends AbstractMavenIntegrationTestCase {

    MavenITmng8341DeadlockTest() {
        super("[4.0.0-beta-6,)");
    }

    /**
     *  Verify that the build succeeds
     */
    @Timeout(value = 60)
    @Test
    void testDeadlock() throws Exception {
        File testDir = ResourceExtractor.simpleExtractResources(getClass(), "/mng-8341-deadlock");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();
    }
}
