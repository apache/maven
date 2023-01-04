package org.apache.maven.it;

import java.io.File;
import java.util.Properties;

import org.apache.maven.shared.verifier.Verifier;
import org.apache.maven.shared.verifier.util.ResourceExtractor;
import org.junit.jupiter.api.Test;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-6609">MNG-6609</a>.
 * Similar to {@link MavenITmng2276ProfileActivationBySettingsPropertyTest}.
 */
class MavenITmng6609ProfileActivationForPackagingTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng6609ProfileActivationForPackagingTest()
    {
        super( "[3.9.0,4.0.0-alpha-1),[4.0.0-alpha-3,)" );
    }

    /**
     * Verify that builds uses packaging based activation.
     * Each profile writes a Maven property named "packaging" with a different value (containing the actual packaging)
     *
     * @throws Exception in case of failure
     */
    @Test
    void testitMojoExecution()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-6609" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.addCliArgument( "validate" );
        verifier.execute();
        verifier.verifyErrorFreeLog();
        Properties props = verifier.loadProperties( "target/profile.properties" );
        assertEquals( "pom", props.getProperty( "project.properties.packaging" ) );
        props = verifier.loadProperties( "jar/target/profile.properties" );
        assertEquals( "jar", props.getProperty( "project.properties.packaging" ) );
        props = verifier.loadProperties( "jar-no-packaging/target/profile.properties" );
        assertEquals( "jar", props.getProperty( "project.properties.packaging" ) );
        props = verifier.loadProperties( "war/target/profile.properties" );
        assertEquals( "war", props.getProperty( "project.properties.packaging" ) );
    }
}
