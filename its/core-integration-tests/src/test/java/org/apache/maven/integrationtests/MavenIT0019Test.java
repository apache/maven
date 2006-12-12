package org.apache.maven.integrationtests;

import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;

public class MavenIT0019Test
    extends AbstractMavenIntegrationTestCase
{

    /**
     * Test that a version is managed by pluginManagement in the super POM
     */
    public void testit0019()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/it0019" );
        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.executeGoal( "compile" );
        verifier.assertFilePresent( "target/classes/org/apache/maven/it0019/Person.class" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

    }
}

