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

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.mortbay.jetty.Handler;
import org.mortbay.jetty.Request;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.AbstractHandler;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-3461">MNG-3461</a>.
 * 
 * @author Benjamin Bentmann
 * @version $Id$
 */
public class MavenITmng3461MirrorMatchingTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng3461MirrorMatchingTest()
    {
        super( "(2.0.8,)" );
    }

    /**
     * Test that mirror definitions are properly evaluated. In particular, an exact match by id should always
     * win over wildcard matches.
     */
    public void testitExactMatchDominatesWildcard()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-3461/test-1" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteArtifacts( "org.apache.maven.its.mng3461" );
        Properties filterProps = verifier.newDefaultFilterProperties();
        verifier.filterFile( "settings-template.xml", "settings.xml", "UTF-8", filterProps );
        verifier.addCliOption( "--settings" );
        verifier.addCliOption( "settings.xml" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        verifier.assertArtifactPresent( "org.apache.maven.its.mng3461", "a", "0.1", "jar" );
    }

    /**
     * Test that mirror definitions are properly evaluated. In particular, the wildcard external:* should not
     * match file:// and localhost repos but only external repos.
     */
    public void testitExternalWildcard()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-3461/test-2" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );

        Handler repoHandler = new AbstractHandler()
        {
            public void handle( String target, HttpServletRequest request, HttpServletResponse response, int dispatch )
                throws IOException
            {
                System.out.println( "Handling " + request.getMethod() + " " + request.getRequestURL() );

                if ( request.getRequestURI().endsWith( "/b-0.1.jar" ) )
                {
                    response.setStatus( HttpServletResponse.SC_OK );
                    response.getWriter().println( request.getRequestURI() );
                }
                else if ( request.getRequestURI().endsWith( "/b-0.1.pom" ) )
                {
                    response.setStatus( HttpServletResponse.SC_OK );
                    response.getWriter().println( "<project>" );
                    response.getWriter().println( "  <modelVersion>4.0.0</modelVersion>" );
                    response.getWriter().println( "  <groupId>org.apache.maven.its.mng3461</groupId>" );
                    response.getWriter().println( "  <artifactId>b</artifactId>" );
                    response.getWriter().println( "  <version>0.1</version>" );
                    response.getWriter().println( "</project>" );
                }
                else
                {
                    response.setStatus( HttpServletResponse.SC_NOT_FOUND );
                }

                ( (Request) request ).setHandled( true );
            }
        };

        Server server = new Server( 0 );
        server.setHandler( repoHandler );

        try
        {
            server.start();
            while ( !server.isRunning() || !server.isStarted() )
            {
                if ( server.isFailed() )
                {
                    fail( "Couldn't bind the server socket to a free port!" );
                }
                Thread.sleep( 100L );
            }

            int port = server.getConnectors()[0].getLocalPort();
            System.out.println( "Bound server socket to the port " + port );

            verifier.setAutoclean( false );
            verifier.deleteArtifacts( "org.apache.maven.its.mng3461" );
            Properties filterProps = verifier.newDefaultFilterProperties();
            filterProps.setProperty( "@test.port@", Integer.toString( port ) );
            verifier.filterFile( "settings-template.xml", "settings.xml", "UTF-8", filterProps );
            verifier.addCliOption( "--settings" );
            verifier.addCliOption( "settings.xml" );
            verifier.executeGoal( "validate" );
            verifier.verifyErrorFreeLog();
            verifier.resetStreams();
        }
        finally
        {
            server.stop();
            server.join();
        }

        verifier.assertArtifactPresent( "org.apache.maven.its.mng3461", "a", "0.1", "jar" );
        verifier.assertArtifactPresent( "org.apache.maven.its.mng3461", "b", "0.1", "jar" );
        verifier.assertArtifactPresent( "org.apache.maven.its.mng3461", "c", "0.1", "jar" );
    }

    /**
     * Test that mirror definitions are properly evaluated. In particular, the wildcards within a single mirrorOf
     * spec should not be greedy.
     */
    public void testitNonGreedyWildcard()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-3461/test-3" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteArtifacts( "org.apache.maven.its.mng3461" );
        Properties filterProps = verifier.newDefaultFilterProperties();
        verifier.filterFile( "settings-template.xml", "settings.xml", "UTF-8", filterProps );
        verifier.addCliOption( "--settings" );
        verifier.addCliOption( "settings.xml" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        verifier.assertArtifactPresent( "org.apache.maven.its.mng3461", "a", "0.1", "jar" );
    }
}
