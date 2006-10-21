package org.apache.maven.integrationtests;

import junit.framework.TestCase;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.FileUtils;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class MavenIT0027Test
    extends TestCase /*extends AbstractMavenIntegrationTest*/
{

    /**
     * Test @execute with a custom lifecycle, including configuration
     */
    public void testit0027()
        throws Exception
    {
        String basedir = System.getProperty( "maven.test.tmpdir", System.getProperty( "java.io.tmpdir" ) );
        File testDir = new File( basedir, getName() );
        FileUtils.deleteDirectory( testDir );
        System.out.println( "Extracting it0027 to " + testDir.getAbsolutePath() );
        ResourceExtractor.extractResourcePath( getClass(), "/it0027", testDir );
        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        List goals = Arrays.asList( new String[]{"core-it:fork", "core-it:fork-goal"} );
        verifier.executeGoals( goals );
        verifier.assertFilePresent( "target/forked/touch.txt" );
        verifier.assertFilePresent( "target/forked2/touch.txt" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
        System.out.println( "PASS" );
    }
}

