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

public class MavenITmng8648ProjectEventsTest extends AbstractMavenIntegrationTestCase {

    public MavenITmng8648ProjectEventsTest() {
        super("[4.0.0-rc-4,)");
    }

    @Test
    public void test() throws Exception {
        File extensionDir = extractResources("/mng-8648/extension");

        Verifier verifier = newVerifier(extensionDir.getAbsolutePath());
        verifier.addCliArgument("install");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        File projectDir = extractResources("/mng-8648/project");

        verifier = newVerifier(projectDir.getAbsolutePath());
        verifier.addCliArguments("compile", "-b", "concurrent");
        try {
            verifier.execute();
        } catch (VerificationException expected) {
        }

        // The root project is marked as successful with the traditional builder
        // With the concurrent builder, it gets no finish event and in the reactor summary it is listed as "skipped"
        verifier.verifyTextInLog("org.apache.maven.its.mng8648:root:pom:1-SNAPSHOT ProjectStarted");
        verifier.verifyTextInLog("org.apache.maven.its.mng8648:root:pom:1-SNAPSHOT ProjectSucceeded");
        verifier.verifyTextInLog("org.apache.maven.its.mng8648:subproject-a:jar:1-SNAPSHOT ProjectStarted");
        verifier.verifyTextInLog("org.apache.maven.its.mng8648:subproject-a:jar:1-SNAPSHOT ProjectSucceeded");
        verifier.verifyTextInLog("org.apache.maven.its.mng8648:subproject-b:jar:1-SNAPSHOT ProjectStarted");
        verifier.verifyTextInLog("org.apache.maven.its.mng8648:subproject-b:jar:1-SNAPSHOT ProjectSucceeded");
        verifier.verifyTextInLog("org.apache.maven.its.mng8648:subproject-c:jar:1-SNAPSHOT ProjectStarted");
        verifier.verifyTextInLog("org.apache.maven.its.mng8648:subproject-c:jar:1-SNAPSHOT ProjectFailed");
        // With the traditional builder, project D is not reported at all (it is never even started) and in the reactor
        // summary it is listed as "skipped"
        // With the concurrent builder, it is currently reported as successful, which seems wrong, since it depends on
        // a failed project
        verifier.verifyTextNotInLog("Failed to notify spy org.apache.maven.its.mng8648.ProjectEventSpy");
    }
}
