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
import java.io.PrintWriter;
import java.util.List;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Request;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.AbstractHandler;
import org.mortbay.jetty.security.SslSocketConnector;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4428">MNG-4428</a>.
 * 
 * @author Benjamin Bentmann
 * @version $Id$
 */
public class MavenITmng4428FollowHttpRedirectTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng4428FollowHttpRedirectTest()
    {
        super( "[2.0.3,3.0-alpha-1),(3.0-alpha-1,)" );
    }

    /**
     * Verify that redirects from HTTP to HTTP are getting followed.
     */
    public void testitHttpToHttp()
        throws Exception
    {
        testit( true, true );
    }

    /**
     * Verify that redirects from HTTPS to HTTPS are getting followed.
     */
    public void testitHttpsToHttps()
        throws Exception
    {
        testit( false, false );
    }

    /**
     * Verify that redirects from HTTP to HTTPS are getting followed.
     */
    public void testitHttpToHttps()
        throws Exception
    {
        requiresMavenVersion( "[2.2.0]" );

        testit( true, false );
    }

    /**
     * Verify that redirects from HTTPS to HTTP are getting followed.
     */
    public void testitHttpsToHttp()
        throws Exception
    {
        requiresMavenVersion( "[2.2.0]" );

        testit( false, true );
    }

    /**
     * Verify that redirects using a relative location URL are getting followed. While a relative URL violates the
     * HTTP spec, popular HTTP clients do support them so we better do, too.
     */
    public void testitRelativeLocation()
        throws Exception
    {
        testit( true, true, true );
    }

    private void testit( boolean fromHttp, boolean toHttp )
        throws Exception
    {
        testit( fromHttp, toHttp, false );
    }

    private void testit( boolean fromHttp, boolean toHttp, boolean relativeLocation )
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-4428" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );

        // NOTE: trust store cannot be reliably configured for the current JVM
        verifier.setForkJvm( true );

        // keytool -genkey -alias localhost -keypass key-passwd -keystore keystore -storepass store-passwd \
        //   -validity 4096 -dname "cn=localhost, ou=None, L=Seattle, ST=Washington, o=ExampleOrg, c=US" -keyalg RSA
        String storePath = new File( testDir, "keystore" ).getAbsolutePath();
        String storePwd = "store-passwd";
        String keyPwd = "key-passwd";

        Server server = new Server( 0 );
        server.addConnector( newHttpsConnector( storePath, storePwd, keyPwd ) );
        Connector from = server.getConnectors()[ fromHttp ? 0 : 1 ];
        Connector to = server.getConnectors()[ toHttp ? 0 : 1 ];
        server.setHandler( new RedirectHandler( toHttp ? "http" : "https", relativeLocation ? null : to ) );

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
            verifier.setAutoclean( false );
            verifier.deleteArtifacts( "org.apache.maven.its.mng4428" );
            verifier.deleteDirectory( "target" );
            Properties filterProps = verifier.newDefaultFilterProperties();
            filterProps.setProperty( "@protocol@", fromHttp ? "http" : "https" );
            filterProps.setProperty( "@port@", Integer.toString( from.getLocalPort() ) );
            verifier.filterFile( "settings-template.xml", "settings.xml", "UTF-8", filterProps );
            verifier.addCliOption( "-X --settings" );
            verifier.addCliOption( "settings.xml" );
            verifier.setSystemProperty( "javax.net.ssl.trustStore", storePath );
            verifier.setSystemProperty( "javax.net.ssl.trustStorePassword", storePwd );
            verifier.setLogFileName( "log-" + getName().substring( 6 ) + ".txt" );
            verifier.executeGoal( "validate" );
            verifier.verifyErrorFreeLog();
            verifier.resetStreams();
        }
        finally
        {
            server.stop();
            server.join();
        }

        List<String> cp = verifier.loadLines( "target/classpath.txt", "UTF-8" );
        assertTrue( cp.toString(), cp.contains( "dep-0.1.jar" ) );
    }

    private Connector newHttpsConnector( String keystore, String storepwd, String keypwd )
    {
        SslSocketConnector connector = new SslSocketConnector();
        connector.setPort( 0 );
        connector.setKeystore( keystore );
        connector.setPassword( storepwd );
        connector.setKeyPassword( keypwd );
        return connector;
    }

    static class RedirectHandler extends AbstractHandler
    {

        private final String protocol;

        private final Connector connector;

        RedirectHandler( String protocol, Connector connector )
        {
            this.protocol = protocol;
            this.connector = connector;
        }

        public void handle( String target, HttpServletRequest request, HttpServletResponse response, int dispatch )
            throws IOException
        {
            System.out.println( "Handling " + request.getMethod() + " " + request.getRequestURL() );

            PrintWriter writer = response.getWriter();

            String uri = request.getRequestURI();
            if ( uri.startsWith( "/repo/" ) )
            {
                String location = "/redirected/" + uri.substring( 6 );
                if ( protocol != null && connector != null )
                {
                    location = protocol + "://localhost:" + connector.getLocalPort() + location;
                }
                if ( uri.endsWith( ".pom" ) )
                {
                    response.setStatus( HttpServletResponse.SC_MOVED_TEMPORARILY );
                }
                else
                {
                    response.setStatus( HttpServletResponse.SC_MOVED_PERMANENTLY );
                }
                response.setHeader( "Location", location );
            }
            else if ( uri.endsWith( ".pom" ) )
            {
                writer.println( "<project>" );
                writer.println( "  <modelVersion>4.0.0</modelVersion>" );
                writer.println( "  <groupId>org.apache.maven.its.mng4428</groupId>" );
                writer.println( "  <artifactId>dep</artifactId>" );
                writer.println( "  <version>0.1</version>" );
                writer.println( "</project>" );
                response.setStatus( HttpServletResponse.SC_OK );
            }
            else if ( uri.endsWith( ".jar" ) )
            {
                writer.println( "empty" );
                response.setStatus( HttpServletResponse.SC_OK );
            }
            else
            {
                response.setStatus( HttpServletResponse.SC_NOT_FOUND );
            }

            ( (Request) request ).setHandled( true );
        }

    }

}
