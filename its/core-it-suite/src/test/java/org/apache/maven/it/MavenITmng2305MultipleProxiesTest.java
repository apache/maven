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
        server.addConnector( newHttpsConnector( storePath, storePwd, keyPwd ) );
        server.setHandler( new RepoHandler() );
        server.start();
        while ( !server.isRunning() || !server.isStarted() )
        {
            if ( server.isFailed() )
            {
                fail( "Couldn't bind the server socket to a free port!" );
            }
            Thread.sleep( 100L );
        }
        int httpPort = server.getConnectors()[0].getLocalPort();
        System.out.println( "Bound server socket to HTTP port " + httpPort );
        int httpsPort = server.getConnectors()[1].getLocalPort();
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
        }

        List<String> cp = verifier.loadLines( "target/classpath.txt", "UTF-8" );
        assertTrue( cp.toString(), cp.contains( "http-0.1.jar" ) );
        assertTrue( cp.toString(), cp.contains( "https-0.1.jar" ) );
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

    static class RepoHandler extends AbstractHandler
    {

        public void handle( String target, HttpServletRequest request, HttpServletResponse response, int dispatch )
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
