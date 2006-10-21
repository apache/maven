package org.apache.maven.integrationtests;

import junit.framework.TestCase;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.FileUtils;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;

public class MavenIT0008Test
    extends TestCase /*extends AbstractMavenIntegrationTest*/
{

    /**
     * Simple goal decoration where a plugin binds to a phase and the plugin must
     * be downloaded from a remote repository before it can be executed. This
     * test also checks to make sure that mojo parameters are aligned to the
     * project basedir when their type is "java.io.File".
     */
    public void testit0008()
        throws Exception
    {
        String basedir = System.getProperty( "maven.test.tmpdir", System.getProperty( "java.io.tmpdir" ) );
        File testDir = new File( basedir, getName() );
        FileUtils.deleteDirectory( testDir );
        System.out.println( "Extracting it0008 to " + testDir.getAbsolutePath() );
        ResourceExtractor.extractResourcePath( getClass(), "/it0008", testDir );
        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.deleteArtifact( "org.apache.maven.plugins", "maven-core-it-plugin", "1.0", "maven-plugin" );
        verifier.executeGoal( "compile" );
        verifier.assertFilePresent( "target/touch.txt" );
        verifier.assertFilePresent( "target/test-basedir-alignment/touch.txt" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
        System.out.println( "PASS" );
    }
}

