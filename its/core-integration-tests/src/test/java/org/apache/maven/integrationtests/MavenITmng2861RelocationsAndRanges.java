package org.apache.maven.integrationtests;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.integrationtests.AbstractMavenIntegrationTestCase;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;


public class MavenITmng2861RelocationsAndRanges
    extends AbstractMavenIntegrationTestCase
{
    public void testitMNG2123 ()
        throws Exception
    {
       

        // The testdir is computed from the location of this
        // file.
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-2861relocationsAndRanges/MNG-2861" );

        Verifier verifier;

        /*
         * We must first make sure that any artifact created
         * by this test has been removed from the local
         * repository. Failing to do this could cause
         * unstable test results. Fortunately, the verifier
         * makes it easy to do this.
         */
        verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.deleteArtifact( "org.apache.maven.its.mng2123", "MNG-2861", "1.0-SNAPSHOT", "pom" );
        verifier.deleteArtifact( "org.apache.maven.its.mng2123", "A", "1.0-SNAPSHOT", "jar" );
        verifier.deleteArtifact( "org.apache.maven.its.mng2123", "B", "1.0-SNAPSHOT", "jar" );
        verifier.deleteArtifact( "org.apache.maven.its.mng2123", "C", "1.0-SNAPSHOT", "jar" );

        List cliOptions = new ArrayList();
        cliOptions.add( "-N" );
        verifier.executeGoal( "install" );

        verifier.verifyErrorFreeLog();

    }
}
