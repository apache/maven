package org.apache.maven.integrationtests;

import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;

public class MavenITmng3475BaseAlignedDir
    extends AbstractMavenIntegrationTestCase
{
    
    public MavenITmng3475BaseAlignedDir()
        throws InvalidVersionSpecificationException
    {
        super( "(2.0.99,)"); // 2.1.0+ only
    }

    public void testitMNG3475()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(),
                                                                 "/mng-3475-baseAlignedDir" );

        File pluginDir = new File( testDir, "plugin" );
        Verifier verifier = new Verifier( pluginDir.getAbsolutePath() );

        verifier.executeGoal( "install" );

        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        File projectDir = new File( testDir, "project" );
        verifier = new Verifier( projectDir.getAbsolutePath() );

        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
    }

}
