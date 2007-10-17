package org.apache.maven.integrationtests;

import java.io.File;
import java.util.List;
import java.util.ArrayList;

import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

public class MavenIT0129ResourceProvidedToAPluginAsAPluginDependency
    extends AbstractMavenIntegrationTestCase
{
    public void testit0129()
        throws Exception
    {
        File testDir =
            ResourceExtractor.simpleExtractResources( getClass(), "/it0129-resourcesForAPluginProvidedAsAPluginDependency" );

        Verifier verifier;

        // Install the parent POM, extension and the plugin
        verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.deleteArtifact( "org.apache.maven.its.it0129", "it0129-plugin-runner", "1.0", "pom" );
        verifier.deleteArtifact( "org.apache.maven.its.it0129", "it0129-extension", "1.0", "jar" );
        verifier.deleteArtifact( "org.apache.maven.its.it0129", "it0129-plugin", "1.0", "jar" );
        verifier.deleteArtifact( "org.apache.maven.its.it0129", "it0129-parent", "1.0", "pom" );

        List cliOptions = new ArrayList();
        verifier.executeGoal( "install" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        //now run the test
        testDir = ResourceExtractor.simpleExtractResources( getClass(), "/it0129-resourcesForAPluginProvidedAsAPluginDependency/test-project" );
        verifier = new Verifier( testDir.getAbsolutePath() );
        cliOptions = new ArrayList();
        verifier.executeGoal( "verify" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

    }
}