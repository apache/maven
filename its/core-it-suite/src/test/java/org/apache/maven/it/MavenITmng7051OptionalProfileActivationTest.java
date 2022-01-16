package org.apache.maven.it;

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

import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;

public class MavenITmng7051OptionalProfileActivationTest
        extends AbstractMavenIntegrationTestCase
{
    private static final String PROJECT_PATH = "/mng-7051-optional-profile-activation";

    public MavenITmng7051OptionalProfileActivationTest()
    {
        super( "[4.0.0-alpha-1,)" );
    }

    /**
     * This test verifies that activating a non-existing profile breaks the build.
     *
     * @throws Exception in case of failure
     */
    public void testActivatingNonExistingProfileBreaks() throws Exception
    {
        final File projectDir = ResourceExtractor.simpleExtractResources( getClass(), PROJECT_PATH );
        final Verifier verifier = newVerifier( projectDir.getAbsolutePath() );

        verifier.addCliOption( "-P" );
        verifier.addCliOption( "non-existing-profile" );
        verifier.setLogFileName( "test-breaking.txt" );

        try
        {
            verifier.executeGoal( "validate" );
            fail( "Activated a non-existing profile without ? prefix should break the build, but it didn't." );
        }
        catch ( VerificationException ve )
        {
            // Inspect the reason why the build broke.
            verifier.verifyTextInLog( "[ERROR] The requested profiles [non-existing-profile] could not be activated or deactivated because they do not exist." );
        }
    }

    /**
     * This test verifies that activating a non-existing profile does not break the build when it is prefixed with <strong>?</strong>.
     *
     * @throws Exception in case of failure
     */
    public void testActivatingNonExistingProfileWithQuestionMarkDoesNotBreak() throws Exception
    {
        final File projectDir = ResourceExtractor.simpleExtractResources( getClass(), PROJECT_PATH );
        final Verifier verifier = newVerifier( projectDir.getAbsolutePath() );

        verifier.addCliOption( "-P" );
        verifier.addCliOption( "?non-existing-profile" );
        verifier.setLogFileName( "test-non-breaking.txt" );

        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.verifyTextInLog( "[INFO] The requested optional profiles [non-existing-profile] could not be activated or deactivated because they do not exist." );
    }

    /**
     * This test verifies that activating both an existing and a non-existing profile does not break the build when it the latter is prefixed with <strong>?</strong>.
     *
     * @throws Exception in case of failure
     */
    public void testActivatingExistingAndNonExistingProfiles() throws Exception
    {
        final File projectDir = ResourceExtractor.simpleExtractResources( getClass(), PROJECT_PATH );
        final Verifier verifier = newVerifier( projectDir.getAbsolutePath() );

        verifier.addCliOption( "-P" );
        verifier.addCliOption( "?non-existing-profile,existing" );
        verifier.setLogFileName( "test-non-breaking-mixed.txt" );

        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.verifyTextInLog( "[INFO] The requested optional profiles [non-existing-profile] could not be activated or deactivated because they do not exist." );
    }

    /**
     * This test verifies that deactivating a non-existing profile does not break the build when it is prefixed with <strong>?</strong>.
     *
     * @throws Exception in case of failure
     */
    public void testDeactivatingNonExistingProfileWithQuestionMarkDoesNotBreak() throws Exception
    {
        final File projectDir = ResourceExtractor.simpleExtractResources( getClass(), PROJECT_PATH );
        final Verifier verifier = newVerifier( projectDir.getAbsolutePath() );

        verifier.addCliOption( "-P" );
        verifier.addCliOption( "!?non-existing-profile" );
        verifier.setLogFileName( "test-deactivating-non-breaking.txt" );

        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.verifyTextInLog( "[INFO] The requested optional profiles [non-existing-profile] could not be activated or deactivated because they do not exist." );
    }

    /**
     * This test verifies that deactivating both an existing and a non-existing profile does not break the build when it the latter is prefixed with <strong>?</strong>.
     *
     * @throws Exception in case of failure
     */
    public void testDeactivatingExistingAndNonExistingProfiles() throws Exception
    {
        final File projectDir = ResourceExtractor.simpleExtractResources( getClass(), PROJECT_PATH );
        final Verifier verifier = newVerifier( projectDir.getAbsolutePath() );

        verifier.addCliOption( "-P" );
        verifier.addCliOption( "!?non-existing-profile,!existing" );
        verifier.setLogFileName( "test-deactivating-non-breaking-mixed.txt" );

        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.verifyTextInLog( "[INFO] The requested optional profiles [non-existing-profile] could not be activated or deactivated because they do not exist." );
    }
}
