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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.eclipse.jetty.server.NetworkConnector;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.junit.jupiter.api.Test;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-5064">MNG-5064</a>.
 *
 * @author Benjamin Bentmann
 */
public class MavenITmng5064SuppressSnapshotUpdatesTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng5064SuppressSnapshotUpdatesTest()
    {
        super( "[3.0.4,)" );
    }

    /**
     * Verify that snapshot updates can be completely suppressed via the CLI arg -nsu. The initial retrieval of a
     * missing snapshot should not be suppressed though.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testit()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-5064" );

        String metadataUri = "org/apache/maven/its/mng5064/dep/0.1-SNAPSHOT/maven-metadata.xml";

        final List<String> requestedUris = Collections.synchronizedList( new ArrayList<String>() );

        AbstractHandler logHandler = new AbstractHandler()
        {
            @Override
            public void handle( String target, Request baseRequest, HttpServletRequest request,
                                HttpServletResponse response )
            {
                if ( request.getRequestURI().startsWith( "/repo/" ) )
                {
                    requestedUris.add( request.getRequestURI().substring( 6 ) );
                }
            }
        };

        ResourceHandler repoHandler = new ResourceHandler();
        repoHandler.setResourceBase( testDir.getAbsolutePath() );

        HandlerList handlerList = new HandlerList();
        handlerList.addHandler( logHandler );
        handlerList.addHandler( repoHandler );
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
            verifier.deleteArtifacts( "org.apache.maven.its.mng5064" );
            Map<String, String> filterProps = verifier.newDefaultFilterMap();
            filterProps.put( "@port@", Integer.toString( port ) );
            verifier.filterFile( "settings-template.xml", "settings.xml", "UTF-8", filterProps );
            verifier.addCliArgument( "-nsu" );
            verifier.addCliArgument( "-s" );
            verifier.addCliArgument( "settings.xml" );

            verifier.setLogFileName( "log-1.txt" );
            verifier.addCliArgument( "validate" );
            verifier.execute();
            verifier.verifyErrorFreeLog();

            List<String> classpath = verifier.loadLines( "target/classpath.txt", "UTF-8" );
            assertTrue( classpath.toString(), classpath.contains( "dep-0.1-SNAPSHOT.jar" ) );
            assertTrue( requestedUris.toString(), requestedUris.contains( metadataUri ) );

            requestedUris.clear();

            verifier.setLogFileName( "log-2.txt" );
            verifier.addCliArgument( "validate" );
            verifier.execute();
            verifier.verifyErrorFreeLog();

            classpath = verifier.loadLines( "target/classpath.txt", "UTF-8" );
            assertTrue( classpath.toString(), classpath.contains( "dep-0.1-SNAPSHOT.jar" ) );
            assertFalse( requestedUris.toString(), requestedUris.contains( metadataUri ) );
        }
        finally
        {
            server.stop();
            server.join();
        }
    }
}
