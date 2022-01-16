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

import static org.eclipse.jetty.http.HttpVersion.HTTP_1_1;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-2305">MNG-2305</a>.
 *
 * @author Benjamin Bentmann
 */
public class MavenITmng2305MultipleProxiesTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng2305MultipleProxiesTest()
    {
        super( "[3.0-alpha-3,)" );
    }

    /**
     * Verify that proxies can be setup for multiple protocols, in this case HTTP and HTTPS. As a nice side effect,
     * this checks HTTPS tunneling over a web proxy.
     *
     * @throws Exception in case of failure
     */
    public void testit()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-2305" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );

        // NOTE: trust store cannot be reliably configured for the current JVM
        verifier.setForkJvm( true );

        // keytool -genkey -alias https.mngit -keypass key-passwd -keystore keystore -storepass store-passwd \
        //   -validity 4096 -dname "cn=https.mngit, ou=None, L=Seattle, ST=Washington, o=ExampleOrg, c=US" -keyalg RSA
        String storePath = new File( testDir, "keystore" ).getAbsolutePath();
        String storePwd = "store-passwd";
        String keyPwd = "key-passwd";

        Server server = new Server( 0 );
        addHttpsConnector( server, storePath, storePwd, keyPwd );
        server.setHandler( new RepoHandler() );
        server.start();
        if ( server.isFailed() )
        {
            fail( "Couldn't bind the server socket to a free port!" );
        }
        int httpPort = ( (NetworkConnector) server.getConnectors()[0] ).getLocalPort();
        System.out.println( "Bound server socket to HTTP port " + httpPort );
        int httpsPort = ( (NetworkConnector) server.getConnectors()[1] ).getLocalPort();
        System.out.println( "Bound server socket to HTTPS port " + httpsPort );

        TunnelingProxyServer proxy = new TunnelingProxyServer( 0, "localhost", httpsPort, "https.mngit:443" );
        proxy.start();
        int proxyPort = proxy.getPort();
        System.out.println( "Bound server socket to the proxy port " + proxyPort );

        try
        {
            verifier.setAutoclean( false );
            verifier.deleteDirectory( "target" );
            verifier.deleteArtifacts( "org.apache.maven.its.mng2305" );
            Properties filterProps = verifier.newDefaultFilterProperties();
            filterProps.setProperty( "@proxy.http@", Integer.toString( httpPort ) );
            filterProps.setProperty( "@proxy.https@", Integer.toString( proxyPort ) );
            verifier.filterFile( "settings-template.xml", "settings.xml", "UTF-8", filterProps );
            verifier.addCliOption( "--settings" );
            verifier.addCliOption( "settings.xml" );
            verifier.setSystemProperty( "javax.net.ssl.trustStore", storePath );
            verifier.setSystemProperty( "javax.net.ssl.trustStorePassword", storePwd );
            // disable concurrent downloading as not all wagons (e.g. wagon-lightweight-http) are thread-safe regarding proxy settings
            verifier.setSystemProperty( "maven.artifact.threads", "1" );
            verifier.executeGoal( "validate" );
            verifier.verifyErrorFreeLog();
            verifier.resetStreams();
        }
        finally
        {
            proxy.stop();
            server.stop();
            server.join();
        }

        List<String> cp = verifier.loadLines( "target/classpath.txt", "UTF-8" );
        assertTrue( cp.toString(), cp.contains( "http-0.1.jar" ) );
        assertTrue( cp.toString(), cp.contains( "https-0.1.jar" ) );
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

    static class RepoHandler extends AbstractHandler
    {
        public void handle( String target, Request baseRequest, HttpServletRequest request,
                            HttpServletResponse response )
            throws IOException
        {
            PrintWriter writer = response.getWriter();

            String uri = request.getRequestURI();

            if ( !uri.startsWith( "/repo/org/apache/maven/its/mng2305/" + request.getScheme() + "/" ) )
            {
                // HTTP connector serves only http-0.1.jar and HTTPS connector serves only https-0.1.jar
                response.setStatus( HttpServletResponse.SC_NOT_FOUND );
            }
            else if ( uri.endsWith( ".pom" ) )
            {
                writer.println( "<project>" );
                writer.println( "  <modelVersion>4.0.0</modelVersion>" );
                writer.println( "  <groupId>org.apache.maven.its.mng2305</groupId>" );
                writer.println( "  <artifactId>" + request.getScheme() + "</artifactId>" );
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
