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
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * This is a test set for
 * <a href="https://issues.apache.org/jira/browse/MNG-7716">MNG-7716</a>.
 * Executing the project should not deadlock
 *
 */
class MavenITmng7716BuildDeadlock extends AbstractMavenIntegrationTestCase {

    public MavenITmng7716BuildDeadlock() {
        super("[3.8.8,3.9.0),[3.9.1,4.0.0-alpha-1),[4.0.0-alpha-5,)");
    }

    /**
     * Verify that maven invocation works (no NPE/error happens).
     *
     * @throws Exception in case of failure
     */
    @Test
    @Timeout(value = 120, unit = TimeUnit.SECONDS)
    void testNoDeadlockAtVersionUpdate() throws Exception {
        File testDir = extractResources("/mng-7716");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.addCliArgument("-f");
        verifier.addCliArgument("settings");
        verifier.addCliArgument("install");
        verifier.setLogFileName("log-settings.txt");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        verifier = newVerifier(testDir.getAbsolutePath());
        verifier.addCliArgument("-T1C");
        verifier.addCliArgument("org.codehaus.mojo:versions-maven-plugin:2.15.0:set");
        verifier.addCliArgument("-DnewVersion=1.2.3");
        verifier.execute();
        verifier.verifyErrorFreeLog();
    }
}
