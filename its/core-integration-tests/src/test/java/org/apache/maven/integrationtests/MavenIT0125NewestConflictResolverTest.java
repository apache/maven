package org.apache.maven.integrationtests;

import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;

public class MavenIT0125NewestConflictResolverTest
    extends AbstractMavenIntegrationTestCase
{
    /**
     * Test that ensures the newest-wins conflict resolver is used.
     * 
     * @throws Exception
     * @see <a href="http://jira.codehaus.org/browse/MNG-612">MNG-612</a>
     */
    public void testit0125() throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/it0125-newestConflictResolver/dependency" );
        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.executeGoal( "install" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        testDir = ResourceExtractor.simpleExtractResources( getClass(), "/it0125-newestConflictResolver/plugin" );
        verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.executeGoal( "install" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        testDir = ResourceExtractor.simpleExtractResources( getClass(), "/it0125-newestConflictResolver/project" );
        verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.executeGoal( "verify" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
    }
}
