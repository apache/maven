package org.apache.maven.it;

import org.apache.maven.shared.verifier.util.ResourceExtractor;
import org.apache.maven.shared.verifier.Verifier;

import java.io.File;
import java.util.Properties;

import org.junit.jupiter.api.Test;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-5771">MNG-5771</a>:
 * check that Maven loads core extensions and components contributed by <code>.mvn/extensions.xml</code>
 * are available to regular plugins.
 */
public class MavenITmng5771CoreExtensionsTest
    extends AbstractMavenIntegrationTestCase
{
    public MavenITmng5771CoreExtensionsTest()
    {
        super( "(3.2.5,)" );
    }

    @Test
    public void testCoreExtension()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-5771-core-extensions" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.filterFile( "settings-template.xml", "settings.xml", "UTF-8", verifier.newDefaultFilterProperties() );

        verifier = newVerifier( new File( testDir, "client" ).getAbsolutePath() );
        verifier.deleteDirectory( "target" );
        verifier.deleteArtifacts( "org.apache.maven.its.it-core-extensions" );
        verifier.addCliArgument( "-s" );
        verifier.addCliArgument( new File( testDir, "settings.xml" ).getAbsolutePath() );
        verifier.addCliArgument( "validate" );
        verifier.execute();
        verifier.verifyErrorFreeLog();
    }

    @Test
    public void testCoreExtensionNoDescriptor()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-5771-core-extensions" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.filterFile( "settings-template.xml", "settings.xml", "UTF-8", verifier.newDefaultFilterProperties() );

        verifier = newVerifier( new File( testDir, "client-no-descriptor" ).getAbsolutePath() );
        verifier.deleteDirectory( "target" );
        verifier.deleteArtifacts( "org.apache.maven.its.it-core-extensions" );
        verifier.addCliArgument( "-s" );
        verifier.addCliArgument( new File( testDir, "settings.xml" ).getAbsolutePath() );
        verifier.addCliArgument( "validate" );
        verifier.execute();
        verifier.verifyErrorFreeLog();
    }

    //
    // https://issues.apache.org/jira/browse/MNG-5795: Maven extensions can not be retrieved from authenticated repositories
    //
    @Test
    public void testCoreExtensionRetrievedFromAMirrorWithBasicAuthentication()
        throws Exception
    {
        requiresMavenVersion( "[3.3.2,)" );

        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-5771-core-extensions" );

        HttpServer server = HttpServer.builder() //
            .port(0) //
            .username("maven") //
            .password("secret") //
            .source(new File(testDir, "repo")) //
            .build();
        server.start();

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        Properties properties = verifier.newDefaultFilterProperties();
        properties.setProperty("@port@", Integer.toString( server.port() ) );
        String mirrorOf;
        if ( matchesVersionRange( "[4.0.0-alpha-1,)" ) )
        {
            mirrorOf = "*";
        }
        else
        {
            mirrorOf = "external:*";
        }
        properties.setProperty("@mirrorOf@", mirrorOf );
        verifier.filterFile( "settings-template-mirror-auth.xml", "settings.xml", "UTF-8", properties );

        verifier = newVerifier( new File( testDir, "client" ).getAbsolutePath() );
        verifier.deleteDirectory( "target" );
        verifier.deleteArtifacts( "org.apache.maven.its.it-core-extensions" );
        verifier.addCliArgument( "-s" );
        verifier.addCliArgument( new File( testDir, "settings.xml" ).getAbsolutePath() );
        verifier.addCliArgument( "validate" );
        verifier.execute();
        verifier.verifyErrorFreeLog();

        server.stop();
    }

    //
    // https://issues.apache.org/jira/browse/MNG-7395: Support properties in extensions.xml
    //
    @Test
    public void testCoreExtensionWithProperties()
        throws Exception
    {
        requiresMavenVersion( "[3.8.5,)" );

        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-5771-core-extensions" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.filterFile( "settings-template.xml", "settings.xml", "UTF-8", verifier.newDefaultFilterProperties() );

        verifier = newVerifier( new File( testDir, "client-properties" ).getAbsolutePath() );
        verifier.deleteDirectory( "target" );
        verifier.deleteArtifacts( "org.apache.maven.its.it-core-extensions" );
        verifier.addCliArgument( "-s" );
        verifier.addCliArgument( new File( testDir, "settings.xml" ).getAbsolutePath() );
        verifier.addCliArgument( "-Dtest-extension-version=0.1" );
        verifier.addCliArgument( "validate" );
        verifier.execute();
        verifier.verifyErrorFreeLog();
    }

    //
    // https://issues.apache.org/jira/browse/MNG-7395: Support properties in extensions.xml
    //
    @Test
    public void testCoreExtensionWithConfig()
        throws Exception
    {
        requiresMavenVersion( "[3.8.5,)" );

        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-5771-core-extensions" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.filterFile( "settings-template.xml", "settings.xml", "UTF-8", verifier.newDefaultFilterProperties() );

        verifier = newVerifier( new File( testDir, "client-config" ).getAbsolutePath() );
        verifier.deleteDirectory( "target" );
        verifier.deleteArtifacts( "org.apache.maven.its.it-core-extensions" );
        verifier.addCliArgument( "-s" );
        verifier.addCliArgument( new File( testDir, "settings.xml" ).getAbsolutePath() );
        verifier.setForkJvm( true ); // force forked JVM since we need the shell script to detect .mvn/
        verifier.addCliArgument( "validate" );
        verifier.execute();
        verifier.verifyErrorFreeLog();
    }
}
