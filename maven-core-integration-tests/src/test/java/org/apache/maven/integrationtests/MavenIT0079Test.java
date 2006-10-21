package org.apache.maven.integrationtests;

import junit.framework.TestCase;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.FileUtils;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;

public class MavenIT0079Test
    extends TestCase /*extends AbstractMavenIntegrationTest*/
{

    /**
     * Test that source attachments have the same build number as the main
     * artifact when deployed.
     */
    public void testit0079()
        throws Exception
    {
        String basedir = System.getProperty( "maven.test.tmpdir", System.getProperty( "java.io.tmpdir" ) );
        File testDir = new File( basedir, getName() );
        FileUtils.deleteDirectory( testDir );
        System.out.println( "Extracting it0079 to " + testDir.getAbsolutePath() );
        ResourceExtractor.extractResourcePath( getClass(), "/it0079", testDir );
        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.executeGoal( "deploy" );
        verifier.assertFilePresent(
            "target/test-repo/org/apache/maven/it/maven-core-it0079/SNAPSHOT/maven-core-it0079-*-1.jar" );
        verifier.assertFilePresent(
            "target/test-repo/org/apache/maven/it/maven-core-it0079/SNAPSHOT/maven-core-it0079-*-1-sources.jar" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
        System.out.println( "PASS" );
    }
}

