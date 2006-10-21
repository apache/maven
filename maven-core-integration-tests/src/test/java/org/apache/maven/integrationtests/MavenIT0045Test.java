package org.apache.maven.integrationtests;

import junit.framework.TestCase;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.FileUtils;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MavenIT0045Test
    extends TestCase /*extends AbstractMavenIntegrationTest*/
{

    /**
     * Test non-reactor behavior when plugin declares "@requiresProject false"
     */
    public void testit0045()
        throws Exception
    {
        String basedir = System.getProperty( "maven.test.tmpdir", System.getProperty( "java.io.tmpdir" ) );
        File testDir = new File( basedir, getName() );
        FileUtils.deleteDirectory( testDir );
        System.out.println( "Extracting it0045 to " + testDir.getAbsolutePath() );
        ResourceExtractor.extractResourcePath( getClass(), "/it0045", testDir );
        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.deleteArtifact( "org.apache.maven.plugins", "maven-core-it-plugin", "1.0", "maven-plugin" );
        List cliOptions = new ArrayList();
        cliOptions.add( "--no-plugin-registry" );
        verifier.setCliOptions( cliOptions );
        verifier.executeGoal( "core-it:light-touch" );
        verifier.assertFilePresent( "target/touch.txt" );
        verifier.assertFileNotPresent( "subproject/target/touch.txt" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
        System.out.println( "PASS" );
    }
}

