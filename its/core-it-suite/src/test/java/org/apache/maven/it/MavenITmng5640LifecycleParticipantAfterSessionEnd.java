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

import org.apache.maven.shared.verifier.VerificationException;
import org.apache.maven.shared.verifier.Verifier;
import org.apache.maven.shared.verifier.util.ResourceExtractor;
import org.junit.jupiter.api.Test;

/**
 * IT that verifies that lifecycle participant
 * methods are invoked even with various build failures/errors.
 */
public class MavenITmng5640LifecycleParticipantAfterSessionEnd extends AbstractMavenIntegrationTestCase {
    public MavenITmng5640LifecycleParticipantAfterSessionEnd() {
        super("[3.2.2,)");
    }

    /**
     * IT executing a Maven build that has UT failure.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testBuildFailureUTFail() throws Exception {
        File testDir =
                ResourceExtractor.simpleExtractResources(getClass(), "/mng-5640-lifecycleParticipant-afterSession");
        File extensionDir = new File(testDir, "extension");
        File projectDir = new File(testDir, "buildfailure-utfail");

        Verifier verifier;

        // install the test plugin
        verifier = newVerifier(extensionDir.getAbsolutePath(), "remote");
        verifier.addCliArgument("install");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        // build the test project
        verifier = newVerifier(projectDir.getAbsolutePath(), "remote");
        try {
            verifier.addCliArgument("package");
            verifier.execute();
            fail("The build should fail");
        } catch (VerificationException e) {
            // expected, as the build will fail due to always failing UT
        }
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
        File testDir =
                ResourceExtractor.simpleExtractResources(getClass(), "/mng-5640-lifecycleParticipant-afterSession");
        File extensionDir = new File(testDir, "extension");
        File projectDir = new File(testDir, "buildfailure-depmissing");

        Verifier verifier;

        // install the test plugin
        verifier = newVerifier(extensionDir.getAbsolutePath(), "remote");
        verifier.addCliArgument("install");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        // build the test project
        verifier = newVerifier(projectDir.getAbsolutePath(), "remote");
        try {
            verifier.addCliArgument("package");
            verifier.execute();
            fail("The build should fail");
        } catch (VerificationException e) {
            // expected, as the build will fail due to always failing UT
        }

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
        File testDir =
                ResourceExtractor.simpleExtractResources(getClass(), "/mng-5640-lifecycleParticipant-afterSession");
        File extensionDir = new File(testDir, "extension");
        File pluginDir = new File(testDir, "badplugin");
        File projectDir = new File(testDir, "builderror-mojoex");

        Verifier verifier;

        // install the test plugin
        verifier = newVerifier(extensionDir.getAbsolutePath(), "remote");
        verifier.addCliArgument("install");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        // install the bad plugin
        verifier = newVerifier(pluginDir.getAbsolutePath(), "remote");
        verifier.addCliArgument("install");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        // build the test project
        verifier = newVerifier(projectDir.getAbsolutePath(), "remote");
        try {
            verifier.addCliArgument("package");
            verifier.execute();
            fail("The build should fail");
        } catch (VerificationException e) {
            // expected, as the build will fail due to always failing UT
        }

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
        File testDir =
                ResourceExtractor.simpleExtractResources(getClass(), "/mng-5640-lifecycleParticipant-afterSession");
        File extensionDir = new File(testDir, "extension");
        File pluginDir = new File(testDir, "badplugin");
        File projectDir = new File(testDir, "builderror-runtimeex");

        Verifier verifier;

        // install the test plugin
        verifier = newVerifier(extensionDir.getAbsolutePath(), "remote");
        verifier.addCliArgument("install");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        // install the bad plugin
        verifier = newVerifier(pluginDir.getAbsolutePath(), "remote");
        verifier.addCliArgument("install");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        // build the test project
        verifier = newVerifier(projectDir.getAbsolutePath(), "remote");
        try {
            verifier.addCliArgument("package");
            verifier.execute();
            fail("The build should fail");
        } catch (VerificationException e) {
            // expected, as the build will fail due to always failing UT
        }

        verifier.verifyFilePresent("target/afterProjectsRead.txt");
        // See https://issues.apache.org/jira/browse/MNG-5641
        // verifier.verifyFilePresent( "target/afterSessionStart.txt" );
        verifier.verifyFilePresent("target/afterSessionEnd.txt");
    }
}
