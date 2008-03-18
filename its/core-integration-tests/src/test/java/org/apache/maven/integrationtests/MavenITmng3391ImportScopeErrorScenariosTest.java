package org.apache.maven.integrationtests;

import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;

public class MavenITmng3391ImportScopeErrorScenariosTest
    extends AbstractMavenIntegrationTestCase
{
    public MavenITmng3391ImportScopeErrorScenariosTest()
        throws InvalidVersionSpecificationException
    {
        super( "(2.0.8,)" ); // only test in 2.0.9+
    }

    public void testitMNG3391a()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(),
                                                                 "/mng-3391-importScopeErrorScenarios/depMgmt-importPom-noParentCycle" );

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );

        verifier.deleteArtifact( "org.apache.maven.its.mng3391.2", "dm-pom", "1", "pom" );

        verifier.executeGoal( "install" );

        verifier.verifyErrorFreeLog();

        verifier.resetStreams();
    }

    public void testitMNG3391b()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(),
                                                                 "/mng-3391-importScopeErrorScenarios/depMgmt-importPom-noParentCycle" );

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );

        verifier.deleteArtifact( "org.apache.maven.its.mng3391.2", "dm-pom", "1", "pom" );

        Verifier v2 = new Verifier( new File( testDir, "dm-pom" ).getAbsolutePath() );
        v2.executeGoal( "install" );
        v2.verifyErrorFreeLog();
        v2.resetStreams();

        verifier.executeGoal( "install" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
    }

    public void testitMNG3391c()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(),
                                                                 "/mng-3391-importScopeErrorScenarios/depMgmt-importPom-parentCycle" );

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );

        verifier.deleteArtifact( "org.apache.maven.its.mng3391.1", "dm-pom", "1", "pom" );

        verifier.executeGoal( "install" );

        verifier.verifyErrorFreeLog();

        verifier.resetStreams();
    }

    public void testitMNG3391d()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(),
                                                                 "/mng-3391-importScopeErrorScenarios/depMgmt-importPom-parentCycle" );

        Verifier verifier = new Verifier( new File( testDir, "dm-pom" ).getAbsolutePath() );
        verifier.executeGoal( "install" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
    }

}
