package org.apache.maven.integrationtests;

import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;
import java.util.Properties;

public class MavenIT0068Test
    extends AbstractMavenIntegrationTestCase
{

    /**
     * Test repository accumulation.
     */
    public void testit0068()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/it0068" );
        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.deleteArtifact( "org.codehaus.modello", "modello-core", "1.0-alpha-3", "jar" );
        Properties verifierProperties = new Properties();
        verifierProperties.put( "failOnErrorOutput", "false" );
        verifier.setVerifierProperties( verifierProperties );
        verifier.executeGoal( "generate-sources" );
        verifier.assertFilePresent( "target/generated-sources/modello/org/apache/maven/settings/Settings.java" );
// don't verify error free log
        verifier.resetStreams();

    }
}

