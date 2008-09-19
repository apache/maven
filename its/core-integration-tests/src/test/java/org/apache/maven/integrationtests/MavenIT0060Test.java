package org.apache.maven.integrationtests;

import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;

public class MavenIT0060Test
    extends AbstractMavenIntegrationTestCase
{

    /**
     * Test aggregation of list configuration items when using
     * 'combine.children=append' attribute.
     */
    public void testit0060()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/it0060" );
        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.executeGoal( "initialize" );
        verifier.assertFilePresent( "parent.txt" );
        verifier.assertFilePresent( "child.txt" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

    }
}

