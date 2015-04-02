package org.apache.maven.it;

import java.io.File;
import java.util.Properties;

import org.apache.maven.it.util.ResourceExtractor;

public class MavenITmng5771CoreExtensionsTest
    extends AbstractMavenIntegrationTestCase
{
    public MavenITmng5771CoreExtensionsTest()
    {
        super( "(3.2.5,)" );
    }

    public void testCoreExtension()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-5771-core-extensions" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.filterFile( "settings-template.xml", "settings.xml", "UTF-8", verifier.newDefaultFilterProperties() );

        verifier = newVerifier( new File( testDir, "client" ).getAbsolutePath() );
        verifier.deleteDirectory( "target" );
        verifier.deleteArtifacts( "org.apache.maven.its.it-core-extensions" );
        verifier.getCliOptions().add( "-s" );
        verifier.getCliOptions().add( new File( testDir, "settings.xml" ).getAbsolutePath() );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
    }

    public void testCoreExtensionNoDescriptor()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-5771-core-extensions" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.filterFile( "settings-template.xml", "settings.xml", "UTF-8", verifier.newDefaultFilterProperties() );

        verifier = newVerifier( new File( testDir, "client-no-descriptor" ).getAbsolutePath() );
        verifier.deleteDirectory( "target" );
        verifier.deleteArtifacts( "org.apache.maven.its.it-core-extensions" );
        verifier.getCliOptions().add( "-s" );
        verifier.getCliOptions().add( new File( testDir, "settings.xml" ).getAbsolutePath() );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
    }
    
    //
    // https://jira.codehaus.org/browse/MNG-5795: Maven extensions can not be retrieved from authenticated repositories
    //
    public void testCoreExtensionRetrievedFromAMirrorWithBasicAuthentication()
        throws Exception
    {
        requiresMavenVersion( "[3.3.2-SNAPSHOT,)" );

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
        properties.setProperty("@port@", Integer.toString(server.port()));
        verifier.filterFile( "settings-template-mirror-auth.xml", "settings.xml", "UTF-8", properties );

        verifier = newVerifier( new File( testDir, "client" ).getAbsolutePath() );
        verifier.deleteDirectory( "target" );
        verifier.deleteArtifacts( "org.apache.maven.its.it-core-extensions" );
        verifier.getCliOptions().add( "-s" );
        verifier.getCliOptions().add( new File( testDir, "settings.xml" ).getAbsolutePath() );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
        
        server.stop();
    }
    

}
