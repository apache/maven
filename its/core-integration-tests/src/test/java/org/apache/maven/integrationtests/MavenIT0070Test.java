package org.apache.maven.integrationtests;

import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;

public class MavenIT0070Test
    extends AbstractMavenIntegrationTestCase
{

    /**
     * Test a RAR generation.
     */
    public void testit0070()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/it0070" );
        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.executeGoal( "package" );
        verifier.assertFilePresent( "target/maven-it-it0070-1.0.rar" );
        verifier.assertFilePresent( "target/maven-it-it0070-1.0.rar!/META-INF/ra.xml" );
        verifier.assertFilePresent( "target/maven-it-it0070-1.0.rar!/SomeResource.txt" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

    }
}

