package org.apache.maven.integrationtests;

import org.apache.maven.it.Verifier;
import org.apache.maven.it.VerificationException;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;

public class MavenIT0103Test
    extends AbstractMavenIntegrationTestCase
{

    /**
     * Verify that multimodule builds where one project references another as
     * a parent can build, even if that parent is not correctly referenced by
     * &lt;relativePath/&gt; and is not in the local repository. [MNG-2196]
     */
    public void testit0103()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/it0103" );
        Verifier verifier = new Verifier( testDir.getAbsolutePath() );

        
        if ( matchesVersionRange( "[2.0.4, 2.99.99)" ) )
        {
            verifier.executeGoal( "package" );
            verifier.verifyErrorFreeLog();
            verifier.resetStreams();
        }
        else
        {
            try
            {
                verifier.executeGoal( "package" );
            }
            catch ( VerificationException e )
            {
                verifier.verifyTextInLog( "java.io.IOException" );
                verifier.resetStreams();
                return;
            }
            throw new VerificationException( "Build should have failed with java.io.IOException" );           
        }
    }
}

