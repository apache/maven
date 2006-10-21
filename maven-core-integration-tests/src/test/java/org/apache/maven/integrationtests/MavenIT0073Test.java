package org.apache.maven.integrationtests;

import junit.framework.TestCase;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.FileUtils;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class MavenIT0073Test
    extends TestCase /*extends AbstractMavenIntegrationTest*/
{

    /**
     * Tests context passing between mojos in the same plugin.
     */
    public void testit0073()
        throws Exception
    {
        String basedir = System.getProperty( "maven.test.tmpdir", System.getProperty( "java.io.tmpdir" ) );
        File testDir = new File( basedir, getName() );
        FileUtils.deleteDirectory( testDir );
        System.out.println( "Extracting it0073 to " + testDir.getAbsolutePath() );
        ResourceExtractor.extractResourcePath( getClass(), "/it0073", testDir );
        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.deleteArtifact( "org.apache.maven.plugins", "maven-core-it-plugin", "1.0", "maven-plugin" );
        List goals = Arrays.asList( new String[]{"core-it:throw", "core-it:catch"} );
        verifier.executeGoals( goals );
        verifier.assertFilePresent( "target/thrown-value" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
        System.out.println( "PASS" );
    }
}

