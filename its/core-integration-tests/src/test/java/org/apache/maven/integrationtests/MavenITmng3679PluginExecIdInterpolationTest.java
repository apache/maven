package org.apache.maven.integrationtests;

import java.io.File;

import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

/**
 * Checks that a plugin execution with an id that contains an expression will 
 * still execute without a problem.
 * 
 * @author jdcasey
 */
public class MavenITmng3679PluginExecIdInterpolationTest
    extends AbstractMavenIntegrationTestCase
{
    public void testitMNG3679 ()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-3679-pluginExecIdInterpolation" );

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );

        verifier.executeGoal( "validate" );

        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
        
        assertTrue( new File( testDir, "target/check.txt" ).exists() );
    }
}
