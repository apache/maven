package org.apache.maven.integrationtests;

import junit.framework.TestCase;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;

public class MavenIT0065Test
    extends TestCase /*extends AbstractMavenIntegrationTest*/
{

    /**
     * Test that the basedir of the parent is set correctly.
     */
    public void testit0065()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/it0065" );
        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.executeGoal( "install" );
        verifier.assertFilePresent( "subproject/target/child-basedir" );
        verifier.assertFilePresent( "parent-basedir" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
        System.out.println( "it0065 PASS" );
    }
}

