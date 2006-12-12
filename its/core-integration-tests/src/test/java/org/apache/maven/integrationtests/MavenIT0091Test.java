package org.apache.maven.integrationtests;

import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;

/**
 * # it0091 currrently fails. Not sure if there is an associated JIRA.
 */
public class MavenIT0091Test
    extends AbstractMavenIntegrationTestCase
{

    /**
     * Test that currently demonstrates that properties are not correctly
     * interpolated into other areas in the POM. This may strictly be a boolean
     * problem: I captured the problem as it was reported.
     */
    public void testit0091()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/it0091" );
        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.executeGoal( "test" );
        verifier.assertFilePresent( "target/classes/test.properties" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

    }
}

