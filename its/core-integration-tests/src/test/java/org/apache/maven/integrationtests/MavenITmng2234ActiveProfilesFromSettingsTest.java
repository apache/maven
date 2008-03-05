package org.apache.maven.integrationtests;

import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MavenITmng2234ActiveProfilesFromSettingsTest
    extends AbstractMavenIntegrationTestCase
{
    public MavenITmng2234ActiveProfilesFromSettingsTest()
        throws InvalidVersionSpecificationException
    {
        super( "(2.0.8,)" );
    }

    public void testitMNG2234 ()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-2234-activeProfilesFromSettings" );

        Verifier verifier;

        verifier = new Verifier( testDir.getAbsolutePath() );

        List cliOptions = new ArrayList();
        cliOptions.add( "-s" );
        cliOptions.add( "settings.xml" );
        verifier.setCliOptions( cliOptions );
        verifier.executeGoal( "install" );

        verifier.verifyErrorFreeLog();
        verifier.assertFilePresent( "target/touch.txt" );
        verifier.resetStreams();
    }
}
