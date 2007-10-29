package org.apache.maven.integrationtests;

import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MavenIT0067Test
    extends AbstractMavenIntegrationTestCase
{

    /**
     * Test activation of a profile from the command line.
     */
    public void testit0067()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/it0067" );
        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.deleteArtifact( "org.apache.maven", "maven-core-it-support", "1.0", "jar" );
        List cliOptions = new ArrayList();
        cliOptions.add( "-P test-profile" );
        verifier.setCliOptions( cliOptions );
        verifier.executeGoal( "compile" );
        verifier.assertFilePresent( "target/classes/org/apache/maven/it0067/Person.class" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

    }
}

