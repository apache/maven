package org.apache.maven.it;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.File;
import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.maven.it.util.ResourceExtractor;
import org.apache.maven.shared.utils.StringUtils;
import org.apache.maven.shared.utils.io.FileUtils;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.NetworkConnector;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-3599">MNG-3599</a>.
 * 
 * @author Brett Porter
 * @author John Casey
 * @version $Id$
 */
public class MavenITmng3599useHttpProxyForWebDAVTest
    extends AbstractMavenIntegrationTestCase
{
    private static final String LS = System.getProperty( "line.separator" );
    
    private Server server;

    private int port;

    private static final String content = "<project xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                            "  xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd\">\n" +
                            "  <modelVersion>4.0.0</modelVersion>\n" +
                            "  <groupId>org.apache.maven.its.mng3599</groupId>\n" +
                            "  <artifactId>test</artifactId>\n" +
                            "  <version>1.0-SNAPSHOT</version>\n" +
                            "  <name>MNG-3599</name>\n" +
                            "</project>";

    public MavenITmng3599useHttpProxyForWebDAVTest()
    {
        super( "(2.0.9,3.3.9)" );
    }

    @Override
    protected void setUp()
        throws Exception
    {
        Handler handler = new AbstractHandler()
        {
            @Override
            public void handle( String target, Request baseRequest, HttpServletRequest request,
                                HttpServletResponse response )
                throws IOException
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
                /*
                 * 2008-09-29 Oleg: "Proxy-Connection" is not part of http spec, but an extended header, and 
                 * as such cannot be expected from all the clients.
                 * Changing the code to test for more generalized case: local proxy receives a request with
                 * correct server url and resource uri
                 */
                else if( 
                    request.getRequestURI().startsWith( "/org/apache/maven/its/mng3599/test-dependency" )
                    && request.getRequestURL().toString().startsWith( "http://www.example.com" )
                )
                {
                    response.setStatus( HttpServletResponse.SC_OK );
                    response.getWriter().println( content );
                    
                    System.out.println( "Correct proxied request 'http://www.example.com' for resource '/org/apache/maven/its/mng3599/test-dependency' found." );
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
        if ( server.isFailed() )
        {
            fail( "Couldn't bind the server socket to a free port!" );
        }
        port = ( (NetworkConnector) server.getConnectors()[0] ).getLocalPort();
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

    public void testitUseHttpProxyForHttp()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-3599" );

        /*
         * NOTE: Make sure the WebDAV extension required by the test project has been pulled down into the local
         * repo before the actual test installs Jetty as a mirror for everything. Otherwise, we will get garbage
         * for the JAR/POM of the extension and its dependencies when run against a vanilla repo.
         */
        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        String settings = FileUtils.fileRead( new File( testDir, "settings-template.xml" ) );
        settings = StringUtils.replace( settings, "@port@", Integer.toString( port ) );
        String newSettings = StringUtils.replace( settings, "@protocol@", "http" );
        
        FileUtils.fileWrite( new File( testDir, "settings.xml" ).getAbsolutePath(), newSettings );
        
        verifier = newVerifier( testDir.getAbsolutePath() );

        verifier.addCliOption( "--settings" );
        verifier.addCliOption( "settings.xml" );
        verifier.addCliOption( "-X" );

        verifier.deleteArtifacts( "org.apache.maven.its.mng3599" );

        verifier.setLogFileName( "logHttp.txt" );
        verifier.executeGoal( "compile" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        verifier.assertArtifactPresent( "org.apache.maven.its.mng3599", "test-dependency", "1.0", "jar" );
        verifier.assertArtifactContents( "org.apache.maven.its.mng3599", "test-dependency", "1.0", "jar",
                                         content + LS );
    }

    /**
     * Test that HTTP proxy is used for HTTP and for WebDAV.
     */
    public void testitUseHttpProxyForWebDAV()
        throws Exception
    {
        requiresMavenVersion( "[2.1.0-M1,3.0-alpha-1),[3.0-beta-3,3.3.9)" );

        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-3599" );

        /*
         * NOTE: Make sure the WebDAV extension required by the test project has been pulled down into the local
         * repo before the actual test installs Jetty as a mirror for everything. Otherwise, we will get garbage
         * for the JAR/POM of the extension and its dependencies when run against a vanilla repo.
         */
        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        String settings = FileUtils.fileRead( new File( testDir, "settings-template.xml" ) );
        settings = StringUtils.replace( settings, "@port@", Integer.toString( port ) );
        String newSettings = StringUtils.replace( settings, "@protocol@", "dav" );
        
        FileUtils.fileWrite( new File( testDir, "settings.xml" ).getAbsolutePath(), newSettings );

        verifier = newVerifier( testDir.getAbsolutePath() );

        verifier.addCliOption( "--settings" );
        verifier.addCliOption( "settings.xml" );
        verifier.addCliOption( "-X" );

        verifier.deleteArtifacts( "org.apache.maven.its.mng3599" );

        verifier.setLogFileName( "logDAV.txt" );
        verifier.executeGoal( "compile" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        verifier.assertArtifactPresent( "org.apache.maven.its.mng3599", "test-dependency", "1.0", "jar" );
        verifier.assertArtifactContents( "org.apache.maven.its.mng3599", "test-dependency", "1.0", "jar",
                                         content + LS );
    }
}
