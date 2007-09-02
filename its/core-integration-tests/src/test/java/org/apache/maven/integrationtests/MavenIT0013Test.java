package org.apache.maven.integrationtests;

import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class MavenIT0013Test
    extends AbstractMavenIntegrationTestCase
{

    /**
     * Test plugin-plugin, which tests maven-plugin-tools-api and
     * maven-plugin-tools-java. This will generate a plugin descriptor from
     * java-based mojo sources, install the plugin, and then use it.
     */
    public void testit0013()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/it0013" );
        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.deleteArtifact( "org.apache.maven.its.plugins", "maven-it-it0013", "1.0-SNAPSHOT", "maven-plugin" );
        List goals = Arrays.asList( new String[]{"install" } );
        verifier.executeGoals( goals );
        verifier.assertFilePresent( "target/maven-it-it0013-1.0-SNAPSHOT.jar" );

        verifier = new Verifier( testDir.getAbsolutePath() );        
        goals = Arrays.asList( new String[]{"org.apache.maven.its.it0013:maven-it-it0013:it0013"} );
        verifier.executeGoals( goals );
        verifier.assertFilePresent( "target/it0013-verify" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
    }
}

