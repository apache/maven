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

import java.io.File;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-3641">MNG-3641</a>:
 * Profile activation warning test
 */
public class MavenITmng3641ProfileActivationWarningTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng3641ProfileActivationWarningTest()
    {
        super( "[2.0.11,2.1.0-M1),[2.1.0,4.0.0-alpha-1)" ); // only test in 2.0.11+, 2.1.0+
    }

    @Test
    public void testitMNG3641()
        throws Exception
    {
        // (0) Initialize.
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-3641" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );

        // Delete this artifact. Just in case.
        verifier.deleteArtifact( "org.apache.maven.its.mng3641", "parent", "1.0", "pom" );

        // (1) make sure the profile is found. Must not contain a warning.
        verifier.addCliOption( "-P" );
        verifier.addCliOption( "mng-3641-it-provided-profile" );
        verifier.setLogFileName( "log-1.txt" );
        verifier.addCliArgument( "validate" );
        verifier.execute();
        verifier.verifyErrorFreeLog();

        List<String> logFile = verifier.loadFile( verifier.getBasedir(), verifier.getLogFileName(), false );
        assertNull( findWarning( logFile, "mng-3641-it-provided-profile" ) );

        // (2) make sure the profile was not found and a warning was printed.
        verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.addCliOption( "-P" );
        verifier.addCliOption( "mng-3641-TWlzdGVyIFQgd2FzIGhlcmUuICheX14p" );
        verifier.setLogFileName( "log-2.txt" );
        verifier.addCliArgument( "validate" );
        verifier.execute();
        verifier.verifyErrorFreeLog();

        logFile = verifier.loadFile( verifier.getBasedir(), verifier.getLogFileName(), false );
        assertNotNull( findWarning( logFile, "mng-3641-TWlzdGVyIFQgd2FzIGhlcmUuICheX14p" ) );

        // (3) make sure the first profile is found while the other is not and a warning was printed
        // accordingly.
        verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.addCliOption( "-P" );
        verifier.addCliOption( "mng-3641-it-provided-profile,mng-3641-TWlzdGVyIFQgd2FzIGhlcmUuICheX14p" );
        verifier.setLogFileName( "log-3.txt" );
        verifier.addCliArgument( "validate" );
        verifier.execute();
        verifier.verifyErrorFreeLog();

        logFile = verifier.loadFile( verifier.getBasedir(), verifier.getLogFileName(), false );
        assertNull( findWarning( logFile, "mng-3641-it-provided-profile" ) );
        assertNotNull( findWarning( logFile, "mng-3641-TWlzdGVyIFQgd2FzIGhlcmUuICheX14p" ) );

        // (4) make sure the warning is only printed when the profile is missing in all projects
        verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.addCliOption( "-P" );
        verifier.addCliOption( "mng-3641-it-provided-profile-child" );
        verifier.setLogFileName( "log-4.txt" );
        verifier.addCliArgument( "validate" );
        verifier.execute();
        verifier.verifyErrorFreeLog();

        logFile = verifier.loadFile( verifier.getBasedir(), verifier.getLogFileName(), false );
        assertNull( findWarning( logFile, "mng-3641-it-provided-profile-child" ) );

        // (5) make sure the profile is found in subproject. Must not contain a warning.
        verifier = newVerifier( new File( testDir, "child1" ).getAbsolutePath() );
        verifier.addCliOption( "-P" );
        verifier.addCliOption( "mng-3641-it-provided-profile-child" );
        verifier.setLogFileName( "log-5.txt" );
        verifier.addCliArgument( "validate" );
        verifier.execute();
        verifier.verifyErrorFreeLog();

        logFile = verifier.loadFile( verifier.getBasedir(), verifier.getLogFileName(), false );
        assertNull( findWarning( logFile, "mng-3641-it-provided-profile-child" ) );

        // (6) make sure the profile is found from parent in subproject. Must not contain a warning.
        verifier = newVerifier( new File( testDir, "child1" ).getAbsolutePath() );
        verifier.addCliOption( "-P" );
        verifier.addCliOption( "mng-3641-it-provided-profile" );
        verifier.setLogFileName( "log-6.txt" );
        verifier.addCliArgument( "validate" );
        verifier.execute();
        verifier.verifyErrorFreeLog();

        logFile = verifier.loadFile( verifier.getBasedir(), verifier.getLogFileName(), false );
        assertNull( findWarning( logFile, "mng-3641-it-provided-profile" ) );
    }

    private String findWarning( List<String> logLines, String profileId )
    {
        Pattern pattern = Pattern.compile( "(?i).*profile\\s.*\\Q" + profileId + "\\E.*\\snot\\s.*activated.*" );

        for ( String logLine : logLines )
        {
            if ( pattern.matcher( logLine ).matches() )
            {
                return logLine;
            }
        }

        return null;
    }

}
