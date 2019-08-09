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
import org.mortbay.jetty.HttpMethods;
import org.mortbay.jetty.Request;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.DefaultHandler;
import org.mortbay.jetty.handler.HandlerList;
import org.mortbay.jetty.handler.ResourceHandler;
import org.mortbay.jetty.security.Constraint;
import org.mortbay.jetty.security.ConstraintMapping;
import org.mortbay.jetty.security.HashUserRealm;
import org.mortbay.jetty.security.SecurityHandler;
import org.mortbay.resource.Resource;
import org.mortbay.util.IO;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4235">MNG-4235</a>.
 *
 * @author Benjamin Bentmann
 * @version $Id$
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

    @Override
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

        HashUserRealm userRealm = new HashUserRealm( "TestRealm" );
        userRealm.put( "testuser", "testpass" );
        userRealm.addUserToRole( "testuser", "deployer" );

        SecurityHandler securityHandler = new SecurityHandler();
        securityHandler.setUserRealm( userRealm );
        securityHandler.setConstraintMappings( new ConstraintMapping[]{ constraintMapping } );

        HandlerList handlerList = new HandlerList();
        handlerList.addHandler( securityHandler );
        handlerList.addHandler( repoHandler );
        handlerList.addHandler( new DefaultHandler() );

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
     * Test the creation of proper checksums during deployment to a secured HTTP repo. The pitfall with HTTP auth is
     * that it might require double submission of the data, first during an initial PUT without credentials and second
     * during a retried PUT with credentials in response to the auth challenge by the server. The checksum must
     * nevertheless only be calculated on the non-doubled data stream.
     */
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
                          + " use chuncked transfert encoding some http server doesn't support that" );
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


    public static class RepoHandler
        extends ResourceHandler
    {
        List<DeployedResource> deployedResources = new ArrayList<>();

        public void handle( String target, HttpServletRequest request, HttpServletResponse response, int dispatch )
            throws IOException, ServletException
        {
            System.out.println( request.getMethod() + " " + request.getRequestURI() );

            if ( HttpMethods.PUT.equals( request.getMethod() ) )
            {
                Resource resource = getResource( request );

                // NOTE: This can get called concurrently but File.mkdirs() isn't thread-safe in all JREs
                File dir = resource.getFile().getParentFile();
                for ( int i = 0; i < 10 && !dir.exists(); i++ )
                {
                    dir.mkdirs();
                }

                request.getContentLength();

                try ( OutputStream os = resource.getOutputStream() )
                {
                    IO.copy( request.getInputStream(), os );
                }

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
                super.handle( target, request, response, dispatch );
            }
        }
    }


}
