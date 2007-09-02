package org.apache.maven.integrationtests;

import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class MavenIT0090Test
    extends AbstractMavenIntegrationTestCase
{
    /**
     * Test that ensures that envars are interpolated correctly into plugin
     * configurations.
     */
    public void testit0090()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/it0090" );
        Verifier verifier = new Verifier( testDir.getAbsolutePath(), true );
        Map envVars = new HashMap();
        envVars.put( "MAVEN_TEST_ENVAR", "MAVEN_TEST_ENVAR_VALUE" );
        verifier.executeGoal( "test", envVars );
        verifier.assertFilePresent( "target/mojo-generated.properties" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

    }
}

