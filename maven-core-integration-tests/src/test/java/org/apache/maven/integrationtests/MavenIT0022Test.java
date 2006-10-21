package org.apache.maven.integrationtests;

import junit.framework.TestCase;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.FileUtils;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;
import java.util.Properties;

public class MavenIT0022Test
    extends TestCase /*extends AbstractMavenIntegrationTest*/
{

    /**
     * Test profile inclusion from profiles.xml (this one is activated by system
     * property).
     */
    public void testit0022()
        throws Exception
    {
        String basedir = System.getProperty( "maven.test.tmpdir", System.getProperty( "java.io.tmpdir" ) );
        File testDir = new File( basedir, getName() );
        FileUtils.deleteDirectory( testDir );
        System.out.println( "Extracting it0022 to " + testDir.getAbsolutePath() );
        ResourceExtractor.extractResourcePath( getClass(), "/it0022", testDir );
        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        Properties systemProperties = new Properties();
        systemProperties.put( "includeProfile", "true" );
        verifier.setSystemProperties( systemProperties );
        verifier.executeGoal( "core-it:touch" );
        verifier.assertFilePresent( "target/test.txt" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
        System.out.println( "PASS" );
    }
}

