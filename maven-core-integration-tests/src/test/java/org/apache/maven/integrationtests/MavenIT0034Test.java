package org.apache.maven.integrationtests;

import junit.framework.TestCase;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.FileUtils;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;

public class MavenIT0034Test
    extends TestCase /*extends AbstractMavenIntegrationTest*/
{

    /**
     * Test version range junit [3.7,) resolves to 3.8.1
     */
    public void testit0034()
        throws Exception
    {
        String basedir = System.getProperty( "maven.test.tmpdir", System.getProperty( "java.io.tmpdir" ) );
        File testDir = new File( basedir, getName() );
        FileUtils.deleteDirectory( testDir );
        System.out.println( "Extracting it0034 to " + testDir.getAbsolutePath() );
        ResourceExtractor.extractResourcePath( getClass(), "/it0034", testDir );
        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.deleteArtifact( "org.apache.maven", "maven-core-it-support", "1.4", "jar" );
        verifier.deleteArtifact( "junit", "junit", "3.8", "jar" );
        verifier.executeGoal( "package" );
        verifier.assertArtifactPresent( "junit", "junit", "3.8", "jar" );
        verifier.assertArtifactPresent( "org.apache.maven", "maven-core-it-support", "1.4", "jar" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
        System.out.println( "PASS" );
    }
}

