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

import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MavenIT0069Test
    extends AbstractMavenIntegrationTestCase
{

    /**
     * Test offline mode.
     */
    public void testit0069()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/it0069" );

        {
            // phase 1: run build in online mode to fill local repo
            Verifier verifier = new Verifier( testDir.getAbsolutePath() );
            verifier.deleteArtifacts( "org.apache.maven.its.it0069" );
            verifier.executeGoal( "org.apache.maven.its.plugins:maven-it-plugin-dependency-resolution:2.1-SNAPSHOT:compile" );
            verifier.assertFilePresent( "target/compile.txt" );
            verifier.verifyErrorFreeLog();
            verifier.resetStreams();
            new File( testDir, "log.txt").renameTo( new File( testDir, "log1.txt" ) );
        }

        {
            // phase 2: run build in offline mode to check it still passes (after deleting test repo, to be sure)
            Verifier verifier = new Verifier( testDir.getAbsolutePath() );
            verifier.deleteDirectory( "repo" );
            List cliOptions = new ArrayList();
            cliOptions.add( "-o" );
            verifier.setCliOptions( cliOptions );
            verifier.executeGoal( "org.apache.maven.its.plugins:maven-it-plugin-dependency-resolution:2.1-SNAPSHOT:compile" );
            verifier.assertFilePresent( "target/compile.txt" );
            verifier.verifyErrorFreeLog();
            verifier.resetStreams();
            new File( testDir, "log.txt").renameTo( new File( testDir, "log2.txt" ) );
        }

        {
            // phase 3: delete test artifact and run build in offline mode to check it fails now
            Verifier verifier = new Verifier( testDir.getAbsolutePath() );
            verifier.deleteArtifacts( "org.apache.maven.its.it0069" );
            List cliOptions = new ArrayList();
            cliOptions.add( "-o" );
            verifier.setCliOptions( cliOptions );
            try
            {
                verifier.executeGoal( "org.apache.maven.its.plugins:maven-it-plugin-dependency-resolution:2.1-SNAPSHOT:compile" );
                fail( "Build did not fail!" );
            }
            catch( VerificationException e )
            {
                // expected, should fail
            }
            verifier.resetStreams();
            new File( testDir, "log.txt").renameTo( new File( testDir, "log3.txt" ) );
        }
    }

}
