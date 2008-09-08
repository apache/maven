package org.apache.maven.integrationtests;

import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MavenITmng3535SelfReferentialProperties
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng3535SelfReferentialProperties()
        throws InvalidVersionSpecificationException
    {
        super( "(2.0.9,)" );
    }

    public void testitMNG3535_ShouldSucceed()
        throws Exception
    {
        File testDir =
            ResourceExtractor.simpleExtractResources( getClass(), "/mng-3535-selfReferentialProperties/success" );

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );

        List opts = new ArrayList();
        opts.add( "-X" );
        verifier.setCliOptions( opts );

        verifier.setAutoclean( false );
        verifier.executeGoal( "verify" );

        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
    }

    public void testitMNG3535_ShouldFail()
        throws Exception
    {
        File testDir =
            ResourceExtractor.simpleExtractResources( getClass(), "/mng-3535-selfReferentialProperties/failure" );

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );

        List opts = new ArrayList();
        opts.add( "-X" );
        verifier.setCliOptions( opts );

        verifier.setAutoclean( false );

        try
        {
            verifier.executeGoal( "verify" );
            
            verifier.verifyErrorFreeLog();
            fail( "There is a self-referential property in this build; it should fail." );
        }
        catch ( Exception e )
        {
            // should fail this verification, because there truly is a self-referential property.
        }
        
        verifier.resetStreams();
    }
}
