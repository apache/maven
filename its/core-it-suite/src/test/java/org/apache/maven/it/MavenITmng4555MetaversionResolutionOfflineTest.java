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
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.Request;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.AbstractHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.util.Deque;
import java.util.Properties;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4555">MNG-4555</a>.
 *
 * @author Benjamin Bentmann
 */
public class MavenITmng4555MetaversionResolutionOfflineTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng4555MetaversionResolutionOfflineTest()
    {
        super( "[2.0.3,3.0-alpha-1),[3.0-beta-1,)" );
    }

    /**
     * Verify that resolution of the metaversion RELEASE respects offline mode.
     */
    public void testit()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-4555" );

        final Deque<String> uris = new ConcurrentLinkedDeque<>();

        Handler repoHandler = new AbstractHandler()
        {
            public void handle( String target, HttpServletRequest request, HttpServletResponse response, int dispatch )
            {
                String uri = request.getRequestURI();

                if ( uri.startsWith( "/repo/org/apache/maven/its/mng4555" ) )
                {
                    uris.add( uri.substring( 34 ) );
                }

                response.setStatus( HttpServletResponse.SC_NOT_FOUND );
                ( (Request) request ).setHandled( true );
            }
        };

        Server server = new Server( 0 );
        server.setHandler( repoHandler );
        server.start();

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.deleteArtifacts( "org.apache.maven.its.mng4555" );
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
            Properties filterProps = verifier.newDefaultFilterProperties();
            filterProps.setProperty( "@port@", Integer.toString( port ) );
            verifier.filterFile( "settings-template.xml", "settings.xml", "UTF-8", filterProps );
            verifier.addCliOption( "--offline" );
            verifier.addCliOption( "--settings" );
            verifier.addCliOption( "settings.xml" );
            verifier.executeGoal( "validate" );
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

        assertTrue( uris.toString(), uris.isEmpty() );
    }
}
