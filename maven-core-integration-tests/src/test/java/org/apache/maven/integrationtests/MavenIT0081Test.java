package org.apache.maven.integrationtests;

import junit.framework.TestCase;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.FileUtils;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;

public class MavenIT0081Test
    extends TestCase /*extends AbstractMavenIntegrationTest*/
{

    /**
     * Test per-plugin dependencies.
     */
    public void testit0081()
        throws Exception
    {
        String basedir = System.getProperty( "maven.test.tmpdir", System.getProperty( "java.io.tmpdir" ) );
        File testDir = new File( basedir, getName() );
        FileUtils.deleteDirectory( testDir );
        System.out.println( "Extracting it0081 to " + testDir.getAbsolutePath() );
        ResourceExtractor.extractResourcePath( getClass(), "/it0081", testDir );
        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.executeGoal( "install" );
        verifier.assertFilePresent( "test-component-c/target/org.apache.maven.wagon.providers.ftp.FtpWagon" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
        System.out.println( "PASS" );
    }
}

