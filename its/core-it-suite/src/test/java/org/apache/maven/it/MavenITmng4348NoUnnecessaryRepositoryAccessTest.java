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
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.NetworkConnector;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4348">MNG-4348</a>.
 *
 * @author Benjamin Bentmann
 *
 */
public class MavenITmng4348NoUnnecessaryRepositoryAccessTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng4348NoUnnecessaryRepositoryAccessTest()
    {
        super( ALL_MAVEN_VERSIONS );
    }

    /**
     * Test that the (remote) repos are not accessed during execution of a mojo that does not require dependency
     * resolution. In detail, Maven should neither touch POMs, JARs nor metadata.
     *
     * @throws Exception in case of failure
     */
    public void testit()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-4348" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );

        final List<String> requestedUris = Collections.synchronizedList( new ArrayList<String>() );

        Handler repoHandler = new AbstractHandler()
        {
            @Override
            public void handle( String target, Request baseRequest, HttpServletRequest request,
                                HttpServletResponse response )
            {
                System.out.println( "Handling " + request.getMethod() + " " + request.getRequestURL() );

                // NOTE: Old Maven versions use the test repo also to check for plugin updates so we need to filter
                if ( request.getRequestURI().startsWith( "/org/apache/maven/its/mng4348" ) )
                {
                    requestedUris.add( request.getRequestURI() );
                }

                response.setStatus( HttpServletResponse.SC_NOT_FOUND );

                ( (Request) request ).setHandled( true );
            }
        };

        Server server = new Server( 0 );
        server.setHandler( repoHandler );

        try
        {
            server.start();
            if ( server.isFailed() )
            {
                fail( "Couldn't bind the server socket to a free port!" );
            }
            int port = ( (NetworkConnector) server.getConnectors()[0] ).getLocalPort();
            System.out.println( "Bound server socket to the port " + port );
            verifier.setAutoclean( false );
            verifier.deleteArtifacts( "org.apache.maven.its.mng4348" );
            verifier.deleteDirectory( "target" );
            Properties filterProps = verifier.newDefaultFilterProperties();
            filterProps.setProperty( "@port@", Integer.toString( port ) );
            verifier.filterFile( "settings-template.xml", "settings.xml", "UTF-8", filterProps );
            verifier.addCliOption( "--settings" );
            verifier.addCliOption( "settings.xml" );
            verifier.executeGoal( "validate" );
            verifier.verifyErrorFreeLog();
            verifier.resetStreams();
        }
        finally
        {
            server.stop();
            server.join();
        }

        verifier.verifyFilePresent( "target/touch.txt" );
        assertEquals( new ArrayList<String>(), requestedUris );
    }
}
