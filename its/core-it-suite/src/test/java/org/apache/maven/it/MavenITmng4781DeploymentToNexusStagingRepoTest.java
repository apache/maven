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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.Request;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.AbstractHandler;
import org.mortbay.jetty.handler.HandlerList;

/**
 * This is a test set for <a href="http://jira.codehaus.org/browse/MNG-4781">MNG-4781</a>.
 * 
 * @author Benjamin Bentmann
 */
public class MavenITmng4781DeploymentToNexusStagingRepoTest
    extends AbstractMavenIntegrationTestCase
{

    private Server server;

    private int port;

    private List<String> requestedUris = Collections.synchronizedList( new ArrayList<String>() );

    private List<String> deployedUris = Collections.synchronizedList( new ArrayList<String>() );

    public MavenITmng4781DeploymentToNexusStagingRepoTest()
    {
        super( "[2.0.3,)" );
    }

    public void setUp()
        throws Exception
    {
        super.setUp();

        Handler repoHandler = new AbstractHandler()
        {

            private boolean putSeen;

            public void handle( String target, HttpServletRequest request, HttpServletResponse response, int dispatch )
                throws IOException, ServletException
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

        port = server.getConnectors()[0].getLocalPort();
    }

    protected void tearDown()
        throws Exception
    {
        if ( server != null )
        {
            server.stop();
            server = null;
        }

        requestedUris.clear();
        deployedUris.clear();

        super.tearDown();
    }

    /**
     * Verify that deployment to a Nexus-like staging repository works. The subtle difference compared to an ordinary
     * HTTP/WebDAV server is that those staging repos yield a HTTP 400 (and not 404) for every GET request until a
     * PUT request is made (which initializes the staging repo). The bottom line is that remote metadata must not be
     * requested before the first artifact is deployed.
     */
    public void testit()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-4781" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.addCliOption( "-DdeploymentPort=" + port );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        assertTrue( deployedUris.toString(), 
            deployedUris.contains( "/repo/org/apache/maven/its/mng4781/release/1.0/release-1.0.jar" ) );
        assertTrue( deployedUris.toString(), 
            deployedUris.contains( "/repo/org/apache/maven/its/mng4781/release/1.0/release-1.0.pom" ) );
        assertTrue( deployedUris.toString(), 
            deployedUris.contains( "/repo/org/apache/maven/its/mng4781/release/maven-metadata.xml" ) );

        assertTrue( requestedUris.toString(), 
            deployedUris.contains( "/repo/org/apache/maven/its/mng4781/release/maven-metadata.xml" ) );
    }

}
