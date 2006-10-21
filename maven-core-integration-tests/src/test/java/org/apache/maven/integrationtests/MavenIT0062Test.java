package org.apache.maven.integrationtests;

import junit.framework.TestCase;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.FileUtils;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;

public class MavenIT0062Test
    extends TestCase /*extends AbstractMavenIntegrationTest*/
{

    /**
     * Test that a deployment of a snapshot falls back to a non-snapshot repository if no snapshot repository is
     * specified.
     */
    public void testit0062()
        throws Exception
    {
        String basedir = System.getProperty( "maven.test.tmpdir", System.getProperty( "java.io.tmpdir" ) );
        File testDir = new File( basedir, getName() );
        FileUtils.deleteDirectory( testDir );
        System.out.println( "Extracting it0062 to " + testDir.getAbsolutePath() );
        ResourceExtractor.extractResourcePath( getClass(), "/it0062", testDir );
        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.deleteArtifact( "org.apache.maven", "maven-core-it0062-SNAPSHOT", "1.0", "jar" );
        verifier.executeGoal( "deploy" );
        verifier.assertFilePresent( "target/classes/org/apache/maven/it0062/Person.class" );
        verifier.assertFilePresent( "target/maven-core-it0062-1.0-SNAPSHOT.jar" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
        System.out.println( "PASS" );
    }
}

