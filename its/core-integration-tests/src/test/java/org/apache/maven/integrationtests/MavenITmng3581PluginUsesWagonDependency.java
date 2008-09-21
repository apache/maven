package org.apache.maven.integrationtests;

import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;

public class MavenITmng3581PluginUsesWagonDependency
    extends AbstractMavenIntegrationTestCase
{
    public MavenITmng3581PluginUsesWagonDependency()
    {
        // Not 2.0.9
        super( "(,2.0.9),(2.0.9,)" );
    }

    /**
     * Test that a plugin using a wagon directly works
     */
    public void testmng3581()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-3581-useWagonDependency" );
        File pluginDir = new File( testDir, "plugin" );
        File projectDir = new File( testDir, "project" );

        Verifier verifier = new Verifier( pluginDir.getAbsolutePath() );
        verifier.executeGoal( "install" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        verifier = new Verifier( projectDir.getAbsolutePath() );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

    }
}

