package org.apache.maven.integrationtests;

import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class MavenIT0020Test
    extends AbstractMavenIntegrationTestCase
{

    /**
     * Test beanshell mojo support.
     */
    public void testit0020()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/it0020" );
        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.deleteArtifact( "org.apache.maven.its.plugins", "maven-it-it0020", "1.0-SNAPSHOT", "maven-plugin" );
        List goals = Arrays.asList( new String[]{"install"} );
        verifier.executeGoals( goals );

        verifier = new Verifier( testDir.getAbsolutePath() );        
        goals = Arrays.asList( new String[]{"org.apache.maven.its.it0020:maven-it-it0020:it0020"} );
        verifier.executeGoals( goals );
        verifier.assertFilePresent( "target/out.txt" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
    }
}

