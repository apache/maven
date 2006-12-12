package org.apache.maven.integrationtests;

import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;

public class MavenIT0047Test
    extends AbstractMavenIntegrationTestCase
{

    /**
     * Test the use case for having a compile time dependency be transitive:
     * when you extend a class you need its dependencies at compile time.
     */
    public void testit0047()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/it0047" );
        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.executeGoal( "compile" );
        verifier.assertFilePresent( "target/classes/org/apache/maven/it0047/Person.class" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

    }
}

