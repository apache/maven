package org.apache.maven.it;

import java.io.File;

import org.apache.maven.it.util.ResourceExtractor;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-5889">MNG-5889</a>:
 * check that extensions in <code>.mvn/</code> are found when Maven is run with <code>-f path/to/pom.xml</code>.
 * Reuses MNG-5771 core extensions IT to run the test in new conditions.
 * 
 * @see MavenITmng5771CoreExtensionsTest
 */
public class MavenITmng5889CoreExtensionsTest
    extends AbstractMavenIntegrationTestCase
{
    public MavenITmng5889CoreExtensionsTest()
    {
        super( "[3.5.0,)" );
    }

    /**
     * check that <code>.mvn/</code> is found when current dir does not contain <code>pom.xml</code>
     * but path to POM set by <code>--file path/to/pom.xml</code>
     */
    public void testMvnFileLongOption()
        throws Exception
    {
        runCoreExtensionWithOption( "--file" );
    }

    /**
     * check that <code>.mvn/</code> is found when current dir does not contain <code>pom.xml</code>
     * but path to POM set by <code>-f path/to/pom.xml</code>
     */
    public void testMvnFileShortOption()
        throws Exception
    {
        runCoreExtensionWithOption( "-f" );
    }

    private void runCoreExtensionWithOption( String option )
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-5771-core-extensions" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() ); // not client directory
        verifier.filterFile( "settings-template.xml", "settings.xml", "UTF-8", verifier.newDefaultFilterProperties() );

        verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.deleteDirectory( "client/target" );
        verifier.deleteArtifacts( "org.apache.maven.its.it-core-extensions" );
        verifier.getCliOptions().add( "-s" );
        verifier.getCliOptions().add( new File( testDir, "settings.xml" ).getAbsolutePath() );
        verifier.getCliOptions().add( option ); // -f/--file client/pom.xml
        verifier.getCliOptions().add( new File( testDir, "client/pom.xml" ).getAbsolutePath() );
        verifier.setForkJvm( true ); // force forked JVM since we need the shell script to detect .mvn/ location
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
    }
}
