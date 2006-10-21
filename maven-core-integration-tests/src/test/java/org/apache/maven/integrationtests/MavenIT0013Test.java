package org.apache.maven.integrationtests;

import junit.framework.TestCase;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.FileUtils;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class MavenIT0013Test
    extends TestCase /*extends AbstractMavenIntegrationTest*/
{

    /**
     * Test plugin-plugin, which tests maven-plugin-tools-api and
     * maven-plugin-tools-java. This will generate a plugin descriptor from
     * java-based mojo sources, install the plugin, and then use it.
     */
    public void testit0013()
        throws Exception
    {
        String basedir = System.getProperty( "maven.test.tmpdir", System.getProperty( "java.io.tmpdir" ) );
        File testDir = new File( basedir, getName() );
        FileUtils.deleteDirectory( testDir );
        System.out.println( "Extracting it0013 to " + testDir.getAbsolutePath() );
        ResourceExtractor.extractResourcePath( getClass(), "/it0013", testDir );
        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.deleteArtifact( "org.apache.maven.plugins", "maven-it0013-plugin", "1.0-SNAPSHOT", "maven-plugin" );
        List goals = Arrays.asList( new String[]{"install", "it0013:it0013"} );
        verifier.executeGoals( goals );
        verifier.assertFilePresent( "target/maven-it0013-plugin-1.0-SNAPSHOT.jar" );
        verifier.assertFilePresent( "target/it0013-verify" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
        System.out.println( "PASS" );
    }
}

