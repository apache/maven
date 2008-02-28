package org.apache.maven.integrationtests;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.integrationtests.AbstractMavenIntegrationTestCase;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

public class MavenITmng2339BadProjectInterpolationTest
    extends AbstractMavenIntegrationTestCase
{
    public MavenITmng2339BadProjectInterpolationTest()
        throws org.apache.maven.artifact.versioning.InvalidVersionSpecificationException
    {
        super( "(2.0.8,)" ); // 2.0.9+
    }

    public void testitMNG2339()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-2339-badProjectInterpolation" );

        Verifier verifier;

        verifier = new Verifier( testDir.getAbsolutePath() );

        List cliOptions = new ArrayList();
        cliOptions.add( "-Dversion=foo" );
        verifier.setCliOptions( cliOptions );
        verifier.executeGoal( "process-sources" );

        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
    }
}
