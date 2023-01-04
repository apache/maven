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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-95">MNG-95</a>.
 *
 * @author John Casey
 *
 */
public class MavenITmng0095ReactorFailureBehaviorTest
    extends AbstractMavenIntegrationTestCase
{
    public MavenITmng0095ReactorFailureBehaviorTest()
    {
        super( ALL_MAVEN_VERSIONS );
    }

    /**
     * Test fail-fast reactor behavior. Forces an exception to be thrown in
     * the first module and checks that the second &amp; third module is not built and the overall build fails, too.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitFailFast()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-0095" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.deleteDirectory( "subproject1/target" );
        verifier.deleteDirectory( "subproject2/target" );
        verifier.deleteDirectory( "subproject3/target" );
        verifier.addCliOption( "--fail-fast" );
        verifier.setLogFileName( "log-ff.txt" );
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
    public void testitFailNever()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-0095" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.deleteDirectory( "subproject1/target" );
        verifier.deleteDirectory( "subproject2/target" );
        verifier.deleteDirectory( "subproject3/target" );
        verifier.addCliOption( "--fail-never" );
        verifier.setLogFileName( "log-fn.txt" );
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
    public void testitFailAtEnd()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-0095" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.deleteDirectory( "subproject1/target" );
        verifier.deleteDirectory( "subproject2/target" );
        verifier.deleteDirectory( "subproject3/target" );
        verifier.addCliOption( "--fail-at-end" );
        verifier.setLogFileName( "log-fae.txt" );
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

}
