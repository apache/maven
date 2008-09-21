package org.apache.maven.integrationtests;

import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MavenIT0051Test
    extends AbstractMavenIntegrationTestCase
{
    public MavenIT0051Test()                                                                                                                          
    {                                                                                                                                                 
        super( "[,2.99.99)" );
    }    

    /**
     * Test source attachment when -DperformRelease=true is specified.
     */
    public void testit0051()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/it0051" );
        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        List cliOptions = new ArrayList();
        cliOptions.add( "--no-plugin-registry -DperformRelease=true" );
        verifier.setCliOptions( cliOptions );
        verifier.executeGoal( "package" );
        verifier.assertFilePresent( "target/source-jar.txt" );
        verifier.assertFilePresent( "target/javadoc-jar.txt" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

    }
}

