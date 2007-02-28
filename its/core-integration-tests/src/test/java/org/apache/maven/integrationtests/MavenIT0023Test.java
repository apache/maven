package org.apache.maven.integrationtests;

import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;

import java.io.File;
import java.util.Properties;
import java.util.List;
import java.util.ArrayList;

public class MavenIT0023Test
    extends AbstractMavenIntegrationTestCase
{
    /**
     * Test profile inclusion from settings.xml (this one is activated by an id
     * in the activeProfiles section).
     */
    public void testit0023()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/it0023" );
        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        List cliOptions = new ArrayList();
        cliOptions.add( "--settings settings.xml" );
        verifier.setCliOptions( cliOptions );
        verifier.executeGoal( "org.apache.maven.its.plugins:maven-it-plugin-touch:touch" );
        verifier.assertFilePresent( "target/test.txt" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

    }
}

