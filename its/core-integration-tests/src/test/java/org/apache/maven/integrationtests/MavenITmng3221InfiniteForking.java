package org.apache.maven.integrationtests;

import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MavenITmng3221InfiniteForking
    extends AbstractMavenIntegrationTestCase
{
    public MavenITmng3221InfiniteForking()
        throws InvalidVersionSpecificationException
    {
        super( "(2.0.8,2.1-SNAPSHOT)" ); // only test in 2.0.9+
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
