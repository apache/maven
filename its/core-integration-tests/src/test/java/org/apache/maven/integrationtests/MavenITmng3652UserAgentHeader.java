package org.apache.maven.integrationtests;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.Request;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.AbstractHandler;

public class MavenITmng3652UserAgentHeader
    extends AbstractMavenIntegrationTestCase
{
    private Server server;

    private int port;

    private String userAgent;

    public MavenITmng3652UserAgentHeader()
        throws InvalidVersionSpecificationException
    {
        // TODO: fix support for this in 2.1-SNAPSHOT
        // 2.0.10+
        //super( "(2.0.9,)" );
        super( "(2.0.9,2.0.99)" );
    }

    public void setUp()
        throws Exception
    {
        Handler handler = new AbstractHandler()
        {   
            public void handle( String target, HttpServletRequest request, HttpServletResponse response, int dispatch )
                throws IOException, ServletException
            {
                userAgent = request.getHeader( "User-Agent" );

                response.setContentType( "text/plain" );
                response.setStatus( HttpServletResponse.SC_OK );
                response.getWriter().println( "some content" );
                response.getWriter().println();
                
                ( (Request) request ).setHandled( true );
            }
        };

        server = new Server( 0 );
        server.setHandler( handler );
        server.start();

        port = server.getConnectors()[0].getLocalPort();
    }

    protected void tearDown()
        throws Exception
    {
        super.tearDown();

        server.stop();
    }

    /**
     * Test that the user agent header is configured in the wagon manager.
     */
    public void testmng3652()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-3652-user-agent" );
        File pluginDir = new File( testDir, "test-plugin" );
        File projectDir = new File( testDir, "test-project" );

        Verifier verifier = new Verifier( pluginDir.getAbsolutePath() );
        verifier.executeGoal( "install" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        verifier = new Verifier( projectDir.getAbsolutePath() );
        
        List cliOptions = new ArrayList();
        cliOptions.add( "-DtestPort=" + port );
        verifier.setCliOptions( cliOptions );
        
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
        
        String userAgent = this.userAgent;
        assertNotNull( userAgent );
        
        File touchFile = new File( projectDir, "target/touch.txt" );
        assertTrue( touchFile.exists() );
        
        List lines = verifier.loadFile( touchFile, false );

        // NOTE: system property for maven.version may not exist if you use -Dtest
        // surefire parameter to run this single test. Therefore, the plugin writes
        // the maven version into the check file.
        String mavenVersion = ((String) lines.get( 0 )).substring( 0, 3 );
        String javaVersion = (String) lines.get( 1 );
        String os = (String) lines.get( 2 ) + " " + (String) lines.get( 3 );
        String artifactVersion = (String) lines.get( 4 );

        assertEquals( "Comparing User-Agent '" + userAgent + "'", "Apache-Maven/" + mavenVersion + " (Java " + javaVersion + "; " + os + ")" + " maven-artifact/" + artifactVersion, userAgent );

        // test webdav
        cliOptions = new ArrayList();
        cliOptions.add( "-DtestPort=" + port );
        cliOptions.add( "-DtestProtocol=dav:http" );
        verifier.setCliOptions( cliOptions );
        
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
        
        userAgent = this.userAgent;
        assertNotNull( userAgent );
        assertEquals( "Comparing User-Agent '" + userAgent + "'", "Apache-Maven/" + mavenVersion + " (Java " + javaVersion + "; " + os + ")" + " maven-artifact/" + artifactVersion, userAgent );

        // test settings with no config

        cliOptions = new ArrayList();
        cliOptions.add( "-DtestPort=" + port );
        cliOptions.add( "--settings" );
        cliOptions.add( "settings-no-config.xml" );
        verifier.setCliOptions( cliOptions );
        
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
        
        userAgent = this.userAgent;
        assertNotNull( userAgent );
        assertEquals( "Comparing User-Agent '" + userAgent + "'", "Apache-Maven/" + mavenVersion + " (Java " + javaVersion + "; " + os + ")" + " maven-artifact/" + artifactVersion, userAgent );

        // test settings with config

        cliOptions = new ArrayList();
        cliOptions.add( "-DtestPort=" + port );
        cliOptions.add( "--settings" );
        cliOptions.add( "settings.xml" );
        verifier.setCliOptions( cliOptions );
        
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
        
        userAgent = this.userAgent;
        assertNotNull( userAgent );
        assertEquals( "Maven Fu", userAgent );        
    }
}
