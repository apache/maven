package org.apache.maven.integrationtests;

import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;

public class MavenIT0043Test
    extends AbstractMavenIntegrationTestCase
{

    /**
     * Test for repository inheritence - ensure using the same id overrides the defaults
     */
    public void testit0043()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/it0043" );

        File child1 = new File( testDir, "child1" );
        Verifier verifier = new Verifier( child1.getAbsolutePath() );

        verifier.deleteArtifact( "org.apache.maven.plugins", "maven-help-plugin", "2.0.2", "jar" );

        verifier.executeGoal( "org.apache.maven.plugins:maven-help-plugin:2.0.2:effective-pom" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        File child2 = new File( testDir, "child2" );
        verifier = new Verifier( child2.getAbsolutePath() );

        verifier.executeGoal( "org.apache.maven.plugins:maven-help-plugin:2.0.2:effective-pom" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

    }
}

