package org.apache.maven.integrationtests;

import java.io.File;
import java.util.ArrayList;

import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

public class MavenIT0119PluginPrefixOrder
    extends AbstractMavenIntegrationTestCase
{
    public void testit0119()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/it0119-pluginprefixorder" );

        Verifier verifier;

        // Install the parent POM, extension and the plugin
        verifier = new Verifier( testDir.getAbsolutePath() );

        verifier.executeGoal( "install" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
        

        // now run the test. Since we have apache and codehaus, i should get the apache one first
        testDir = ResourceExtractor.simpleExtractResources( getClass(), "/it0119-pluginprefixorder/test-project" );
        verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.executeGoal( "it0119:apache" );
        verifier.verifyErrorFreeLog();

        
//      now run the test. Since we have apache and codehaus and a prefix in my settings, i should get the custom one first
        testDir = ResourceExtractor.simpleExtractResources( getClass(), "/it0119-pluginprefixorder/test-project" );
        verifier = new Verifier( testDir.getAbsolutePath() );
        
        //use my custom settings upon invocation.
        ArrayList cli = new ArrayList();
        cli.add("-s '" +testDir.getAbsolutePath()+"/settings.xml'");
        verifier.setCliOptions( cli );
        verifier.executeGoal( "it0119:custom" );
        verifier.verifyErrorFreeLog();
    }
}
