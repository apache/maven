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

public class MavenITmng3221InfiniteForkingTest
    extends AbstractMavenIntegrationTestCase
{
    public MavenITmng3221InfiniteForkingTest()
    {
        super( "(2.0.8,2.99.99)" );
    }

    public void testitMNG3221a()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-3221" );
        File logBackupDir = testDir.getAbsoluteFile().getParentFile();

        File reportDir = new File( testDir, "report" );
        File projectDir = new File( testDir, "user" );

        Verifier verifier = null;

        try
        {
            verifier = new Verifier( reportDir.getAbsolutePath() );

            verifier.deleteArtifact( "tests", "maven-forking-report-plugin", "1", "jar" );

            verifier.executeGoal( "install" );
            verifier.verifyErrorFreeLog();
            verifier.resetStreams();

            verifier = new Verifier( projectDir.getAbsolutePath() );

            List cliOptions = new ArrayList();
            cliOptions.add( "-Psite" );
            verifier.setCliOptions( cliOptions );

            verifier.executeGoal( "site" );
            verifier.verifyErrorFreeLog();
        }
        finally
        {
            if ( verifier != null )
            {
                verifier.resetStreams();
            }

            File logFile = new File( projectDir, "log.txt" );
            File logFileBackup = new File( logBackupDir, "mng-3221-a-log.txt" );

            logFile.renameTo( logFileBackup );
        }
   }

    public void testitMNG3221b()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-3221" );
        File logBackupDir = testDir.getAbsoluteFile().getParentFile();

        File pluginDir = new File( testDir, "plugin" );
        File projectDir = new File( testDir, "user" );

        Verifier verifier = null;

        try
        {
            verifier = new Verifier( pluginDir.getAbsolutePath() );

            verifier.deleteArtifact( "tests", "maven-forking-test-plugin", "1", "jar" );

            verifier.executeGoal( "install" );
            verifier.verifyErrorFreeLog();
            verifier.resetStreams();

            verifier = new Verifier( projectDir.getAbsolutePath() );

            List cliOptions = new ArrayList();
            cliOptions.add( "-Pplugin" );
            verifier.setCliOptions( cliOptions );

            verifier.executeGoal( "package" );
            verifier.verifyErrorFreeLog();
        }
        finally
        {
            if ( verifier != null )
            {
                verifier.resetStreams();
            }

            File logFile = new File( projectDir, "log.txt" );
            File logFileBackup = new File( logBackupDir, "mng-3221-b-log.txt" );

            logFile.renameTo( logFileBackup );
        }
    }
}
