package org.apache.maven.integrationtests;

import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;

public class MavenIT0128DistMgmtSiteUrlParentCalculationTest
    extends AbstractMavenIntegrationTestCase
{

    /**
     * Test DistributionManagement Site-URL calculation when modules are in sibling dirs of parent. (MNG-3134)
     */
    public void testit0128()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/it0128-distMgmtSiteUrlParentCalc" );
        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.executeGoal( "integration-test" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

    }
}

