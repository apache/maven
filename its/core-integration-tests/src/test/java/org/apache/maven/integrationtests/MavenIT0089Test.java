package org.apache.maven.integrationtests;

import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;

public class MavenIT0089Test
    extends AbstractMavenIntegrationTestCase
{

    /**
     * Test that Checkstyle PackageNamesLoader.loadModuleFactory(..) method will complete as-is with
     * the context classloader available to the plugin.
     */
    public void testit0089()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/it0089" );
        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.deleteArtifact( "org.apache.maven.plugins", "maven-it-it-plugin", "1.0", "maven-plugin" );
        verifier.executeGoal( "install" );
        verifier.assertFilePresent( "project/target/output.txt" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

    }
}

