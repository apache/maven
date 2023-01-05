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
import org.apache.maven.shared.verifier.VerificationException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.File;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;

import org.eclipse.jetty.server.NetworkConnector;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.junit.jupiter.api.Test;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4771">MNG-4771</a>.
 *
 * @author Benjamin Bentmann
 */
public class MavenITmng4771PluginPrefixResolutionDoesntTouchDisabledRepoTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng4771PluginPrefixResolutionDoesntTouchDisabledRepoTest()
    {
        super( "[2.0.3,3.0-alpha-1),[3.0-beta-3,)" );
    }

    /**
     * Verify that repositories which have both releases and snapshots disabled aren't touched when looking for
     * plugin prefix mappings.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testit()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-4771" );

        final Deque<String> requestedUris = new ConcurrentLinkedDeque<>();

        AbstractHandler logHandler = new AbstractHandler()
        {
            @Override
            public void handle( String target, Request baseRequest, HttpServletRequest request,
                                HttpServletResponse response )
            {
                requestedUris.add( request.getRequestURI() );
            }
        };

        HandlerList handlerList = new HandlerList();
        handlerList.addHandler( logHandler );
        handlerList.addHandler( new DefaultHandler() );

        Server server = new Server( 0 );
        server.setHandler( handlerList );
        server.start();

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        try
        {
            if ( server.isFailed() )
            {
                fail( "Couldn't bind the server socket to a free port!" );
            }
            int port = ( (NetworkConnector) server.getConnectors()[0] ).getLocalPort();
            System.out.println( "Bound server socket to the port " + port );
            verifier.setAutoclean( false );
            verifier.deleteDirectory( "target" );
            Map<String, String> filterProps = verifier.newDefaultFilterMap();
            filterProps.put( "@port@", Integer.toString( port ) );
            verifier.filterFile( "settings-template.xml", "settings.xml", "UTF-8", filterProps );
            verifier.addCliArgument( "-U" );
            verifier.addCliArgument( "-s" );
            verifier.addCliArgument( "settings.xml" );
            verifier.addCliArgument( "mng4771:touch" );
            verifier.execute();
            verifier.verifyErrorFreeLog();
            fail( "Build should have failed to resolve unknown prefix" );
        }
        catch ( VerificationException e )
        {
            // expected
        }
        finally
        {
            server.stop();
            server.join();
        }

        assertTrue( requestedUris.toString(), requestedUris.isEmpty() );
    }
}
