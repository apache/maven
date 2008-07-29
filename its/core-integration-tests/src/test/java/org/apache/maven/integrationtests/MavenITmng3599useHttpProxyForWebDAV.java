package org.apache.maven.integrationtests;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.FileUtils;
import org.apache.maven.it.util.StringUtils;
import org.apache.maven.it.util.ResourceExtractor;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.Request;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.AbstractHandler;

public class MavenITmng3599useHttpProxyForWebDAV
    extends AbstractMavenIntegrationTestCase
{
    private Server server;

    private int port;

    public void setUp()
        throws Exception
    {
        Handler handler = new AbstractHandler()
        {   
            public void handle( String target, HttpServletRequest request, HttpServletResponse response, int dispatch )
                throws IOException, ServletException
            {
                response.setContentType( "text/plain" );

                if ( request.getHeader( "Proxy-Connection" ) != null )
                {
                    response.setStatus( HttpServletResponse.SC_OK );
                    response.getWriter().println( "some content" );
                }
                else
                {
                    response.setStatus( HttpServletResponse.SC_NOT_FOUND );
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

    /**
     * Test that HTTP proxy is used for HTTP and for WebDAV.
     */
    public void testmng3599useHttpProxyForWebDAV()
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
        verifier.setCliOptions( cliOptions );

        verifier.deleteArtifact( "org.apache.maven.its.mng3599", "test-dependency", "1.0", "jar" );
        verifier.executeGoal( "compile" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        verifier.assertArtifactPresent( "org.apache.maven.its.mng3599", "test-dependency", "1.0", "jar" );
        verifier.assertArtifactContents( "org.apache.maven.its.mng3599", "test-dependency", "1.0", "jar", "some content\n" );

        // Doesn't work until 2.0.11+
        // TODO: reinstate for 2.1 when WebDAV works
        if ( matchesVersionRange( "(2.0.10,2.0.99)" ) )
        {
            newSettings = StringUtils.replace( settings, "@protocol@", "dav" );
            FileUtils.fileWrite( new File( testDir, "settings.xml" ).getAbsolutePath(), newSettings );

            verifier.deleteArtifact( "org.apache.maven.its.mng3599", "test-dependency", "1.0", "jar" );
            verifier.executeGoal( "compile" );
            verifier.verifyErrorFreeLog();
            verifier.resetStreams();

            verifier.assertArtifactPresent( "org.apache.maven.its.mng3599", "test-dependency", "1.0", "jar" );
            verifier.assertArtifactContents( "org.apache.maven.its.mng3599", "test-dependency", "1.0", "jar", "some content\n" );
        }
        else
        {
            System.out.print( " [skipping DAV test for < 2.0.10 / 2.1 alpha]" );
        }
    }
}

