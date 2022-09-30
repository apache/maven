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

import java.io.File;
import java.net.InetAddress;
import java.util.Properties;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.NetworkConnector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-2387">MNG-2387</a>.
 *
 * @author Brett Porter
 *
 */
public class MavenITmng2387InactiveProxyTest
    extends AbstractMavenIntegrationTestCase
{
    private Server server;

    private int port;

    private Server proxyServer;

    private int proxyPort;

    private File testDir;

    public MavenITmng2387InactiveProxyTest()
    {
        super( "[2.0.11,2.1.0-M1),[2.1.0,)" ); // 2.0.11+, 2.1.0+
    }

    @BeforeEach
    protected void setUp()
        throws Exception
    {
        testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-2387" );

        ResourceHandler resourceHandler = new ResourceHandler();
        resourceHandler.setResourceBase( new File( testDir, "repo" ).getAbsolutePath() );

        HandlerList handlers = new HandlerList();
        handlers.setHandlers( new Handler[] { resourceHandler, new DefaultHandler() } );

        server = new Server( 0 );
        server.setHandler( handlers );
        server.start();
        if ( server.isFailed() )
        {
            fail( "Couldn't bind the server socket to a free port!" );
        }
        port = ( (NetworkConnector) server.getConnectors()[0] ).getLocalPort();
        System.out.println( "Bound server socket to the HTTP port " + port );

        resourceHandler = new ResourceHandler();
        resourceHandler.setResourceBase( new File( testDir, "proxy" ).getAbsolutePath() );

        handlers = new HandlerList();
        handlers.setHandlers( new Handler[] { resourceHandler, new DefaultHandler() } );

        proxyServer = new Server( 0 );
        proxyServer.setHandler( handlers );
        proxyServer.start();
        if ( proxyServer.isFailed() )
        {
            fail( "Couldn't bind the server socket to a free port!" );
        }
        proxyPort = ( (NetworkConnector) proxyServer.getConnectors()[0] ).getLocalPort();
        System.out.println( "Bound server socket to the HTTPS port " + port );
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
        if ( proxyServer != null )
        {
            proxyServer.stop();
            proxyServer.join();
        }
    }

    /**
     * Test that no proxy is used if none of the configured proxies is actually set as active.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testit()
        throws Exception
    {
        Verifier verifier = newVerifier( testDir.getAbsolutePath() );

        Properties properties = verifier.newDefaultFilterProperties();
        properties.setProperty( "@host@", InetAddress.getLoopbackAddress().getCanonicalHostName() );
        properties.setProperty( "@port@", Integer.toString( port ) );
        properties.setProperty( "@proxyPort@", Integer.toString( proxyPort ) );
        verifier.filterFile( "settings-template.xml", "settings.xml", "UTF-8", properties );

        verifier.setAutoclean( false );
        verifier.deleteArtifacts( "org.apache.maven.its.mng2387" );
        verifier.addCliOption( "--settings" );
        verifier.addCliOption( "settings.xml" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        verifier.verifyArtifactPresent( "org.apache.maven.its.mng2387", "a", "0.1", "jar" );
    }
}
