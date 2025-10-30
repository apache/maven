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
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * IT that verifies that lifecycle participant
 * methods are invoked even with various build failures/errors.
 * @since 3.2.2
 *
 */
@SuppressWarnings("checkstyle:UnusedLocalVariable")
class MavenITmng5640LifecycleParticipantAfterSessionEnd extends AbstractMavenIntegrationTestCase {

    /**
     * IT executing a Maven build that has UT failure.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testBuildFailureUTFail() throws Exception {
        Path testDir = extractResources("/mng-5640-lifecycleParticipant-afterSession");
        File extensionDir = testDir.resolve("extension");
        File projectDir = testDir.resolve("buildfailure-utfail");
        Verifier verifier;

        // install the test plugin
        verifier = newVerifier(extensionDir.toString(), "remote");
        verifier.addCliArgument("install");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        // build the test project
        verifier = newVerifier(projectDir.toString(), "remote");
        verifier.addCliArgument("package");
        assertThrows(VerificationException.class, verifier::execute, "The build should fail");
        verifier.verifyTextInLog("testApp(org.apache.maven.its.mng5640.FailingTest)");

        verifier.verifyFilePresent("target/afterProjectsRead.txt");
        // See https://issues.apache.org/jira/browse/MNG-5641
        // verifier.verifyFilePresent( "target/afterSessionStart.txt" );
        verifier.verifyFilePresent("target/afterSessionEnd.txt");
    }

    /**
     * IT executing a Maven build that has missing dependency.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testBuildFailureMissingDependency() throws Exception {
        Path testDir = extractResources("/mng-5640-lifecycleParticipant-afterSession");
        File extensionDir = testDir.resolve("extension");
        File projectDir = testDir.resolve("buildfailure-depmissing");
        Verifier verifier;

        // install the test plugin
        verifier = newVerifier(extensionDir.toString(), "remote");
        verifier.addCliArgument("install");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        // build the test project
        verifier = newVerifier(projectDir.toString(), "remote");
        verifier.addCliArgument("package");
        VerificationException exception =
                assertThrows(VerificationException.class, verifier::execute, "The build should fail");

        verifier.verifyFilePresent("target/afterProjectsRead.txt");
        // See https://issues.apache.org/jira/browse/MNG-5641
        // verifier.verifyFilePresent( "target/afterSessionStart.txt" );
        verifier.verifyFilePresent("target/afterSessionEnd.txt");
    }

    /**
     * IT executing a Maven build that has failing Maven plugin.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testBuildError() throws Exception {
        Path testDir = extractResources("/mng-5640-lifecycleParticipant-afterSession");
        File extensionDir = testDir.resolve("extension");
        File pluginDir = testDir.resolve("badplugin");
        File projectDir = testDir.resolve("builderror-mojoex");
        Verifier verifier;

        // install the test plugin
        verifier = newVerifier(extensionDir.toString(), "remote");
        verifier.addCliArgument("install");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        // install the bad plugin
        verifier = newVerifier(pluginDir.toString(), "remote");
        verifier.addCliArgument("install");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        // build the test project
        verifier = newVerifier(projectDir.toString(), "remote");
        verifier.addCliArgument("package");
        VerificationException exception =
                assertThrows(VerificationException.class, verifier::execute, "The build should fail");

        verifier.verifyFilePresent("target/afterProjectsRead.txt");
        // See https://issues.apache.org/jira/browse/MNG-5641
        // verifier.verifyFilePresent( "target/afterSessionStart.txt" );
        verifier.verifyFilePresent("target/afterSessionEnd.txt");
    }

    /**
     * IT executing a Maven build that has failing Maven plugin throwing RuntimeException.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testBuildErrorRt() throws Exception {
        Path testDir = extractResources("/mng-5640-lifecycleParticipant-afterSession");
        File extensionDir = testDir.resolve("extension");
        File pluginDir = testDir.resolve("badplugin");
        File projectDir = testDir.resolve("builderror-runtimeex");
        Verifier verifier;

        // install the test plugin
        verifier = newVerifier(extensionDir.toString(), "remote");
        verifier.addCliArgument("install");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        // install the bad plugin
        verifier = newVerifier(pluginDir.toString(), "remote");
        verifier.addCliArgument("install");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        // build the test project
        verifier = newVerifier(projectDir.toString(), "remote");
        verifier.addCliArgument("package");
        VerificationException exception =
                assertThrows(VerificationException.class, verifier::execute, "The build should fail");

        verifier.verifyFilePresent("target/afterProjectsRead.txt");
        // See https://issues.apache.org/jira/browse/MNG-5641
        // verifier.verifyFilePresent( "target/afterSessionStart.txt" );
        verifier.verifyFilePresent("target/afterSessionEnd.txt");
    }
}
