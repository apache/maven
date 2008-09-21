package org.apache.maven.integrationtests;

import java.io.File;

import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

/**
 * Verify that dependencies with invalid POMs can still be used without failing
 * the build.
 * 
 * @author jdcasey
 */
public class MavenITmng3680InvalidDependencyPOMTest
    extends AbstractMavenIntegrationTestCase
{
    public MavenITmng3680InvalidDependencyPOMTest()
    {
        super( "(2.0.9,)" );
    }
    
    public void testitMNG3680 ()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-3680-invalidDependencyPOM" );
        File pluginDir = new File( testDir, "maven-mng3680-plugin" );
        
        Verifier verifier = new Verifier( pluginDir.getAbsolutePath() );
        
        verifier.executeGoal( "install" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        verifier = new Verifier( testDir.getAbsolutePath() );
        
        verifier.deleteArtifact( "tests", "dep-L1", "1", "jar" );
        verifier.deleteArtifact( "tests", "dep-L1", "1", "pom" );
        
        verifier.deleteArtifact( "tests", "dep-L1", "1", "jar" );
        verifier.deleteArtifact( "tests", "dep-L2", "1", "pom" );

        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
    }
}
