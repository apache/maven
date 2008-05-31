package org.apache.maven.integrationtests;

import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;

public class MavenITmng3581PluginUsesWagonDependency
    extends AbstractMavenIntegrationTestCase
{
    public MavenITmng3581PluginUsesWagonDependency()
        throws InvalidVersionSpecificationException
    {
        // Not 2.0.9
        super( "(,2.0.9),(2.0.9,2.1-SNAPSHOT)" );
    }

    /**
     * Test that a plugin using a wagon directly works
     */
    public void testmng3581()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-3581-useWagonDependency" );
        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

    }
}

