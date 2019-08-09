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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.Properties;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-5175">MNG-5175</a>.
 * test correct integration of wagon http: read time out configuration from settings.xml
 *
 * @version $Id$
 */
public class MavenITmng5175WagonHttpTest
    extends AbstractMavenIntegrationTestCase
{
    private Server server;

    private int port;

    public MavenITmng5175WagonHttpTest()
    {
        super( "[3.0.4,)" ); // 3.0.4+
    }

    @Override
    protected void setUp()
        throws Exception
    {
        Handler handler = new AbstractHandler()
        {
            public void handle( String target, HttpServletRequest request, HttpServletResponse response, int dispatch )
                throws IOException, ServletException
            {
                try
                {
                    Thread.sleep( 15 );
                }
                catch ( InterruptedException e )
                {
                    throw new ServletException( e.getMessage() );
                }
                response.setContentType( "text/plain" );
                response.setStatus( HttpServletResponse.SC_OK );
                response.getWriter().println( "some content" );
                response.getWriter().println();

                ( (Request) request ).setHandled( true );
            }
        };

        server = new Server( 0 );
        server.setHandler( handler );
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
     * Test that the read time out from settings is used.
     * basically use a 1ms time out and wait a bit in the handler
     */
    public void testmng5175_ReadTimeOutFromSettings()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-5175" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );

        Properties filterProps = new Properties();
        filterProps.setProperty( "@port@", Integer.toString( port ) );

        verifier.filterFile( "settings-template.xml", "settings.xml", "UTF-8", filterProps );

        verifier.addCliOption( "-U" );
        verifier.addCliOption( "--settings" );
        verifier.addCliOption( "settings.xml" );
        //verifier.
        verifier.executeGoal( "validate" );

        verifier.verifyTextInLog( "Read timed out" );
        verifier.resetStreams();
    }
}
