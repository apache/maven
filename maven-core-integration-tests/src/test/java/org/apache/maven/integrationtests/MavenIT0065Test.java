package org.apache.maven.integrationtests;

import junit.framework.TestCase;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.FileUtils;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;

public class MavenIT0065Test
    extends TestCase /*extends AbstractMavenIntegrationTest*/
{

    /**
     * Test that the basedir of the parent is set correctly.
     */
    public void testit0065()
        throws Exception
    {
        String basedir = System.getProperty( "maven.test.tmpdir", System.getProperty( "java.io.tmpdir" ) );
        File testDir = new File( basedir, getName() );
        FileUtils.deleteDirectory( testDir );
        System.out.println( "Extracting it0065 to " + testDir.getAbsolutePath() );
        ResourceExtractor.extractResourcePath( getClass(), "/it0065", testDir );
        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        FileUtils.deleteFile( new File( basedir, "parent-basedir" ) );
        verifier.executeGoal( "install" );
        verifier.assertFilePresent( "subproject/target/child-basedir" );
        verifier.assertFilePresent( "parent-basedir" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
        System.out.println( "PASS" );
    }
}

