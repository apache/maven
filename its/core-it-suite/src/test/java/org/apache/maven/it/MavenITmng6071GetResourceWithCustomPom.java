package org.apache.maven.it;

import org.apache.maven.it.util.ResourceExtractor;
import org.junit.Test;

import java.io.File;
import java.util.Properties;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-6071">MNG-6071</a>:
 * check that getClass().getResource("/") returns consistent results when Maven is run with <code>-f ./pom.xml</code>.
 */
public class MavenITmng6071GetResourceWithCustomPom
    extends AbstractMavenIntegrationTestCase
{
    public MavenITmng6071GetResourceWithCustomPom()
    {
        super( "[3.7.0-SNAPSHOT,)" );
    }

    /**
     * check when path to POM set by <code>-f ./pom.xml</code>
     */
    @Test
    public void testRunCustomPomWithDot( )
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-6071" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.getCliOptions().add( "-f ./pom.xml" );
        verifier.setForkJvm( true );
        verifier.executeGoal( "verify" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
    }
}
