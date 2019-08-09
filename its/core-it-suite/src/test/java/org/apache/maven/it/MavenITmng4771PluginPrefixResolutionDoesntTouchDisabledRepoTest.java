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
import java.util.Deque;
import java.util.Properties;
import java.util.concurrent.ConcurrentLinkedDeque;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.AbstractHandler;
import org.mortbay.jetty.handler.DefaultHandler;
import org.mortbay.jetty.handler.HandlerList;

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
     */
    public void testit()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-4771" );

        final Deque<String> requestedUris = new ConcurrentLinkedDeque<>();

        AbstractHandler logHandler = new AbstractHandler()
        {
            public void handle( String target, HttpServletRequest request, HttpServletResponse response, int dispatch )
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
            while ( !server.isRunning() || !server.isStarted() )
            {
                if ( server.isFailed() )
                {
                    fail( "Couldn't bind the server socket to a free port!" );
                }
                Thread.sleep( 100L );
            }
            int port = server.getConnectors()[0].getLocalPort();
            System.out.println( "Bound server socket to the port " + port );
            verifier.setAutoclean( false );
            verifier.deleteDirectory( "target" );
            Properties filterProps = verifier.newDefaultFilterProperties();
            filterProps.setProperty( "@port@", Integer.toString( port ) );
            verifier.filterFile( "settings-template.xml", "settings.xml", "UTF-8", filterProps );
            verifier.addCliOption( "-U" );
            verifier.addCliOption( "-s" );
            verifier.addCliOption( "settings.xml" );
            verifier.executeGoal( "mng4771:touch" );
            verifier.verifyErrorFreeLog();
            fail( "Build should have failed to resolve unknown prefix" );
        }
        catch ( VerificationException e )
        {
            // expected
        }
        finally
        {
            verifier.resetStreams();
            server.stop();
            server.join();
        }

        assertTrue( requestedUris.toString(), requestedUris.isEmpty() );
    }
}
