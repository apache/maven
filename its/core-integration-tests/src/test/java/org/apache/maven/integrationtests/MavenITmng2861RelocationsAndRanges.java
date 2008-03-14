package org.apache.maven.integrationtests;

import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;


public class MavenITmng2861RelocationsAndRanges
    extends AbstractMavenIntegrationTestCase
{
    public MavenITmng2861RelocationsAndRanges()
        throws InvalidVersionSpecificationException
    {
        super( "(2.0.8,)" );
    }

    public void testitMNG2861 ()
        throws Exception
    {


        // The testdir is computed from the location of this
        // file.
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-2861relocationsAndRanges" );

        Verifier verifier;

        /*
         * We must first make sure that any artifact created
         * by this test has been removed from the local
         * repository. Failing to do this could cause
         * unstable test results. Fortunately, the verifier
         * makes it easy to do this.
         */
        verifier = new Verifier( new File(testDir,"MNG-2861").getAbsolutePath() );
        verifier.deleteArtifact( "org.apache.maven.its.mng2123", "MNG-2861", "1.0-SNAPSHOT", "pom" );
        verifier.deleteArtifact( "org.apache.maven.its.mng2123", "A", "1.0-SNAPSHOT", "jar" );
        verifier.deleteArtifact( "org.apache.maven.its.mng2123", "B", "1.0-SNAPSHOT", "jar" );
        verifier.deleteArtifact( "org.apache.maven.its.mng2123", "C", "1.0-SNAPSHOT", "jar" );

        verifier.executeGoal( "install" );

        verifier.verifyErrorFreeLog();

    }
}
