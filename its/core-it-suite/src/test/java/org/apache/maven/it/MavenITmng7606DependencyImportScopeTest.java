package org.apache.maven.it;

import java.io.File;

import org.apache.maven.shared.verifier.Verifier;
import org.apache.maven.shared.verifier.util.ResourceExtractor;
import org.junit.jupiter.api.Test;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-7606">MNG-7606</a>.
 * It checks that "import" scope for dependencies work
 *
 */
class MavenITmng7606DependencyImportScopeTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng7606DependencyImportScopeTest()
    {
        super( ALL_MAVEN_VERSIONS );
    }

    /**
     * Verify that dependencies which are managed through imported dependency management work
     *
     * @throws Exception in case of failure
     */
    @Test
    void testDependencyResolution()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-7606" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( true );
        verifier.deleteArtifacts( "org.apache.maven.its.mng7606" );
        verifier.addCliArgument( "verify" );
        verifier.execute();
        verifier.verifyErrorFreeLog();
    }

}
