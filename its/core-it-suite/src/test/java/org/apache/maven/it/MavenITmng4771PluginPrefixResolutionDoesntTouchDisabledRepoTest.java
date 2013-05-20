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
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.AbstractHandler;
import org.mortbay.jetty.handler.DefaultHandler;
import org.mortbay.jetty.handler.HandlerList;

/**
 * This is a test set for <a href="http://jira.codehaus.org/browse/MNG-4771">MNG-4771</a>.
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

        final List<String> requestedUris = Collections.synchronizedList( new ArrayList<String>() );

        AbstractHandler logHandler = new AbstractHandler()
        {
            public void handle( String target, HttpServletRequest request, HttpServletResponse response, int dispatch )
                throws IOException, ServletException
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
            verifier.setAutoclean( false );
            verifier.deleteDirectory( "target" );
            Properties filterProps = verifier.newDefaultFilterProperties();
            filterProps.setProperty( "@port@", Integer.toString( server.getConnectors()[0].getLocalPort() ) );
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
            assertTrue( true );
        }
        finally
        {
            verifier.resetStreams();
            server.stop();
        }

        assertTrue( requestedUris.toString(), requestedUris.isEmpty() );
    }

}
