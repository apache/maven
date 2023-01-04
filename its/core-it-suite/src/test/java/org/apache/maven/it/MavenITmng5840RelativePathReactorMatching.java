package org.apache.maven.it;

import org.apache.maven.shared.verifier.util.ResourceExtractor;
import org.apache.maven.shared.verifier.Verifier;

import java.io.File;

import org.junit.jupiter.api.Test;

public class MavenITmng5840RelativePathReactorMatching
    extends AbstractMavenIntegrationTestCase
{
    public MavenITmng5840RelativePathReactorMatching()
    {
        super( ALL_MAVEN_VERSIONS );
    }

    @Test
    public void testRelativePathPointsToWrongVersion()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-5840-relative-path-reactor-matching" );

        Verifier verifier = newVerifier( new File( testDir, "parent-1" ).getAbsolutePath(), "remote" );
        verifier.executeGoal( "install" );
        verifier.verifyErrorFreeLog();

        verifier = newVerifier( new File( testDir, "child" ).getAbsolutePath(), "remote" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
    }
}
