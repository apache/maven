package org.apache.maven.integrationtests;

import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MavenIT0102Test
    extends AbstractMavenIntegrationTestCase
{

    /**
     * Test that &lt;activeByDefault/&gt; calculations for profile activation only
     * use profiles defined in the POM. [MNG-2136]
     */
    public void testit0102()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/it0102" );

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );

        List options = new ArrayList();
        options.add( "-Doutput=" + new File( testDir, "target/effective-pom.txt" ).getAbsolutePath() );

        verifier.setCliOptions( options );

        List goals = new ArrayList();
        goals.add( "org.apache.maven.plugins:maven-help-plugin:2.0.2:effective-pom" );
        goals.add( "verify" );

        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

    }
}

