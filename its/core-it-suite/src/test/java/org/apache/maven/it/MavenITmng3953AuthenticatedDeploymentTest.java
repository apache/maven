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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.maven.it.util.ResourceExtractor;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.Request;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.AbstractHandler;
import org.mortbay.jetty.handler.HandlerList;
import org.mortbay.jetty.security.Constraint;
import org.mortbay.jetty.security.ConstraintMapping;
import org.mortbay.jetty.security.HashUserRealm;
import org.mortbay.jetty.security.SecurityHandler;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-3953">MNG-3953</a>.
 * 
 * @author Benjamin Bentmann
 * @version $Id$
 */
public class MavenITmng3953AuthenticatedDeploymentTest
    extends AbstractMavenIntegrationTestCase
{
    private Server server;

    private int port;

    private volatile boolean deployed;

    public MavenITmng3953AuthenticatedDeploymentTest()
    {
        super( "(2.0.1,)" );
    }

    @Override
    protected void setUp()
        throws Exception
    {
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
            }
        };

        Constraint constraint = new Constraint();
        constraint.setName( Constraint.__BASIC_AUTH );
        constraint.setRoles( new String[] { "deployer" } );
        constraint.setAuthenticate( true );

        ConstraintMapping constraintMapping = new ConstraintMapping();
        constraintMapping.setConstraint( constraint );
        constraintMapping.setPathSpec( "/*" );

        HashUserRealm userRealm = new HashUserRealm( "TestRealm" );
        userRealm.put( "testuser", "testtest" );
        userRealm.addUserToRole( "testuser", "deployer" );

        SecurityHandler securityHandler = new SecurityHandler();
        securityHandler.setUserRealm( userRealm );
        securityHandler.setConstraintMappings( new ConstraintMapping[] { constraintMapping } );

        HandlerList handlerList = new HandlerList();
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
     * Test that deployment (of a release) to a repository that requires authentification works.
     */
    public void testitRelease()
        throws Exception
    {
        testitMNG3953( "release" );
    }

    /**
     * Test that deployment (of a snapshot) to a repository that requires authentification works.
     */
    public void testitSnapshot()
        throws Exception
    {
        testitMNG3953( "snapshot" );
    }

    private void testitMNG3953( String project )
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-3953/" + project );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.addCliOption( "--settings" );
        verifier.addCliOption( "settings.xml" );
        verifier.addCliOption( "-DdeploymentPort=" + port );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        assertTrue( deployed );
    }

}
