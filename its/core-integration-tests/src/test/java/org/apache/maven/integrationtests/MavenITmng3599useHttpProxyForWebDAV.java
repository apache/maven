package org.apache.maven.integrationtests;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.FileUtils;
import org.apache.maven.it.util.ResourceExtractor;
import org.apache.maven.it.util.StringUtils;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.Request;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.AbstractHandler;

public class MavenITmng3599useHttpProxyForWebDAV
    extends AbstractMavenIntegrationTestCase
{
    private static final String LS = System.getProperty( "line.separator" );
    
    private Server server;

    private int port;

    private static final String content = "<project xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                            "  xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd\">\n" +
                            "  <modelVersion>4.0.0</modelVersion>\n" +
                            "  <groupId>org.apache.maven.plugin.site.test10</groupId>\n" +
                            "  <artifactId>site-plugin-test10</artifactId>\n" +
                            "  <version>1.0-SNAPSHOT</version>\n" +
                            "  <name>Maven Site Plugin Test10</name>\n" +
                            "</project>";

    public void setUp()
        throws Exception
    {
        Handler handler = new AbstractHandler()
        {
            public void handle( String target, HttpServletRequest request, HttpServletResponse response, int dispatch )
                throws IOException, ServletException
            {
                System.out.println( "Got request for URL: '" + request.getRequestURL() + "'" );
                System.out.flush();
                
                response.setContentType( "text/plain" );

                System.out.println( "Checking for 'Proxy-Connection' header..." );
                if ( request.getHeader( "Proxy-Connection" ) != null )
                {
                    response.setStatus( HttpServletResponse.SC_OK );
                    response.getWriter().println( content );
                    
                    System.out.println( "Proxy-Connection found." );
                }
                else
                {
                    response.setStatus( HttpServletResponse.SC_BAD_REQUEST );
                    
                    System.out.println( "Proxy-Connection not found." );
                }

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

    public void testmng3599useHttpProxyForHttp()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-3599-useHttpProxyForWebDAV" );

        String settings = FileUtils.fileRead( new File( testDir, "settings.xml.template" ) );
        settings = StringUtils.replace( settings, "@port@", Integer.toString( port ) );
        String newSettings = StringUtils.replace( settings, "@protocol@", "http" );
        
        FileUtils.fileWrite( new File( testDir, "settings.xml" ).getAbsolutePath(), newSettings );
        
        Verifier verifier = new Verifier( testDir.getAbsolutePath() );

        List cliOptions = new ArrayList();
        cliOptions.add( "--settings" );
        cliOptions.add( "settings.xml" );
        cliOptions.add( "-X" );
        
        verifier.setCliOptions( cliOptions );

        verifier.deleteArtifact( "org.apache.maven.its.mng3599", "test-dependency", "1.0", "jar" );
        verifier.deleteArtifact( "org.apache.maven.its.mng3599", "test-dependency", "1.0", "pom" );
        
        verifier.executeGoal( "compile" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        File logFile = new File( testDir, "log.txt" );
        logFile.renameTo( new File( testDir, "logHttp.txt" ) );

        verifier.assertArtifactPresent( "org.apache.maven.its.mng3599", "test-dependency", "1.0", "jar" );
        verifier.assertArtifactContents( "org.apache.maven.its.mng3599", "test-dependency", "1.0", "jar",
                                         content + LS );
    }

    /**
     * Test that HTTP proxy is used for HTTP and for WebDAV.
     */
    public void testmng3599useHttpProxyForWebDAV()
        throws Exception
    {
        // Doesn't work until 2.0.10+
        if ( matchesVersionRange( "(2.0.9, 2.99.99)" ) )
        {
            File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-3599-useHttpProxyForWebDAV" );

            String settings = FileUtils.fileRead( new File( testDir, "settings.xml.template" ) );
            settings = StringUtils.replace( settings, "@port@", Integer.toString( port ) );
            String newSettings = StringUtils.replace( settings, "@protocol@", "dav" );
            
            FileUtils.fileWrite( new File( testDir, "settings.xml" ).getAbsolutePath(), newSettings );

            Verifier verifier = new Verifier( testDir.getAbsolutePath() );

            List cliOptions = new ArrayList();
            cliOptions.add( "--settings" );
            cliOptions.add( "settings.xml" );
            cliOptions.add( "-X" );
            
            verifier.setCliOptions( cliOptions );

            verifier.deleteArtifact( "org.apache.maven.its.mng3599", "test-dependency", "1.0", "jar" );
            verifier.deleteArtifact( "org.apache.maven.its.mng3599", "test-dependency", "1.0", "pom" );
            
            verifier.executeGoal( "compile" );
            verifier.verifyErrorFreeLog();
            verifier.resetStreams();

            File logFile = new File( testDir, "log.txt" );
            logFile.renameTo( new File( testDir, "logDAV.txt" ) );

            verifier.assertArtifactPresent( "org.apache.maven.its.mng3599", "test-dependency", "1.0", "jar" );
            verifier.assertArtifactContents( "org.apache.maven.its.mng3599", "test-dependency", "1.0", "jar",
                                             content + LS );
        }
        else
        {
            System.out.print( " [skipping DAV test for Maven versions < 2.0.10 / 2.1 alpha]" );
        }
    }
}
