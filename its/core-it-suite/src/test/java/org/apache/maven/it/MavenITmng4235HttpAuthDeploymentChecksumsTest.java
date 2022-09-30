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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Deque;
import java.util.Properties;
import java.util.concurrent.ConcurrentLinkedDeque;

import org.apache.maven.it.utils.DeployedResource;
import org.codehaus.plexus.util.StringUtils;
import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.server.NetworkConnector;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.security.Constraint;
import org.eclipse.jetty.util.security.Password;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static org.eclipse.jetty.servlet.ServletContextHandler.SECURITY;
import static org.eclipse.jetty.servlet.ServletContextHandler.SESSIONS;
import static org.eclipse.jetty.util.security.Constraint.__BASIC_AUTH;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4235">MNG-4235</a>.
 *
 * @author Benjamin Bentmann
 *
 */
public class MavenITmng4235HttpAuthDeploymentChecksumsTest
    extends AbstractMavenIntegrationTestCase
{
    private File testDir;

    private Server server;

    private int port;

    private final RepoHandler repoHandler = new RepoHandler();

    public MavenITmng4235HttpAuthDeploymentChecksumsTest()
    {
        super( "[2.0.5,2.2.0),(2.2.0,)" );
    }

    @BeforeEach
    protected void setUp()
        throws Exception
    {
        testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-4235" );

        repoHandler.setResourceBase( testDir.getAbsolutePath() );

        Constraint constraint = new Constraint();
        constraint.setName( Constraint.__BASIC_AUTH );
        constraint.setRoles( new String[]{ "deployer" } );
        constraint.setAuthenticate( true );

        ConstraintMapping constraintMapping = new ConstraintMapping();
        constraintMapping.setConstraint( constraint );
        constraintMapping.setPathSpec( "/*" );

        HashLoginService userRealm = new HashLoginService( "TestRealm" );
        userRealm.putUser( "testuser", new Password( "testpass" ), new String[] { "deployer" } );

        ServletContextHandler ctx = new ServletContextHandler( server, "/", SESSIONS | SECURITY );
        ConstraintSecurityHandler securityHandler = (ConstraintSecurityHandler) ctx.getSecurityHandler();
        securityHandler.setLoginService( userRealm );
        securityHandler.setAuthMethod( __BASIC_AUTH );
        securityHandler.setConstraintMappings( new ConstraintMapping[] { constraintMapping } );

        HandlerList handlerList = new HandlerList();
        handlerList.addHandler( securityHandler );
        handlerList.addHandler( repoHandler );
        handlerList.addHandler( new DefaultHandler() );

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
    }

    /**
     * Test the creation of proper checksums during deployment to a secured HTTP repo. The pitfall with HTTP auth is
     * that it might require double submission of the data, first during an initial PUT without credentials and second
     * during a retried PUT with credentials in response to the auth challenge by the server. The checksum must
     * nevertheless only be calculated on the non-doubled data stream.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testit()
        throws Exception
    {
        Properties filterProps = new Properties();
        filterProps.setProperty( "@port@", Integer.toString( port ) );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.filterFile( "pom-template.xml", "pom.xml", "UTF-8", filterProps );
        verifier.setAutoclean( false );
        verifier.deleteArtifacts( "org.apache.maven.its.mng4235" );
        verifier.deleteDirectory( "repo" );
        verifier.addCliOption( "--settings" );
        verifier.addCliOption( "settings.xml" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        assertHash( verifier, "repo/org/apache/maven/its/mng4235/test/0.1/test-0.1.jar", ".sha1", "SHA-1" );
        assertHash( verifier, "repo/org/apache/maven/its/mng4235/test/0.1/test-0.1.jar", ".md5", "MD5" );

        assertHash( verifier, "repo/org/apache/maven/its/mng4235/test/0.1/test-0.1.pom", ".sha1", "SHA-1" );
        assertHash( verifier, "repo/org/apache/maven/its/mng4235/test/0.1/test-0.1.pom", ".md5", "MD5" );

        assertHash( verifier, "repo/org/apache/maven/its/mng4235/test/maven-metadata.xml", ".sha1", "SHA-1" );
        assertHash( verifier, "repo/org/apache/maven/its/mng4235/test/maven-metadata.xml", ".md5", "MD5" );

        for ( DeployedResource deployedResource : repoHandler.deployedResources )
        {
            if ( StringUtils.equalsIgnoreCase( "chunked", deployedResource.transferEncoding ) )
            {
                fail( "deployedResource " + deployedResource
                          + " use chunked transfert encoding some http server doesn't support that" );
            }
        }
    }

    private void assertHash( Verifier verifier, String dataFile, String hashExt, String algo )
        throws Exception
    {
        String actualHash = ItUtils.calcHash( new File( verifier.getBasedir(), dataFile ), algo );

        String expectedHash = verifier.loadLines( dataFile + hashExt, "UTF-8" ).get( 0 ).trim();

        assertTrue( "expected=" + expectedHash + ", actual=" + actualHash,
                    expectedHash.equalsIgnoreCase( actualHash ) );
    }

    private static class RepoHandler
            extends ResourceHandler
    {
        private final Deque<DeployedResource> deployedResources = new ConcurrentLinkedDeque<>();

        @Override
        public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException
        {
            System.out.println( request.getMethod() + " " + request.getRequestURI() );

            if ( "PUT".equals( request.getMethod() ) )
            {
                Resource resource = getResource( request );

                // NOTE: This can get called concurrently but File.mkdirs() isn't thread-safe in all JREs
                File dir = resource.getFile().getParentFile();
                for ( int i = 0; i < 10 && !dir.exists(); i++ )
                {
                    dir.mkdirs();
                }

                Files.copy( request.getInputStream(), resource.getFile().toPath(), REPLACE_EXISTING );

                DeployedResource deployedResource = new DeployedResource();

                deployedResource.httpMethod = request.getMethod();
                deployedResource.requestUri = request.getRequestURI();
                deployedResource.transferEncoding = request.getHeader( "Transfer-Encoding" );
                deployedResource.contentLength = request.getHeader( "Content-Length" );

                deployedResources.add( deployedResource );

                response.setStatus( HttpServletResponse.SC_NO_CONTENT );

                ( (Request) request ).setHandled( true );
            }
            else
            {
                super.handle( target, baseRequest, request, response );
            }
        }
    }
}
