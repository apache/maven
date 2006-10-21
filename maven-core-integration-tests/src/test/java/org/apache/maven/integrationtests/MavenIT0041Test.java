package org.apache.maven.integrationtests;

import junit.framework.TestCase;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.FileUtils;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;

public class MavenIT0041Test
    extends TestCase /*extends AbstractMavenIntegrationTest*/
{

    /**
     * Test the use of a new type from a plugin
     */
    public void testit0041()
        throws Exception
    {
        String basedir = System.getProperty( "maven.test.tmpdir", System.getProperty( "java.io.tmpdir" ) );
        File testDir = new File( basedir, getName() );
        FileUtils.deleteDirectory( testDir );
        System.out.println( "Extracting it0041 to " + testDir.getAbsolutePath() );
        ResourceExtractor.extractResourcePath( getClass(), "/it0041", testDir );
        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.deleteArtifact( "org.apache.maven", "maven-core-it-support", "1.2", "coreit-artifact" );
        verifier.executeGoal( "package" );
        verifier.assertFilePresent( "target/maven-core-it0041-1.0-SNAPSHOT.jar" );
        verifier.assertArtifactPresent( "org.apache.maven", "maven-core-it-support", "1.2", "coreit-artifact" );
        verifier.assertArtifactPresent( "org.apache.maven", "maven-core-it-support", "1.2", "pom" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
        System.out.println( "PASS" );
    }
}

