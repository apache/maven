package org.apache.maven.integrationtests;

import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MavenIT0039Test
    extends AbstractMavenIntegrationTestCase
{

    /**
     * Test reactor for projects that have release-pom.xml in addition to
     * pom.xml. The release-pom.xml file should be chosen above pom.xml for
     * these projects in the build.
     */
    public void testit0039()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/it0039" );
        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        List cliOptions = new ArrayList();
        cliOptions.add( "-r" );
        verifier.setCliOptions( cliOptions );
        verifier.executeGoal( "package" );
        verifier.assertFilePresent( "project/target/maven-it-it0039-p1-1.0.jar" );
        verifier.assertFilePresent( "project2/target/maven-it-it0039-p2-1.0.jar" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

    }
}

