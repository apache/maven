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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.File;
import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.NetworkConnector;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4781">MNG-4781</a>.
 *
 * @author Benjamin Bentmann
 */
public class MavenITmng4781DeploymentToNexusStagingRepoTest
    extends AbstractMavenIntegrationTestCase
{
    private Server server;

    private int port;

    private final Deque<String> requestedUris = new ConcurrentLinkedDeque<>();

    private final Deque<String> deployedUris = new ConcurrentLinkedDeque<>();

    public MavenITmng4781DeploymentToNexusStagingRepoTest()
    {
        super( "[2.0.3,)" );
    }

    @BeforeEach
    public void setUp()
        throws Exception
    {
        Handler repoHandler = new AbstractHandler()
        {
            private volatile boolean putSeen;

            @Override
            public void handle( String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response )
            {
                System.out.println( "Handling " + request.getMethod() + " " + request.getRequestURL() );

                if ( "PUT".equalsIgnoreCase( request.getMethod() ) )
                {
                    response.setStatus( HttpServletResponse.SC_CREATED );
                    deployedUris.add( request.getRequestURI() );
                    putSeen = true;
                }
                else if ( !putSeen )
                {
                    response.setStatus( HttpServletResponse.SC_BAD_REQUEST );
                }
                else
                {
                    response.setStatus( HttpServletResponse.SC_NOT_FOUND );
                    requestedUris.add( request.getRequestURI() );
                }

                ( (Request) request ).setHandled( true );
            }
        };

        HandlerList handlerList = new HandlerList();
        handlerList.addHandler( repoHandler );

        server = new Server( 0 );
        server.setHandler( handlerList );
        server.start();
        if ( server.isFailed() )
        {
            fail( "Couldn't bind the server socket to a free port!" );
        }
        port = ( (NetworkConnector) server.getConnectors()[0] ).getLocalPort();
        System.out.println( "Bound server socket to the port " + port );
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

        requestedUris.clear();
        deployedUris.clear();
    }

    /**
     * Verify that deployment to a Nexus-like staging repository works. The subtle difference compared to an ordinary
     * HTTP/WebDAV server is that those staging repos yield a HTTP 400 (and not 404) for every GET request until a
     * PUT request is made (which initializes the staging repo). The bottom line is that remote metadata must not be
     * requested before the first artifact is deployed.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testit()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-4781" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.addCliArgument( "-DdeploymentPort=" + port );
        verifier.addCliArgument( "validate" );
        verifier.execute();
        verifier.verifyErrorFreeLog();

        assertTrue( deployedUris.contains( "/repo/org/apache/maven/its/mng4781/release/1.0/release-1.0.jar" ) );
        assertTrue( deployedUris.contains( "/repo/org/apache/maven/its/mng4781/release/1.0/release-1.0.pom" ) );
        assertTrue( deployedUris.contains( "/repo/org/apache/maven/its/mng4781/release/maven-metadata.xml" ) );
        assertTrue( requestedUris.contains( "/repo/org/apache/maven/its/mng4781/release/maven-metadata.xml" ) );
    }
}
