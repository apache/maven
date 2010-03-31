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

import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.mortbay.jetty.Handler;
import org.mortbay.jetty.Request;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.AbstractHandler;

/**
 * This is a test set for <a href="http://jira.codehaus.org/browse/MNG-4555">MNG-4555</a>.
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

        final List uris = new ArrayList();

        Handler repoHandler = new AbstractHandler()
        {
            public void handle( String target, HttpServletRequest request, HttpServletResponse response, int dispatch )
                throws IOException
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

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.deleteArtifacts( "org.apache.maven.its.mng4555" );
        try
        {
            Properties filterProps = verifier.newDefaultFilterProperties();
            filterProps.setProperty( "@port@", Integer.toString( server.getConnectors()[0].getLocalPort() ) );
            verifier.filterFile( "settings-template.xml", "settings.xml", "UTF-8", filterProps );
            verifier.getCliOptions().add( "--offline" );
            verifier.getCliOptions().add( "--settings" );
            verifier.getCliOptions().add( "settings.xml" );
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
        }

        assertTrue( uris.toString(), uris.isEmpty() );
    }

}
