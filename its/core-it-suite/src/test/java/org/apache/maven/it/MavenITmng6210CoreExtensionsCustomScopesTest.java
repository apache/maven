package org.apache.maven.it;

import java.io.File;

import org.apache.maven.it.util.ResourceExtractor;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-6210">MNG-6210</a>:
 * check that Maven loads core extensions and {@code @SessionScoped} and
 * {@code @MojoExecutionScoped} components contributed by <code>.mvn/extensions.xml</code> are
 * available to regular plugins.
 */
public class MavenITmng6210CoreExtensionsCustomScopesTest
    extends AbstractMavenIntegrationTestCase
{
    public MavenITmng6210CoreExtensionsCustomScopesTest()
    {
        super( "(3.5.0,)" );
    }

    public void testCoreExtensionCustomScopes()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-6210-core-extensions-scopes" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.filterFile( "settings-template.xml", "settings.xml", "UTF-8", verifier.newDefaultFilterProperties() );

        verifier = newVerifier( new File( testDir, "client" ).getAbsolutePath() );
        verifier.deleteDirectory( "target" );
        verifier.deleteArtifacts( "org.apache.maven.its.it-core-extensions-custom-scopes" );
        verifier.getCliOptions().add( "-s" );
        verifier.getCliOptions().add( new File( testDir, "settings.xml" ).getAbsolutePath() );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
    }
}
