package org.apache.maven.integrationtests;

import java.io.File;

/**
 * #it0104 Commenting out, not fixed until post-2.0.4, due to dependency on new plexus-container-default version.
 */
public class MavenIT0110PluginDependenciesComeFromPluginReposTest
    extends AbstractMavenIntegrationTestCase
{

    /**
     * Verify that plugin configurations are resolved correctly, particularly
     * when they contain ${project.build.directory} in the string value of a
     * Map.Entry.
     */
    public void testit0110()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/it0110-pluginDependenciesComeFromPluginRepos" );
        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.executeGoal( "clean" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

    }
}
