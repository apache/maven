package org.apache.maven.integrationtests;

import junit.framework.TestCase;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;

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
        verifier.executeGoal( "verify" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
        System.out.println( "it0102 PASS" );
    }
}

