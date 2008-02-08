package org.apache.maven.integrationtests;

import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MavenIT0100Test
    extends AbstractMavenIntegrationTestCase
{

    /**
     * Test that ${parent.artifactId} resolves correctly. [MNG-2124]
     */
    public void testit0100()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/it0100" );
        File child = new File( testDir, "parent/child" );

        Verifier verifier = new Verifier( child.getAbsolutePath() );

        List options = new ArrayList();
        options.add( "-Doutput=" + new File( child, "target/effective-pom.txt" ).getAbsolutePath() );

        verifier.setCliOptions( options );

        List goals = new ArrayList();
        goals.add( "org.apache.maven.plugins:maven-help-plugin:2.0.2:effective-pom" );
        goals.add( "verify" );

        verifier.executeGoals( goals );

        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

    }
}

