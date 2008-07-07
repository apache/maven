package org.apache.maven.integrationtests;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.integrationtests.AbstractMavenIntegrationTestCase;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

/**
 * Tests that artifact checksums are properly verified.
 */
public class MavenITmng2744checksumVerificationTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng2744checksumVerificationTest()
        throws InvalidVersionSpecificationException
    {
        super( "(2.0.8,)" ); // only test in 2.0.9+
    }

    /**
     * Tests that hex digits are compared without regard to case.
     */
    public void testitMNG2744()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-2744-checksumVerification" );

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );

        verifier.deleteArtifact( "org.apache.maven.its.mng2744", "a", "1", "pom" );
        verifier.deleteArtifact( "org.apache.maven.its.mng2744", "a", "1", "jar" );
        verifier.deleteArtifact( "org.apache.maven.its.mng2744", "b", "1", "pom" );
        verifier.deleteArtifact( "org.apache.maven.its.mng2744", "b", "1", "jar" );

        verifier.executeGoal( "compile" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
    }

}
