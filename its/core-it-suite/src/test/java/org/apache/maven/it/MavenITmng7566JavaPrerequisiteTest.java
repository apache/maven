package org.apache.maven.it;

import java.io.File;

import org.apache.maven.shared.verifier.Verifier;
import org.apache.maven.shared.verifier.util.ResourceExtractor;
import org.junit.jupiter.api.Test;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-7566">MNG-7566</a>.
 * Similar to {@link MavenITmng4840MavenPrerequisiteTest}.
 *
 */
class MavenITmng7566JavaPrerequisiteTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng7566JavaPrerequisiteTest()
    {
        super( "[4.0.0-alpha-3,)" );
    }

    /**
     * Verify that builds fail straight when the current Java version doesn't match a plugin's prerequisite.
     *
     * @throws Exception in case of failure
     */
    @Test
    void testitMojoExecution()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-7566" );

        Verifier verifier = newVerifier( new File( testDir, "test-1" ).getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.deleteArtifacts( "org.apache.maven.its.mng7566" );
        verifier.addCliOption( "-s" );
        verifier.addCliOption( "settings.xml" );
        verifier.filterFile( "../settings-template.xml", "settings.xml", "UTF-8", verifier.newDefaultFilterProperties() );
        try
        {
            verifier.executeGoal( "validate" );
            verifier.verifyErrorFreeLog();
            fail( "Build did not fail despite unsatisfied prerequisite of plugin on Maven version." );
        }
        catch ( Exception e )
        {
            // expected, unsolvable version conflict
        }
        finally
        {
            verifier.resetStreams();
        }
    }

    /**
     * Verify that automatic plugin version resolution automatically skips plugin versions whose prerequisite on
     * the current Java version isn't satisfied.
     *
     * @throws Exception in case of failure
     */
    @Test
    void testitPluginVersionResolution()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-7566" );

        Verifier verifier = newVerifier( new File( testDir, "test-2" ).getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.deleteArtifacts( "org.apache.maven.its.mng7566" );
        verifier.addCliOption( "-s" );
        verifier.addCliOption( "settings.xml" );
        verifier.filterFile( "../settings-template.xml", "settings.xml", "UTF-8", verifier.newDefaultFilterProperties() );
        verifier.executeGoal( "org.apache.maven.its.mng7566:maven-mng7566-plugin:touch" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        verifier.verifyFilePresent( "target/touch-1.txt" );
        verifier.verifyFileNotPresent( "target/touch-2.txt" );
    }

}
