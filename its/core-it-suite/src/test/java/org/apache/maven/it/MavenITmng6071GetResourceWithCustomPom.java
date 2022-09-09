package org.apache.maven.it;

import org.apache.maven.it.util.ResourceExtractor;
import org.junit.Test;

import java.io.File;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-6071">MNG-6071</a>:
 * check that getClass().getResource("/") returns consistent results when Maven is run with <code>-f ./pom.xml</code>.
 */
public class MavenITmng6071GetResourceWithCustomPom
    extends AbstractMavenIntegrationTestCase
{
    public MavenITmng6071GetResourceWithCustomPom()
    {
        super( "[3.8.2,)" );
    }

    /**
     * check when path to POM set by <code>-f ./pom.xml</code>
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testRunCustomPomWithDot( )
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-6071" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.addCliOption( "-f" );
        verifier.addCliOption( "./pom.xml" );
        verifier.setForkJvm( true );
        verifier.executeGoal( "verify" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
    }
}
