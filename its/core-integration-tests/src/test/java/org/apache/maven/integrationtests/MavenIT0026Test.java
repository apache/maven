package org.apache.maven.integrationtests;

import junit.framework.TestCase;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;
import java.util.Properties;

public class MavenIT0026Test
    extends AbstractMavenIntegrationTestCase
{

    /**
     * Test merging of global- and user-level settings.xml files.
     */
    public void testit0026()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/it0026" );
        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        Properties systemProperties = new Properties();
        systemProperties.put( "org.apache.maven.user-settings", "user-settings.xml" );
        systemProperties.put( "org.apache.maven.global-settings", "global-settings.xml" );
        verifier.setSystemProperties( systemProperties );
        verifier.executeGoal( "org.apache.maven.its.plugins:maven-it-plugin-touch:touch" );
        verifier.assertFilePresent( "target/test.txt" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
        System.out.println( "it0026 PASS" );
    }
}

