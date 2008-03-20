package org.apache.maven.integrationtests;

import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;

/**
 * Tests that the PluginDescriptor.getArtifacts() call returns all of the dependencies of the plugin,
 * not just those that made it past the filter excluding Maven's core artifacts.
 */
public class MavenITmng3473PluginReportCrash
    extends AbstractMavenIntegrationTestCase
{
    public void testitMNG3473 ()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-3473PluginReportCrash" );

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );

        verifier.executeGoal( "install site" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
    }
}
