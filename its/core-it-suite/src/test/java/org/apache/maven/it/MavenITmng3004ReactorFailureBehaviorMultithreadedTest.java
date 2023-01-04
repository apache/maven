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

import org.apache.maven.shared.verifier.util.ResourceExtractor;
import org.apache.maven.shared.verifier.Verifier;
import org.apache.maven.shared.verifier.VerificationException;

import java.io.File;

import org.junit.jupiter.api.Test;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-3004">MNG-3004</a>.
 *
 * @author Dan Fabulich
 *
 */
public class MavenITmng3004ReactorFailureBehaviorMultithreadedTest
    extends AbstractMavenIntegrationTestCase
{
    public MavenITmng3004ReactorFailureBehaviorMultithreadedTest()
    {
        super( "(3.0-alpha-3,)" );
    }

    /**
     * Test fail-fast reactor behavior. Forces an exception to be thrown in
     * the first module and checks that the second &amp; third module is not built and the overall build fails, too.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitFailFastSingleThread()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-0095" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.deleteDirectory( "subproject1/target" );
        verifier.deleteDirectory( "subproject2/target" );
        verifier.deleteDirectory( "subproject3/target" );
        verifier.addCliArgument( "--fail-fast" );
        verifier.setLogFileName( "log-ff-mt1.txt" );
        verifier.addCliArgument( "-Dmaven.threads.experimental=1" );

        try
        {
            verifier.addCliArgument( "org.apache.maven.its.plugins:maven-it-plugin-touch:touch" );
            verifier.execute();
            verifier.verifyErrorFreeLog();
        }
        catch ( VerificationException e )
        {
            // expected
        }

        verifier.verifyFilePresent( "target/touch.txt" );
        verifier.verifyFileNotPresent( "subproject1/target/touch.txt" );
        verifier.verifyFileNotPresent( "subproject2/target/touch.txt" );
        verifier.verifyFileNotPresent( "subproject3/target/touch.txt" );
    }

    /**
     * Test fail-never reactor behavior. Forces an exception to be thrown in
     * the first module, but checks that the second &amp; third module is built and the overall build succeeds.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitFailNeverSingleThread()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-0095" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.deleteDirectory( "subproject1/target" );
        verifier.deleteDirectory( "subproject2/target" );
        verifier.deleteDirectory( "subproject3/target" );
        verifier.addCliArgument( "--fail-never" );
        verifier.setLogFileName( "log-fn-mt1.txt" );
        verifier.addCliArgument( "-Dmaven.threads.experimental=1" );
        verifier.addCliArgument( "org.apache.maven.its.plugins:maven-it-plugin-touch:touch" );
        verifier.execute();

        verifier.verifyFilePresent( "target/touch.txt" );
        verifier.verifyFileNotPresent( "subproject1/target/touch.txt" );
        verifier.verifyFilePresent( "subproject2/target/touch.txt" );
        verifier.verifyFilePresent( "subproject3/target/touch.txt" );
    }

    /**
     * Test fail-at-end reactor behavior. Forces an exception to be thrown in
     * the first module and checks that the second module is still built but the overall build finally fails
     * and the third module (which depends on the failed module) is skipped.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitFailAtEndSingleThread()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-0095" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.deleteDirectory( "subproject1/target" );
        verifier.deleteDirectory( "subproject2/target" );
        verifier.deleteDirectory( "subproject3/target" );
        verifier.addCliArgument( "--fail-at-end" );
        verifier.setLogFileName( "log-fae-mt1.txt" );
        verifier.addCliArgument( "-Dmaven.threads.experimental=1" );
        try
        {
            verifier.addCliArgument( "org.apache.maven.its.plugins:maven-it-plugin-touch:touch" );
            verifier.execute();
            verifier.verifyErrorFreeLog();
        }
        catch ( VerificationException e )
        {
            // expected
        }

        verifier.verifyFilePresent( "target/touch.txt" );
        verifier.verifyFileNotPresent( "subproject1/target/touch.txt" );
        verifier.verifyFilePresent( "subproject2/target/touch.txt" );
        verifier.verifyFileNotPresent( "subproject3/target/touch.txt" );
    }

    /**
     * Test fail-never reactor behavior. Forces an exception to be thrown in
     * the first module, but checks that the second &amp; third module is built and the overall build succeeds.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitFailNeverTwoThreads()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-0095" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.deleteDirectory( "subproject1/target" );
        verifier.deleteDirectory( "subproject2/target" );
        verifier.deleteDirectory( "subproject3/target" );
        verifier.addCliArgument( "--fail-never" );
        verifier.setLogFileName( "log-fn-mt2.txt" );
        verifier.addCliArgument( "-Dmaven.threads.experimental=2" );
        verifier.addCliArgument( "org.apache.maven.its.plugins:maven-it-plugin-touch:touch" );
        verifier.execute();

        verifier.verifyFilePresent( "target/touch.txt" );
        verifier.verifyFileNotPresent( "subproject1/target/touch.txt" );
        verifier.verifyFilePresent( "subproject2/target/touch.txt" );
        verifier.verifyFilePresent( "subproject3/target/touch.txt" );
    }

    /**
     * Test fail-at-end reactor behavior. Forces an exception to be thrown in
     * the first module and checks that the second module is still built but the overall build finally fails
     * and the third module (which depends on the failed module) is skipped.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitFailAtEndTwoThreads()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-0095" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.deleteDirectory( "subproject1/target" );
        verifier.deleteDirectory( "subproject2/target" );
        verifier.deleteDirectory( "subproject3/target" );
        verifier.addCliArgument( "--fail-at-end" );
        verifier.setLogFileName( "log-fae-mt2.txt" );
        verifier.addCliArgument( "-Dmaven.threads.experimental=2" );
        try
        {
            verifier.addCliArgument( "org.apache.maven.its.plugins:maven-it-plugin-touch:touch" );
            verifier.execute();
            verifier.verifyErrorFreeLog();
        }
        catch ( VerificationException e )
        {
            // expected
        }

        verifier.verifyFilePresent( "target/touch.txt" );
        verifier.verifyFileNotPresent( "subproject1/target/touch.txt" );
        verifier.verifyFilePresent( "subproject2/target/touch.txt" );
        verifier.verifyFileNotPresent( "subproject3/target/touch.txt" );
    }

    // DGF not testing fail fast with multiple real threads since that's a "best effort" attempt to halt the build
    // The whole build could have finished by the time you try to halt.

}
