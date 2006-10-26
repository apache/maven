package org.apache.maven.integrationtests;

import junit.framework.TestCase;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;

public class MavenIT0063Test
    extends TestCase /*extends AbstractMavenIntegrationTest*/
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
        verifier.assertFilePresent( "target/classes/org/apache/maven/it0001/Person.class" );
        verifier.assertFilePresent( "target/test-classes/org/apache/maven/it0001/PersonTest.class" );
        verifier.assertFilePresent( "target/maven-it-it0063-1.0.jar" );
        verifier.assertFilePresent( "target/maven-it-it0063-1.0.jar!/it0001.properties" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
        System.out.println( "it0063 PASS" );
    }
}

