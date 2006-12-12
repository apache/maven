package org.apache.maven.integrationtests;

import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;

public class MavenIT0097Test
    extends AbstractMavenIntegrationTestCase
{

    /**
     * Test that the implied relative path for the parent POM works, even two
     * levels deep.
     */
    public void testit0097()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/it0097" );
        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.executeGoal( "package" );
        verifier.assertFilePresent( "project/project-level2/project-level3/target/it0097.txt" );
        verifier.assertFilePresent( "project/project-sibling-level2/target/it0097.txt" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

    }
}

