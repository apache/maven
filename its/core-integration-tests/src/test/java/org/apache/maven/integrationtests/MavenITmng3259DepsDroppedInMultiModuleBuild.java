package org.apache.maven.integrationtests;

import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MavenITmng3259DepsDroppedInMultiModuleBuild
    extends AbstractMavenIntegrationTestCase
{
    public void testitMNG3259 ()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-3259-depsDroppedInMultiModuleBuild" );

        Verifier verifier;

        verifier = new Verifier( new File( testDir, "parent" ).getAbsolutePath() );

        List cliOptions = new ArrayList();

        verifier.setCliOptions( cliOptions );

        verifier.executeGoal( "install" );

        verifier.verifyErrorFreeLog();

        verifier.resetStreams();

        verifier = new Verifier( testDir.getAbsolutePath() );

        verifier.executeGoal( "install" );

        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
    }
}
