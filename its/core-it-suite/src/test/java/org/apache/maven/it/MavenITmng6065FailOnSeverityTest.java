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

/**
 * An integration test to verify that builds fail when logs occur
 * above or equal to the --fail-on-severity cli property.
 *
 * <a href="https://issues.apache.org/jira/browse/MNG-6065">MNG-6065</a>.
 *
 */
public class MavenITmng6065FailOnSeverityTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng6065FailOnSeverityTest()
    {
        super( "[4.0.0-alpha-1,)" );
    }

    public void testItShouldFailOnWarnLogMessages()
            throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-6065-fail-on-severity" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath(), false );
        verifier.setForkJvm( true );
        verifier.addCliOption( "--fail-on-severity" );
        verifier.addCliOption( "WARN" );

        boolean failed = false;

        try
        {
            verifier.executeGoal( "compile" );
        }
        catch ( VerificationException e )
        {
            failed = true;
            verifier.verifyTextInLog( "Enabled to break the build on log level WARN." );
            verifier.verifyTextInLog( "Build failed due to log statements with a higher severity than allowed." );
        }

        assertTrue( "Build should have failed", failed );
    }

    public void testItShouldSucceedOnWarnLogMessagesWhenFailLevelIsError()
            throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-6065-fail-on-severity" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath(), false );
        verifier.setForkJvm( true );
        verifier.addCliOption( "--fail-on-severity" );
        verifier.addCliOption( "ERROR" );

        verifier.executeGoal( "compile" );

        verifier.verifyTextInLog( "Enabled to break the build on log level ERROR." );
    }
}
