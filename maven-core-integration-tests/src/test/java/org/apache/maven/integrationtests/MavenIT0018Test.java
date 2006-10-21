package org.apache.maven.integrationtests;

import junit.framework.TestCase;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.FileUtils;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;

public class MavenIT0018Test
    extends TestCase /*extends AbstractMavenIntegrationTest*/
{

    /**
     * Ensure that managed dependencies for dependency POMs are calculated
     * correctly when resolved. Removes commons-logging-1.0.3 and checks it is
     * redownloaded.
     */
    public void testit0018()
        throws Exception
    {
        String basedir = System.getProperty( "maven.test.tmpdir", System.getProperty( "java.io.tmpdir" ) );
        File testDir = new File( basedir, getName() );
        FileUtils.deleteDirectory( testDir );
        System.out.println( "Extracting it0018 to " + testDir.getAbsolutePath() );
        ResourceExtractor.extractResourcePath( getClass(), "/it0018", testDir );
        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.deleteArtifact( "commons-logging", "commons-logging", "1.0.3", "jar" );
        verifier.executeGoal( "package" );
// TODO: I would like to build some small core-it artifacts for this purpose instead
        verifier.assertArtifactPresent( "commons-logging", "commons-logging", "1.0.3", "jar" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
        System.out.println( "PASS" );
    }
}

