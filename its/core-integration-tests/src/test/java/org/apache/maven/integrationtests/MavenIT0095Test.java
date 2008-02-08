package org.apache.maven.integrationtests;

import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MavenIT0095Test
    extends AbstractMavenIntegrationTestCase
{

    /**
     * Test URL calculation when modules are in sibling dirs of parent. (MNG-2006)
     */
    public void testit0095()
        throws Exception
    {
        // TODO: This is WRONG! Need to run only sub1 to effective-pom, then run all to verify.
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/it0095" );
        File sub1 = new File( testDir, "sub1" );

        Verifier verifier = new Verifier( sub1.getAbsolutePath() );

        List options = new ArrayList();
        options.add( "-Doutput=" + new File( sub1, "target/effective-pom.xml" ).getAbsolutePath() );

        verifier.setCliOptions( options );

        List goals = new ArrayList();
        goals.add( "org.apache.maven.plugins:maven-help-plugin:2.0.2:effective-pom" );
        goals.add( "verify" );

        verifier.executeGoals( goals );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

    }
}

