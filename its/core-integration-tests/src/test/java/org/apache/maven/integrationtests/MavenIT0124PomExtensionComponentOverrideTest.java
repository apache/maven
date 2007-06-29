package org.apache.maven.integrationtests;

import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;

public class MavenIT0124PomExtensionComponentOverrideTest
    extends AbstractMavenIntegrationTestCase
{
    /**
     * Test that ensures the POM extensions can override default component implementations.
     * 
     * @throws Exception
     * @see <a href="http://jira.codehaus.org/browse/MNG-2771">MNG-2771</a>
     */
    public void testit0124() throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/it0124-pomExtensionComponentOverride/extension" );
        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.executeGoal( "install" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        testDir = ResourceExtractor.simpleExtractResources( getClass(), "/it0124-pomExtensionComponentOverride/plugin" );
        verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.executeGoal( "install" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        testDir = ResourceExtractor.simpleExtractResources( getClass(), "/it0124-pomExtensionComponentOverride/project" );
        verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.executeGoal( "verify" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
    }
}
