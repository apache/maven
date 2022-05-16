package org.apache.maven.it;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.apache.maven.it.util.ResourceExtractor;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-7470">MNG-7470</a>:
 * check that Maven two bundled transports works as expected.
 */
public class MavenITmng7470ResolverTransportTest
        extends AbstractMavenIntegrationTestCase
{
    private File projectDir;

    private HttpServer server;

    private int port;

    public MavenITmng7470ResolverTransportTest()
    {
        super( "[3.9.0,)" );
    }

    @Override
    protected void setUp()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-7470-resolver-transport" );
        projectDir = new File( testDir, "project" );

        server = HttpServer.builder()
                .port( 0 )
                .source( new File( testDir, "repo" ) )
                .build();
        server.start();
        if ( server.isFailed() )
        {
            fail( "Couldn't bind the server socket to a free port!" );
        }
        port = server.port();
        System.out.println( "Bound server socket to the port " + port );
    }

    @Override
    protected void tearDown()
        throws Exception
    {
        if ( server != null )
        {
            server.stop();
            server.join();
        }
    }

    private void performTest( /* nullable */ final String transport, final String logSnippet ) throws Exception
    {
        Verifier verifier = newVerifier( projectDir.getAbsolutePath() );
        verifier.setForkJvm( true );

        Map<String, String> properties = new HashMap<>();
        properties.put( "@port@", Integer.toString( port ) );
        verifier.filterFile( "settings-template.xml", "settings.xml", "UTF-8", properties );
        if ( transport == null )
        {
            verifier.setLogFileName( "default-transport.log" );
        }
        else
        {
            verifier.setLogFileName( transport + "-transport.log" );
        }
        verifier.deleteDirectory( "target" );
        verifier.deleteArtifacts( "org.apache.maven.resolver.resolver-demo-maven-plugin" );
        verifier.deleteArtifacts( "org.apache.maven.its.resolver-transport" );
        verifier.addCliOption( "-X" );
        verifier.addCliOption( "-s" );
        verifier.addCliOption( new File( projectDir, "settings.xml" ).getAbsolutePath() );
        verifier.addCliOption( "-Pmaven-core-it-repo" );
        if ( transport != null )
        {
            verifier.addCliOption( "-Dmaven.resolver.transport=" + transport );
        }
        // Maven will fail if project dependencies cannot be resolved.
        // As dependency exists ONLY in HTTP repo, it MUST be reached using selected transport and
        // successfully resolved from it.
        verifier.executeGoal( "verify" );
        verifier.verifyErrorFreeLog();
        // verify maven console output contains "[DEBUG] Using transporter XXXTransporter"
        verifier.verifyTextInLog( logSnippet );
        verifier.resetStreams();
    }

    public void testResolverTransportDefault()
            throws Exception
    {
        performTest( null, "[DEBUG] Using transporter WagonTransporter" );
    }

    public void testResolverTransportWagon()
            throws Exception
    {
        performTest( "wagon", "[DEBUG] Using transporter WagonTransporter" );
    }

    public void testResolverTransportNative()
            throws Exception
    {
        performTest( "native", "[DEBUG] Using transporter HttpTransporter" );
    }
}
