package org.apache.maven.integrationtests;

import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;

public class MavenIT0123SnapshotRangeRepositoryTest
    extends AbstractMavenIntegrationTestCase
{
    /**
     * Test that snapshot repositories are checked for ranges with snapshot boundaries.
     * 
     * @throws Exception
     * @see <a href="http://jira.codehaus.org/browse/MNG-2994">MNG-2994</a>
     */
    public void testit0123() throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/it0123-snapshotRangeRepository" );
        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.executeGoal( "compile" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
    }
}
