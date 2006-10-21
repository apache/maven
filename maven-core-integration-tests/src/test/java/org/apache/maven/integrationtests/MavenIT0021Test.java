package org.apache.maven.integrationtests;

import junit.framework.TestCase;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.FileUtils;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;
import java.util.Properties;

public class MavenIT0021Test
    extends TestCase /*extends AbstractMavenIntegrationTest*/
{

    /**
     * Test pom-level profile inclusion (this one is activated by system
     * property).
     */
    public void testit0021()
        throws Exception
    {
        String basedir = System.getProperty( "maven.test.tmpdir", System.getProperty( "java.io.tmpdir" ) );
        File testDir = new File( basedir, getName() );
        FileUtils.deleteDirectory( testDir );
        System.out.println( "Extracting it0021 to " + testDir.getAbsolutePath() );
        ResourceExtractor.extractResourcePath( getClass(), "/it0021", testDir );
        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.deleteArtifact( "org.apache.maven", "maven-core-it-support", "1.0", "jar" );
        Properties systemProperties = new Properties();
        systemProperties.put( "includeProfile", "true" );
        verifier.setSystemProperties( systemProperties );
        verifier.executeGoal( "compile" );
        verifier.assertArtifactPresent( "org.apache.maven", "maven-core-it-support", "1.0", "jar" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
        System.out.println( "PASS" );
    }
}

