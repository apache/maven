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

import org.apache.maven.it.util.ResourceExtractor;
import org.apache.maven.it.utils.DeployedResource;
import org.codehaus.plexus.util.StringUtils;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.Request;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.AbstractHandler;
import org.mortbay.jetty.handler.HandlerList;
import org.mortbay.jetty.security.B64Code;
import org.mortbay.jetty.security.Constraint;
import org.mortbay.jetty.security.ConstraintMapping;
import org.mortbay.jetty.security.HashUserRealm;
import org.mortbay.jetty.security.SecurityHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.util.Collections;
import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4470">MNG-4470</a>.
 *
 * @author Benjamin Bentmann
 * @version $Id$
 */
public class MavenITmng4470AuthenticatedDeploymentToProxyTest
    extends AbstractMavenIntegrationTestCase
{
    private Server server;

    private int port;

    private volatile boolean deployed;

    private final Deque<DeployedResource> deployedResources = new ConcurrentLinkedDeque<>();

    public MavenITmng4470AuthenticatedDeploymentToProxyTest()
    {
        super( "[2.0.3,3.0-alpha-1),[3.0-alpha-6,)" );
    }

    @Override
    protected void setUp()
        throws Exception
    {
        Handler proxyHandler = new AbstractHandler()
        {
            public void handle( String target, HttpServletRequest request, HttpServletResponse response, int dispatch )
            {
                System.out.println( "Handling " + request.getMethod() + " " + request.getRequestURL() );

                String auth = request.getHeader( "Proxy-Authorization" );
                if ( auth != null )
                {
                    auth = auth.substring( auth.indexOf( ' ' ) + 1 ).trim();
                    auth = B64Code.decode( auth );
                }
                System.out.println( "Proxy-Authorization: " + auth );

                if ( !"proxyuser:proxypass".equals( auth ) )
                {
                    response.setStatus( HttpServletResponse.SC_PROXY_AUTHENTICATION_REQUIRED );
                    response.addHeader( "Proxy-Authenticate", "Basic realm=\"Squid proxy-caching web server\"" );
                    ( (Request) request ).setHandled( true );
                }

                DeployedResource deployedResource = new DeployedResource();

                deployedResource.httpMethod = request.getMethod();
                deployedResource.requestUri = request.getRequestURI();
                deployedResource.transferEncoding = request.getHeader( "Transfer-Encoding" );
                deployedResource.contentLength = request.getHeader( "Content-Length" );

                deployedResources.add( deployedResource );
            }
        };

        Handler repoHandler = new AbstractHandler()
        {
            public void handle( String target, HttpServletRequest request, HttpServletResponse response, int dispatch )
            {
                System.out.println( "Handling " + request.getMethod() + " " + request.getRequestURL() );

                if ( "PUT".equalsIgnoreCase( request.getMethod() ) )
                {
                    response.setStatus( HttpServletResponse.SC_OK );
                    deployed = true;
                }
                else
                {
                    response.setStatus( HttpServletResponse.SC_NOT_FOUND );
                }

                ( (Request) request ).setHandled( true );

                DeployedResource deployedResource = new DeployedResource();

                deployedResource.httpMethod = request.getMethod();
                deployedResource.requestUri = request.getRequestURI();
                deployedResource.transferEncoding = request.getHeader( "Transfer-Encoding" );
                deployedResource.contentLength = request.getHeader( "Content-Length" );

                deployedResources.add( deployedResource );
            }
        };

        Constraint constraint = new Constraint();
        constraint.setName( Constraint.__BASIC_AUTH );
        constraint.setRoles( new String[]{ "deployer" } );
        constraint.setAuthenticate( true );

        ConstraintMapping constraintMapping = new ConstraintMapping();
        constraintMapping.setConstraint( constraint );
        constraintMapping.setPathSpec( "/*" );

        HashUserRealm userRealm = new HashUserRealm( "TestRealm" );
        userRealm.put( "testuser", "testtest" );
        userRealm.addUserToRole( "testuser", "deployer" );

        SecurityHandler securityHandler = new SecurityHandler();
        securityHandler.setUserRealm( userRealm );
        securityHandler.setConstraintMappings( new ConstraintMapping[]{ constraintMapping } );

        HandlerList handlerList = new HandlerList();
        handlerList.addHandler( proxyHandler );
        handlerList.addHandler( securityHandler );
        handlerList.addHandler( repoHandler );

        server = new Server( 0 );
        server.setHandler( handlerList );
        server.start();
        while ( !server.isRunning() || !server.isStarted() )
        {
            if ( server.isFailed() )
            {
                fail( "Couldn't bind the server socket to a free port!" );
            }
            Thread.sleep( 100L );
        }
        port = server.getConnectors()[0].getLocalPort();
        System.out.println( "Bound server socket to the port " + port );
        deployed = false;
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

    /**
     * Test that deployment (of a release) to a proxy that requires authentication works.
     */
    public void testitRelease()
        throws Exception
    {
        testit( "release" );
    }

    /**
     * Test that deployment (of a snapshot) to a proxy that requires authentication works.
     */
    public void testitSnapshot()
        throws Exception
    {
        testit( "snapshot" );
    }

    private void testit( String project )
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-4470/" + project );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.filterFile( "settings-template.xml", "settings.xml", "UTF-8",
                             Collections.singletonMap( "@port@", Integer.toString( port ) ) );
        verifier.addCliOption( "--settings" );
        verifier.addCliOption( "settings.xml" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        for ( DeployedResource deployedResource : deployedResources )
        {
            if ( StringUtils.equalsIgnoreCase( "chunked", deployedResource.transferEncoding ) )
            {
                fail( "deployedResource " + deployedResource
                          + " use chuncked transfert encoding some http server doesn't support that" );
            }
        }

        assertTrue( deployed );
    }
}
