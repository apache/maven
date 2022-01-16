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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-3221">MNG-3221</a>.
 *
 *
 */
public class MavenITmng3221InfiniteForkingTest
    extends AbstractMavenIntegrationTestCase
{
    public MavenITmng3221InfiniteForkingTest()
    {
        super( "(2.0.8,3.0-alpha-1)" ); // irrelevant in 3.0+
    }

    public void testitMNG3221a()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-3221" );

        File reportDir = new File( testDir, "report" );
        File projectDir = new File( testDir, "user" );

        Verifier verifier = null;

        try
        {
            verifier = newVerifier( reportDir.getAbsolutePath(), "remote" );

            verifier.deleteArtifact( "org.apache.maven.its.mng3221", "maven-forking-report-plugin", "1", "jar" );

            verifier.setLogFileName( "mng-3221-a-log.txt" );
            verifier.executeGoal( "install" );
            verifier.verifyErrorFreeLog();
            verifier.resetStreams();

            verifier = newVerifier( projectDir.getAbsolutePath(), "remote" );

            verifier.addCliOption( "-Psite" );

            verifier.setLogFileName( "mng-3221-a-log.txt" );
            verifier.executeGoal( "site" );
            verifier.verifyErrorFreeLog();
        }
        finally
        {
            if ( verifier != null )
            {
                verifier.resetStreams();
            }
        }
   }

    public void testitMNG3221b()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-3221" );

        File pluginDir = new File( testDir, "plugin" );
        File projectDir = new File( testDir, "user" );

        Verifier verifier = null;

        try
        {
            verifier = newVerifier( pluginDir.getAbsolutePath(), "remote" );

            verifier.deleteArtifact( "org.apache.maven.its.mng3221", "maven-forking-test-plugin", "1", "jar" );

            verifier.setLogFileName( "mng-3221-b-log.txt" );
            verifier.executeGoal( "install" );
            verifier.verifyErrorFreeLog();
            verifier.resetStreams();

            verifier = newVerifier( projectDir.getAbsolutePath(), "remote" );

            verifier.addCliOption( "-Pplugin" );

            verifier.setLogFileName( "mng-3221-b-log.txt" );
            verifier.executeGoal( "package" );
            verifier.verifyErrorFreeLog();
        }
        finally
        {
            if ( verifier != null )
            {
                verifier.resetStreams();
            }
        }
    }
}
