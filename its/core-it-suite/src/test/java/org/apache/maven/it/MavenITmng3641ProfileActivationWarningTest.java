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

import java.io.File;
import java.util.Collections;
import java.util.List;

import org.apache.maven.it.util.ResourceExtractor;

/**
 * MNG-3641 - Profile activation warning test
 */
public class MavenITmng3641ProfileActivationWarningTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng3641ProfileActivationWarningTest()
    {
        super( "[2.0.11,2.1.0-M1),[2.1.0,)" ); // only test in 2.0.11+, 2.1.0+
    }

    public void testitMNG3641()
        throws Exception
    {
        // (0) Initialize.
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-3641" );

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );

        // Delete this artifact. Just in case.
        verifier.deleteArtifact( "org.apache.maven.its.mng3641", "parent", "1.0", "pom" );

        // (1) First run: make sure the profile is found. Must not contain a warning.
        verifier.setCliOptions( Collections.singletonList( "-P mng-3641-it-provided-profile" ) );
        verifier.setLogFileName( "log-1.txt" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        List logFile = verifier.loadFile( verifier.getBasedir(), verifier.getLogFileName(), false );
        assertFalse( logFile.contains( "[WARNING] Profile with id: 'mng-3641-it-provided-profile' has not been activated." ) );

        // (2) Second run: make sure the profile was not found and a warning was printed.
        verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.setCliOptions( Collections.singletonList( "-P mng-3641-TWlzdGVyIFQgd2FzIGhlcmUuICheX14p" ) );
        verifier.setLogFileName( "log-2.txt" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        logFile = verifier.loadFile( verifier.getBasedir(), verifier.getLogFileName(), false );
        assertTrue( logFile.contains( "[WARNING] Profile with id: 'mng-3641-TWlzdGVyIFQgd2FzIGhlcmUuICheX14p' has not been activated." ) );

        // (3) Third run: make sure the first profile is found while the other is not and a warning was printed
        // accordingly.
        verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.setCliOptions( Collections.singletonList( "-P mng-3641-it-provided-profile,mng-3641-TWlzdGVyIFQgd2FzIGhlcmUuICheX14p" ) );
        verifier.setLogFileName( "log-3.txt" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        logFile = verifier.loadFile( verifier.getBasedir(), verifier.getLogFileName(), false );
        assertFalse( logFile.contains( "[WARNING] Profile with id: 'mng-3641-it-provided-profile' has not been activated." ) );
        assertTrue( logFile.contains( "[WARNING] Profile with id: 'mng-3641-TWlzdGVyIFQgd2FzIGhlcmUuICheX14p' has not been activated." ) );
    }
}
