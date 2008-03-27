package org.apache.maven.integrationtests;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

/**
 * Tests that the PluginDescriptor.getArtifacts() call returns all of the dependencies of the plugin,
 * not just those that made it past the filter excluding Maven's core artifacts.
 */
public class MavenITmng3473PluginReportCrash
    extends AbstractMavenIntegrationTestCase
{
    public MavenITmng3473PluginReportCrash()
        throws InvalidVersionSpecificationException
    {
        super( "(2.0.8,)" ); // >2.0.8
    }
    public void testitMNG3473 ()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-3473PluginReportCrash" );

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );


        verifier.executeGoal( "install" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
        
        List cliOptions = new ArrayList();
        cliOptions.add( "-Dplugin.version=2.4.1" );
        verifier.setCliOptions( cliOptions );
        
        //should succeed with 2.4.1
        verifier.executeGoal( "site" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
        
        //should fail with 2.4
        cliOptions.clear();
        cliOptions.add( "-Dplugin.version=2.4" );
        verifier.setCliOptions( cliOptions );
        
        try
        {
          verifier.executeGoal( "site" );
        }
        catch (VerificationException e)
        {
          //expected this but don't require it cause some os's don't return the correct error code   
        }
        verifier.verifyTextInLog( "org/apache/maven/doxia/module/site/manager/SiteModuleNotFoundException" );
        verifier.resetStreams();
    }
}
