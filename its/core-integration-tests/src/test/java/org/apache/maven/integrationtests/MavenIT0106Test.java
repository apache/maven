package org.apache.maven.integrationtests;

import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;

/**
 * #it0106 MNG-2318 not yet fixed
 */
public class MavenIT0106Test
    extends AbstractMavenIntegrationTestCase
{

    /**
     * When a project has modules and its parent is not preinstalled [MNG-2318]
     */
    public void testit0106()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/it0106" );
        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.executeGoal( "clean" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

    }
}

