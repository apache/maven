package org.apache.maven.integrationtests;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.integrationtests.AbstractMavenIntegrationTestCase;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;

/**
 * Verify that any plugin injecting reactorProjects gets project instances that
 * have their concrete state calculated.
 * 
 * @author jdcasey
 */
public class MavenITmng3694ReactorProjectsDynamismTest
    extends AbstractMavenIntegrationTestCase
{
    public MavenITmng3694ReactorProjectsDynamismTest()
        throws InvalidVersionSpecificationException
    {
        super( "[,2.99.99)" );
    }

    public void testitMNG3694 ()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-3694-reactorProjectsDynamism" );

        File pluginDir = new File( testDir, "maven-mng3694-plugin" );
        File projectDir = new File( testDir, "projects" );
        
        Verifier verifier = new Verifier( pluginDir.getAbsolutePath() );

        verifier.executeGoal( "install" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
        
        verifier = new Verifier( projectDir.getAbsolutePath() );

        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
    }
}
