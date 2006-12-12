package org.apache.maven.integrationtests;

import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;

public class MavenIT0054Test
    extends AbstractMavenIntegrationTestCase
{

    /**
     * Test resource filtering.
     */
    public void testit0054()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/it0054" );
        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.executeGoal( "package" );
        verifier.assertFilePresent( "target/classes/org/apache/maven/it0054/Person.class" );
        verifier.assertFilePresent( "target/test-classes/org/apache/maven/it0054/PersonTest.class" );
        verifier.assertFilePresent( "target/maven-it-it0054-1.0.jar" );
        verifier.assertFilePresent( "target/maven-it-it0054-1.0.jar!/it0054.properties" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

    }
}

