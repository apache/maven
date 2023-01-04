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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Properties;

import org.eclipse.jetty.server.NetworkConnector;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-5280">MNG-5280</a>.
 *
 * @author Anders Hammar
 */
public class MavenITmng5280SettingsProfilesRepositoriesOrderTest
    extends AbstractMavenIntegrationTestCase
{
    private File testDir;

    private Server server;

    public MavenITmng5280SettingsProfilesRepositoriesOrderTest()
    {
        super( "[3.1-A,)" );
    }

    @BeforeEach
    protected void setUp()
        throws Exception
    {
        testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-5280" );
        server = new Server( 0 );
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
     * Verify that the repositories are used in the reversed order of definition in settings.xml.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testRepositoriesOrder()
        throws Exception
    {
        RepoHandler repoHandler = new RepoHandler();
        server.setHandler( repoHandler );
        server.start();
        if ( server.isFailed() )
        {
            fail( "Couldn't bind the server socket to a free port!" );
        }
        int httpPort = ( (NetworkConnector) server.getConnectors()[0] ).getLocalPort();
        System.out.println( "Bound server socket to the port " + httpPort );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );

        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.deleteArtifacts( "org.apache.maven.its.mng5280" );
        Properties filterProps = verifier.newDefaultFilterProperties();
        filterProps.setProperty( "@httpserver.port@", Integer.toString( httpPort ) );
        verifier.filterFile( "settings-template.xml", "settings.xml", "UTF-8", filterProps );
        verifier.addCliArgument( "--settings" );
        verifier.addCliArgument( "settings.xml" );
        verifier.addCliArgument( "org.apache.maven.its.plugins:maven-it-plugin-dependency-resolution:2.1-SNAPSHOT:compile" );
        verifier.execute();
        verifier.verifyErrorFreeLog();

        assertTrue( repoHandler.artifactRequestedFromRepo2 );
        assertTrue( repoHandler.artifactRequestedFromRepo1Last );
    }

    /**
     * Verify that the plugin repositories are used in the reversed order of definition in settings.xml.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testPluginRepositoriesOrder()
        throws Exception
    {
        PluginRepoHandler pluginRepoHandler = new PluginRepoHandler();
        server.setHandler( pluginRepoHandler );
        server.start();
        if ( server.isFailed() )
        {
            fail( "Couldn't bind the server socket to a free port!" );
        }
        int httpPort = ( (NetworkConnector) server.getConnectors()[0] ).getLocalPort();
        System.out.println( "Bound server socket to the port " + httpPort );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );

        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.deleteArtifacts( "org.apache.maven.its.mng5280" );
        Properties filterProps = verifier.newDefaultFilterProperties();
        filterProps.setProperty( "@httpserver.port@", Integer.toString( httpPort ) );
        verifier.filterFile( "settings-template.xml", "settings.xml", "UTF-8", filterProps );
        verifier.addCliArgument( "--settings" );
        verifier.addCliArgument( "settings.xml" );
        verifier.addCliArgument( "org.apache.maven.its.mng5280:fake-maven-plugin:1.0:fake" );
        verifier.execute();

        assertTrue( pluginRepoHandler.pluginRequestedFromRepo2 );
        assertTrue( pluginRepoHandler.pluginRequestedFromRepo1Last );
    }

    private static final class RepoHandler
        extends AbstractHandler
    {
        private volatile boolean artifactRequestedFromRepo1Last;

        private volatile boolean artifactRequestedFromRepo2;

        public void handle( String target, Request baseRequest, HttpServletRequest request,
                            HttpServletResponse response )
            throws IOException
        {
            String uri = request.getRequestURI();

            if ( uri.startsWith( "/repo1/org/apache/maven/its/mng5280/fake-artifact/1.0/" ) )
            {
                PrintWriter writer = response.getWriter();

                if ( uri.endsWith( ".pom" ) )
                {
                    writer.println( "<project>" );
                    writer.println( "  <modelVersion>4.0.0</modelVersion>" );
                    writer.println( "  <groupId>org.apache.maven.its.mng5280</groupId>" );
                    writer.println( "  <artifactId>fake-artifact</artifactId>" );
                    writer.println( "  <version>1.0</version>" );
                    writer.println( "</project>" );

                    response.setStatus( HttpServletResponse.SC_OK );
                }
                else if ( uri.endsWith( ".jar" ) )
                {
                    writer.println( "empty" );

                    response.setStatus( HttpServletResponse.SC_OK );
                    artifactRequestedFromRepo1Last = true;
                }
                else
                {
                    response.setStatus( HttpServletResponse.SC_NOT_FOUND );
                }
            }
            else if ( uri.startsWith( "/repo2/org/apache/maven/its/mng5280/fake-artifact/1.0/" ) )
            {
                if ( uri.endsWith( ".jar" ) )
                {
                    artifactRequestedFromRepo1Last = false;
                    artifactRequestedFromRepo2 = true;
                }
                response.setStatus( HttpServletResponse.SC_NOT_FOUND );
            }
            else
            {
                response.setStatus( HttpServletResponse.SC_NOT_FOUND );
            }

            ( (Request) request ).setHandled( true );
        }
    }

    private class PluginRepoHandler
        extends AbstractHandler
    {
        private volatile boolean pluginRequestedFromRepo1Last;

        private volatile boolean pluginRequestedFromRepo2;

        public void handle( String target, Request baseRequest, HttpServletRequest request,
                            HttpServletResponse response )
            throws IOException
        {
            String uri = request.getRequestURI();

            if ( uri.startsWith( "/pluginRepo1/org/apache/maven/its/mng5280/fake-maven-plugin/1.0/" ) )
            {
                OutputStream outStream = response.getOutputStream();

                if ( uri.endsWith( ".pom" ) )
                {
                    File pluginPom = new File( testDir, "fake-maven-plugin/fake-maven-plugin-1.0.pom" );
                    InputStream inStream = new FileInputStream( pluginPom );
                    copy( inStream, outStream );

                    response.setStatus( HttpServletResponse.SC_OK );
                }
                else if ( uri.endsWith( ".jar" ) )
                {
                    File pluginJar = new File( testDir, "fake-maven-plugin/fake-maven-plugin-1.0.jar" );
                    InputStream inStream = new FileInputStream( pluginJar );
                    copy( inStream, outStream );

                    response.setStatus( HttpServletResponse.SC_OK );
                    pluginRequestedFromRepo1Last = true;
                }
                else
                {
                    response.setStatus( HttpServletResponse.SC_NOT_FOUND );
                }
            }
            else if ( uri.startsWith( "/pluginRepo2/org/apache/maven/its/mng5280/fake-maven-plugin/1.0/" ) )
            {
                if ( uri.endsWith( ".jar" ) )
                {
                    pluginRequestedFromRepo1Last = false;
                    pluginRequestedFromRepo2 = true;
                }
                response.setStatus( HttpServletResponse.SC_NOT_FOUND );
            }
            else
            {
                response.setStatus( HttpServletResponse.SC_NOT_FOUND );
            }

            ( (Request) request ).setHandled( true );
        }

        private long copy( InputStream input, OutputStream output )
            throws IOException
        {
            byte[] buffer = new byte[4 * 1024];
            long count = 0;
            int n = 0;
            while ( -1 != ( n = input.read( buffer ) ) )
            {
                output.write( buffer, 0, n );
                count += n;
            }
            return count;
        }
    }
}
