package org.apache.maven.integrationtests;

import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;

public class MavenIT0057Test
    extends AbstractMavenIntegrationTestCase
{

    /**
     * Verify that scope == 'provided' dependencies are available to tests.
     */
    public void testit0057()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/it0057" );
        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.executeGoal( "package" );
        verifier.assertFilePresent( "target/classes/org/apache/maven/it0057/Person.class" );
        verifier.assertFilePresent( "target/test-classes/org/apache/maven/it0057/PersonTest.class" );
        verifier.assertFilePresent( "target/maven-it-it0057-1.0.jar" );
        verifier.assertFilePresent( "target/maven-it-it0057-1.0.jar!/it0057.properties" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

    }
}

