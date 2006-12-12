package org.apache.maven.integrationtests;

import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;

public class MavenIT0078Test
    extends AbstractMavenIntegrationTestCase
{

    /**
     * Test that configuration for maven-compiler-plugin is injected from
     * PluginManagement section even when it's not explicitly defined in the
     * plugins section.
     */
    public void testit0078()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/it0078" );
        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.executeGoal( "compile" );
        verifier.assertFileNotPresent( "target/classes/Test.class" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

    }
}

