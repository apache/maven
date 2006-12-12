package org.apache.maven.integrationtests;

import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;

public class MavenIT0000Test
    extends AbstractMavenIntegrationTestCase
{

    /**
     * The simplest of builds. We have one application class and one test
     * class. There are no resources, no source generation, no resource
     * generation and a the super model is employed to provide the build
     * information.
     */
    public void testit0000()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/it0000" );
        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.executeGoal( "package" );
        verifier.assertFilePresent( "target/classes/org/apache/maven/it0000/Person.class" );
        verifier.assertFilePresent( "target/test-classes/org/apache/maven/it0000/PersonTest.class" );
        verifier.assertFilePresent( "target/maven-it-it0000-1.0.jar" );
        verifier.assertFilePresent( "target/surefire-reports/org.apache.maven.it0000.PersonTest.txt" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

    }
}

