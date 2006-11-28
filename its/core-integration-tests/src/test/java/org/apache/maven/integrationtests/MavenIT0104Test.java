package org.apache.maven.integrationtests;

import junit.framework.TestCase;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;

/**
 * #it0104 Commenting out, not fixed until post-2.0.4, due to dependency on new plexus-container-default version.                      
 */
public class MavenIT0104Test
    extends AbstractMavenIntegrationTestCase
{

    /**
     * Verify that plugin configurations are resolved correctly, particularly
     * when they contain ${project.build.directory} in the string value of a
     * Map.Entry.
     */
    public void testit0104()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/it0104" );
        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.executeGoal( "test" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
        System.out.println( "it0104 PASS" );
    }
}

