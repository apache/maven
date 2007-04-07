package org.apache.maven.integrationtests;

import java.io.File;
import java.util.List;
import java.util.ArrayList;

import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

public class MavenIT0119PluginPrefixOrder
    extends AbstractMavenIntegrationTestCase
{
    public void testit0119()
        throws Exception
    {
        File testDir =
            ResourceExtractor.simpleExtractResources( getClass(), "/it0119-pluginprefixorder" );

        Verifier verifier;

        // Install the parent POM, extension and the plugin 
        verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.deleteArtifact( "org.apache.maven.plugins", "it0119", "1.0", "jar" );                
        verifier.deleteArtifact( "org.codehaus.mojo", "it0119", "1.0", "jar" );
        verifier.deleteArtifact( "it0119", "it0119", "1.0", "jar" );
        verifier.deleteArtifact( "org.apache.maven.its.it0119", "it0119-parent", "1.0", "pom" );
        
        
        List cliOptions = new ArrayList();        
        verifier.executeGoal( "install" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
        
        //now run the test
        testDir =
         ResourceExtractor.simpleExtractResources( getClass(), "/it0119-pluginprefixorder/test-project" );
        verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.executeGoal( "it0119:apache" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
        
    }
}
