package org.apache.maven.integrationtests;

import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;

public class MavenIT0033Test
    extends AbstractMavenIntegrationTestCase
{

    /**
     * Test an EAR generation
     */
    public void testit0033()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/it0033" );
        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.executeGoal( "package" );
        verifier.assertFilePresent( "target/maven-it-it0033-1.0.ear" );
        verifier.assertFilePresent( "target/maven-it-it0033-1.0.ear!/META-INF/application.xml" );
        verifier.assertFilePresent( "target/maven-it-it0033-1.0.ear!/META-INF/appserver-application.xml" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

    }
}

