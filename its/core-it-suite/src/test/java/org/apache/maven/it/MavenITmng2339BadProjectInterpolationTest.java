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

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-2339">MNG-2339</a>.
 *
 *
 */
public class MavenITmng2339BadProjectInterpolationTest
    extends AbstractMavenIntegrationTestCase
{
    public MavenITmng2339BadProjectInterpolationTest()
    {
        super( "(2.0.8,)" ); // 2.0.9+
    }

    public void testitMNG2339a()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-2339/a" );

        Verifier verifier;

        verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );

        verifier.addCliOption( "-Dversion=foo" );
        verifier.executeGoal( "validate" );

        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
    }

    // test that -Dversion=1.0 is still available for interpolation.
    public void testitMNG2339b()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-2339/b" );

        Verifier verifier;

        verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );

        verifier.setLogFileName( "log-pom-specified.txt" );
        verifier.executeGoal( "initialize" );

        assertTrue( "Touchfile using ${project.version} for ${version} does not exist.",
                    new File( testDir, "target/touch-1.txt" ).exists() );

        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );

        verifier.addCliOption( "-Dversion=2" );
        verifier.setLogFileName( "log-cli-specified.txt" );
        verifier.executeGoal( "initialize" );

        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        assertTrue( "Touchfile using CLI-specified ${version} does not exist.",
                    new File( testDir, "target/touch-2.txt" ).exists() );
    }

}
