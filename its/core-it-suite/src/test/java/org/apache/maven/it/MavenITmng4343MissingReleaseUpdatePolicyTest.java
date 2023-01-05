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

import org.apache.maven.shared.verifier.util.ResourceExtractor;
import org.apache.maven.shared.verifier.Verifier;
import org.apache.maven.shared.verifier.VerificationException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.NetworkConnector;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4343">MNG-4343</a>.
 *
 * @author Benjamin Bentmann
 *
 */
public class MavenITmng4343MissingReleaseUpdatePolicyTest
    extends AbstractMavenIntegrationTestCase
{
    private Server server;

    private Deque<String> requestedUris;

    private volatile boolean blockAccess;

    private int port;

    public MavenITmng4343MissingReleaseUpdatePolicyTest()
    {
        super( "[3.0-alpha-3,)" );
    }

    @BeforeEach
    protected void setUp()
        throws Exception
    {
        Handler repoHandler = new AbstractHandler()
        {
            @Override
            public void handle( String target, Request baseRequest, HttpServletRequest request,
                                HttpServletResponse response )
                throws IOException
            {
                System.out.println( "Handling " + request.getMethod() + " " + request.getRequestURL() );

                if ( request.getRequestURI().startsWith( "/org/apache/maven/its/mng4343" ) )
                {
                    requestedUris.add( request.getRequestURI().substring( 29 ) );
                }

                if ( blockAccess )
                {
                    response.setStatus( HttpServletResponse.SC_NOT_FOUND );
                }
                else
                {
                    PrintWriter writer = response.getWriter();

                    response.setStatus( HttpServletResponse.SC_OK );

                    if ( request.getRequestURI().endsWith( ".pom" ) )
                    {
                        writer.println( "<project>" );
                        writer.println( "  <modelVersion>4.0.0</modelVersion>" );
                        writer.println( "  <groupId>org.apache.maven.its.mng4343</groupId>" );
                        writer.println( "  <artifactId>dep</artifactId>" );
                        writer.println( "  <version>0.1</version>" );
                        writer.println( "</project>" );
                    }
                    else if ( request.getRequestURI().endsWith( ".jar" ) )
                    {
                        writer.println( "empty" );
                    }
                    else if ( request.getRequestURI().endsWith( ".md5" ) || request.getRequestURI().endsWith( ".sha1" ) )
                    {
                        response.setStatus( HttpServletResponse.SC_NOT_FOUND );
                    }
                }

                ( (Request) request ).setHandled( true );
            }
        };

        server = new Server( 0 );
        server.setHandler( repoHandler );
        server.start();
        if ( server.isFailed() )
        {
            fail( "Couldn't bind the server socket to a free port!" );
        }
        port = ( (NetworkConnector) server.getConnectors()[0] ).getLocalPort();
        System.out.println( "Bound server socket to the port " + port );
        requestedUris = new ConcurrentLinkedDeque<>();
    }

    @AfterEach
    protected void tearDown()
        throws Exception
    {
        if ( server != null )
        {
            server.stop();
            server.join();
        }
    }

    /**
     * Verify that checking for *missing* release artifacts respects the update policy that is configured in the
     * release section for the respective repository, in this case "always".
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitAlways()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-4343" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteArtifacts( "org.apache.maven.its.mng4343" );
        verifier.addCliArgument( "-s" );
        verifier.addCliArgument( "settings.xml" );

        Map<String, String> filterProps = verifier.newDefaultFilterMap();
        filterProps.put( "@updates@", "always" );
        filterProps.put( "@port@", Integer.toString( port ) );
        verifier.filterFile( "settings-template.xml", "settings.xml", "UTF-8", filterProps );

        blockAccess = true;

        verifier.setLogFileName( "log-always-1.txt" );
        try
        {
            verifier.addCliArgument( "validate" );
            verifier.execute();
            verifier.verifyErrorFreeLog();
            fail( "Build succeeded despite missing dependency" );
        }
        catch ( VerificationException e )
        {
            // expected
        }

        assertTrue( requestedUris.toString(),
            requestedUris.contains( "/dep/0.1/dep-0.1.jar" ) || requestedUris.contains( "/dep/0.1/dep-0.1.pom" ) );
        requestedUris.clear();

        blockAccess = false;

        verifier.setLogFileName( "log-always-2.txt" );
        verifier.addCliArgument( "validate" );
        verifier.execute();
        verifier.verifyErrorFreeLog();

        assertTrue( requestedUris.toString(), requestedUris.contains( "/dep/0.1/dep-0.1.jar" ) );
        assertTrue( requestedUris.toString(), requestedUris.contains( "/dep/0.1/dep-0.1.pom" ) );
        verifier.verifyArtifactPresent( "org.apache.maven.its.mng4343", "dep", "0.1", "jar" );
        verifier.verifyArtifactPresent( "org.apache.maven.its.mng4343", "dep", "0.1", "pom" );

    }

    /**
     * Verify that checking for *missing* release artifacts respects the update policy that is configured in the
     * release section for the respective repository, in this case "never", unless overridden from the CLI via -U.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitNever()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-4343" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteArtifacts( "org.apache.maven.its.mng4343" );
        verifier.addCliArgument( "-s" );
        verifier.addCliArgument( "settings.xml" );

        Map<String, String> filterProps = verifier.newDefaultFilterMap();
        filterProps.put( "@updates@", "never" );
        filterProps.put( "@port@", Integer.toString( port ) );
        verifier.filterFile( "settings-template.xml", "settings.xml", "UTF-8", filterProps );

        blockAccess = true;

        verifier.setLogFileName( "log-never-1.txt" );
        try
        {
            verifier.addCliArgument( "validate" );
            verifier.execute();
            verifier.verifyErrorFreeLog();
            fail( "Build succeeded despite missing dependency" );
        }
        catch ( VerificationException e )
        {
            // expected
        }

        assertTrue( requestedUris.toString(),
            requestedUris.contains( "/dep/0.1/dep-0.1.jar" ) || requestedUris.contains( "/dep/0.1/dep-0.1.pom" ) );
        requestedUris.clear();

        blockAccess = false;

        verifier.setLogFileName( "log-never-2.txt" );
        try
        {
            verifier.addCliArgument( "validate" );
            verifier.execute();
            verifier.verifyErrorFreeLog();
            fail( "Remote repository was accessed despite updatePolicy=never" );
        }
        catch ( VerificationException e )
        {
            // expected
        }

        //noinspection unchecked
        assertTrue( requestedUris.isEmpty() );
        verifier.verifyArtifactNotPresent( "org.apache.maven.its.mng4343", "dep", "0.1", "jar" );
        verifier.verifyArtifactNotPresent( "org.apache.maven.its.mng4343", "dep", "0.1", "pom" );

        verifier.setLogFileName( "log-never-3.txt" );
        verifier.addCliArgument( "-U" );
        verifier.addCliArgument( "validate" );
        verifier.execute();
        verifier.verifyErrorFreeLog();

        assertTrue( requestedUris.contains( "/dep/0.1/dep-0.1.jar" ) );
        assertTrue( requestedUris.contains( "/dep/0.1/dep-0.1.pom" ) );
        verifier.verifyArtifactPresent( "org.apache.maven.its.mng4343", "dep", "0.1", "jar" );
        verifier.verifyArtifactPresent( "org.apache.maven.its.mng4343", "dep", "0.1", "pom" );

        requestedUris.clear();

        verifier.setLogFileName( "log-never-4.txt" );
        verifier.addCliArgument( "-U" );
        verifier.addCliArgument( "validate" );
        verifier.execute();
        verifier.verifyErrorFreeLog();

        //noinspection unchecked
        assertTrue( requestedUris.isEmpty() );

    }

}
