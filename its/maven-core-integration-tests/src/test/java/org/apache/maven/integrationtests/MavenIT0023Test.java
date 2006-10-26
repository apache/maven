package org.apache.maven.integrationtests;

import junit.framework.TestCase;
import org.apache.maven.it.Verifier;
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
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/it0023" );
        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        Properties systemProperties = new Properties();
        systemProperties.put( "org.apache.maven.user-settings", "settings.xml" );
        verifier.setSystemProperties( systemProperties );
        verifier.executeGoal( "org.apache.maven.its.plugins:maven-it-plugin-touch:touch" );
        verifier.assertFilePresent( "target/test.txt" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
        System.out.println( "it0023 PASS" );
    }
}

