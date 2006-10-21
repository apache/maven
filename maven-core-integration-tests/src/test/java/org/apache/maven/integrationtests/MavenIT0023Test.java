package org.apache.maven.integrationtests;

import junit.framework.TestCase;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.FileUtils;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;
import java.util.Properties;

public class MavenIT0023Test
    extends TestCase /*extends AbstractMavenIntegrationTest*/
{

    /**
     * Test profile inclusion from settings.xml (this one is activated by an id
     * in the activeProfiles section).
     */
    public void testit0023()
        throws Exception
    {
        String basedir = System.getProperty( "maven.test.tmpdir", System.getProperty( "java.io.tmpdir" ) );
        File testDir = new File( basedir, getName() );
        FileUtils.deleteDirectory( testDir );
        System.out.println( "Extracting it0023 to " + testDir.getAbsolutePath() );
        ResourceExtractor.extractResourcePath( getClass(), "/it0023", testDir );
        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        Properties systemProperties = new Properties();
        systemProperties.put( "org.apache.maven.user-settings", "settings.xml" );
        verifier.setSystemProperties( systemProperties );
        verifier.executeGoal( "core-it:touch" );
        verifier.assertFilePresent( "target/test.txt" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
        System.out.println( "PASS" );
    }
}

