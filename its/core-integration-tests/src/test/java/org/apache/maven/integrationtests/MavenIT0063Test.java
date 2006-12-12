package org.apache.maven.integrationtests;

import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;

public class MavenIT0063Test
    extends AbstractMavenIntegrationTestCase
{

    /**
     * Test the use of a system scoped dependency to tools.jar.
     */
    public void testit0063()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/it0063" );
        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.executeGoal( "package" );
        verifier.assertFilePresent( "target/classes/org/apache/maven/it0063/Person.class" );
        verifier.assertFilePresent( "target/test-classes/org/apache/maven/it0063/PersonTest.class" );
        verifier.assertFilePresent( "target/maven-it-it0063-1.0.jar" );
        verifier.assertFilePresent( "target/maven-it-it0063-1.0.jar!/it0063.properties" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

    }
}

