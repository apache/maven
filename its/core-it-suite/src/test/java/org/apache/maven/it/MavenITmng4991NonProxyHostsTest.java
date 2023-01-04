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
import java.util.List;
import java.util.Properties;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.NetworkConnector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.junit.jupiter.api.Test;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4991">MNG-4991</a>.
 *
 * @author Benjamin Bentmann
 */
public class MavenITmng4991NonProxyHostsTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng4991NonProxyHostsTest()
    {
        super( "[2.0.3,3.0-alpha-1),[3.0.3,)" );
    }

    /**
     * Verify that the nonProxyHosts settings is respected.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testit()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-4991" );

        ResourceHandler resourceHandler = new ResourceHandler();
        resourceHandler.setResourceBase( new File( testDir, "repo" ).getAbsolutePath() );

        HandlerList handlers = new HandlerList();
        handlers.setHandlers( new Handler[] { resourceHandler, new DefaultHandler() } );

        Server server = new Server( 0 );
        server.setHandler( handlers );

        /*
         * NOTE: To guard against automatic fallback to direct connection when the proxy is unreachable, we set up
         * a dummy proxy as trap to catch the erroneous proxy usage in all cases.
         */
        Server proxy = new Server( 0 );
        proxy.setHandler( new DefaultHandler() );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        try
        {
            server.start();
            if ( server.isFailed() )
            {
                fail( "Couldn't bind the server socket to a free port!" );
            }

            proxy.start();
            if ( proxy.isFailed() )
            {
                fail( "Couldn't bind the server socket to a free port!" );
            }

            verifier.setAutoclean( false );
            verifier.deleteDirectory( "target" );
            verifier.deleteArtifacts( "org.apache.maven.its.mng4991" );
            Properties filterProps = verifier.newDefaultFilterProperties();
            int port = ( (NetworkConnector) server.getConnectors()[0] ).getLocalPort();
            filterProps.setProperty( "@port@", Integer.toString( port ) );
            int proxyPort = ( (NetworkConnector) proxy.getConnectors()[0] ).getLocalPort();
            filterProps.setProperty( "@proxyPort@", Integer.toString( proxyPort ) );
            filterProps.setProperty( "@localhost@", InetAddress.getLoopbackAddress().getCanonicalHostName() );
            verifier.filterFile( "settings-template.xml", "settings.xml", "UTF-8", filterProps );
            verifier.addCliOption( "-s" );
            verifier.addCliOption( "settings.xml" );
            verifier.executeGoal( "validate" );
            verifier.verifyErrorFreeLog();
        }
        finally
        {
            server.stop();
            proxy.stop();
            server.join();
            proxy.join();
        }

        List<String> compile = verifier.loadLines( "target/compile.txt", "UTF-8" );

        assertTrue( compile.toString(), compile.contains( "dep-0.1.jar" ) );
    }
}
