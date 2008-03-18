package org.apache.maven.integrationtests;

import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;
import java.util.Iterator;
import java.util.List;

public class MavenITmng3220ImportScopeTest
    extends AbstractMavenIntegrationTestCase
{
    public MavenITmng3220ImportScopeTest()
        throws InvalidVersionSpecificationException
    {
        super( "(2.0.8,)" ); // only test in 2.0.9+
    }

    public void testitMNG3220a()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(),
                                                                 "/mng-3220-importedDepMgmt/imported-pom-depMgmt" );

        File dmDir = new File( testDir, "dm-pom" );
        Verifier verifier = new Verifier( dmDir.getAbsolutePath() );

        verifier.executeGoal( "install" );

        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        File projectDir = new File( testDir, "project" );
        verifier = new Verifier( projectDir.getAbsolutePath() );

        verifier.executeGoal( "package" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
    }

    public void testitMNG3220b()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(),
                                                                 "/mng-3220-importedDepMgmt/depMgmt-pom-module-notImported" );

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );

        try
        {
            verifier.executeGoal( "install" );
            fail( "Should fail to build with missing junit version." );
        }
        catch ( VerificationException e )
        {
        }

        verifier.resetStreams();

        List lines = verifier.loadFile( new File( testDir, "log.txt" ), false );

        boolean found = false;
        for ( Iterator it = lines.iterator(); it.hasNext(); )
        {
            String line = (String) it.next();
            if ( line.indexOf( "\'dependencies.dependency.version\' is missing for junit:junit") > -1 )
            {
                found = true;
                break;
            }
        }

        assertTrue( "Should have found validation error line in output.", found );
    }

}
