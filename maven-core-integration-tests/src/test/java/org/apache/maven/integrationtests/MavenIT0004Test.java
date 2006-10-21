package org.apache.maven.integrationtests;

import junit.framework.TestCase;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.FileUtils;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;

public class MavenIT0004Test
    extends TestCase /*extends AbstractMavenIntegrationTest*/
{

    /**
     * The simplest of pom installation. We have a pom and we install it in
     * local repository.
     */
    public void testit0004()
        throws Exception
    {
        String basedir = System.getProperty( "maven.test.tmpdir", System.getProperty( "java.io.tmpdir" ) );
        File testDir = new File( basedir, getName() );
        FileUtils.deleteDirectory( testDir );
        System.out.println( "Extracting it0004 to " + testDir.getAbsolutePath() );
        ResourceExtractor.extractResourcePath( getClass(), "/it0004", testDir );
        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.deleteArtifact( "org.apache.maven", "maven-core-it0004", "1.0", "pom" );
        verifier.executeGoal( "install:install" );
        verifier.assertArtifactPresent( "org.apache.maven", "maven-core-it0004", "1.0", "pom" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
        System.out.println( "PASS" );
    }
}

