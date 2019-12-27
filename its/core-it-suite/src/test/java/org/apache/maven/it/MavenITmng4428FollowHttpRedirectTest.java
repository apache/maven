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
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.NetworkConnector;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Properties;

import static org.eclipse.jetty.http.HttpVersion.HTTP_1_1;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4428">MNG-4428</a>.
 * 
 * @author Benjamin Bentmann
 *
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
        addHttpsConnector( server, storePath, storePwd, keyPwd );
        Connector from = server.getConnectors()[ fromHttp ? 0 : 1 ];
        Connector to = server.getConnectors()[ toHttp ? 0 : 1 ];
        server.setHandler(
                new RedirectHandler( toHttp ? "http" : "https", relativeLocation ? null : (NetworkConnector) to ) );

        try
        {
            server.start();
            if ( server.isFailed() )
            {
                fail( "Couldn't bind the server socket to a free port!" );
            }
            verifier.setAutoclean( false );
            verifier.deleteArtifacts( "org.apache.maven.its.mng4428" );
            verifier.deleteDirectory( "target" );
            Properties filterProps = verifier.newDefaultFilterProperties();
            filterProps.setProperty( "@protocol@", fromHttp ? "http" : "https" );
            filterProps.setProperty( "@port@", Integer.toString( ( (NetworkConnector) from ).getLocalPort() ) );
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

    private void addHttpsConnector( Server server, String keyStorePath, String keyStorePassword, String keyPassword )
    {
        SslContextFactory sslContextFactory = new SslContextFactory( keyStorePath );
        sslContextFactory.setKeyStorePassword( keyStorePassword );
        sslContextFactory.setKeyManagerPassword( keyPassword );
        HttpConfiguration httpConfiguration = new HttpConfiguration();
        httpConfiguration.setSecureScheme( "https" );
        HttpConfiguration httpsConfiguration = new HttpConfiguration( httpConfiguration );
        httpsConfiguration.addCustomizer( new SecureRequestCustomizer() );
        ServerConnector httpsConnector = new ServerConnector( server,
                new SslConnectionFactory( sslContextFactory, HTTP_1_1.asString() ),
                new HttpConnectionFactory( httpsConfiguration ) );
        server.addConnector( httpsConnector );
    }

    static class RedirectHandler extends AbstractHandler
    {
        private final String protocol;

        private final NetworkConnector connector;

        RedirectHandler( String protocol, NetworkConnector connector )
        {
            this.protocol = protocol;
            this.connector = connector;
        }

        public void handle( String target, Request baseRequest, HttpServletRequest request,
                            HttpServletResponse response )
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
