package org.apache.maven.integrationtests;

import java.io.File;
import java.util.List;
import java.util.ArrayList;

import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

public class MavenIT0118AttachedArtifactsInReactor
    extends AbstractMavenIntegrationTestCase
{
    public void testit0118()
        throws Exception
    {
        File testDir =
            ResourceExtractor.simpleExtractResources( getClass(), "/it0118-attachedartifactinreactor" );

        Verifier verifier;

        // Install the parent POM
        verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.deleteArtifact( "org.apache.maven.its.it0118", "parent", "1.0", "pom" );                
        verifier.deleteArtifact( "org.apache.maven.its.it0118", "one", "1.0", "jar" );
        verifier.deleteArtifact( "org.apache.maven.its.it0118", "two", "1.0", "pom" );
        List cliOptions = new ArrayList();
        verifier.executeGoal( "package" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
    }
}
