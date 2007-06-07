package org.apache.maven.integrationtests;

import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;

public class MavenIT0122ReactorDependencyResolutionTest
    extends AbstractMavenIntegrationTestCase
{
    /**
     * Test that reactor projects are included in dependency resolution.
     * 
     * @throws Exception
     */
    public void testit0122() throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/it0122-reactorDependencyResolution/plugin" );
        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.executeGoal( "install" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        testDir = ResourceExtractor.simpleExtractResources( getClass(), "/it0122-reactorDependencyResolution/project" );
        verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.executeGoal( "org.apache.maven.its.it0122:maven-it-it0122-plugin:1.0:test" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
    }
}
