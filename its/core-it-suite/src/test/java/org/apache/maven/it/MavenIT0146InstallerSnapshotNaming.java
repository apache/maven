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
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.DefaultHandler;
import org.mortbay.jetty.handler.HandlerList;
import org.mortbay.jetty.handler.ResourceHandler;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Properties;

public class MavenIT0146InstallerSnapshotNaming
    extends AbstractMavenIntegrationTestCase
{
    private Server server;

    private int port;

    private final File testDir;

    public MavenIT0146InstallerSnapshotNaming()
        throws IOException
    {
        super( "(2.0.2,)" );
        testDir = ResourceExtractor.simpleExtractResources( getClass(), "/it0146" );
    }

    @Override
    protected void setUp()
        throws Exception
    {
        ResourceHandler resourceHandler = new ResourceHandler();
        resourceHandler.setResourceBase( new File( testDir, "repo" ).getAbsolutePath() );
        HandlerList handlers = new HandlerList();
        handlers.setHandlers( new Handler[]{ resourceHandler, new DefaultHandler() } );

        server = new Server( 0 );
        server.setHandler( handlers );
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

    public void testitRemoteDownloadTimestampedName()
        throws Exception
    {

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );

        Properties properties = verifier.newDefaultFilterProperties();
        properties.setProperty( "@host@", InetAddress.getLocalHost().getCanonicalHostName() );
        properties.setProperty( "@port@", Integer.toString( port ) );

        verifier.filterFile( "settings-template.xml", "settings.xml", "UTF-8", properties );

        verifier.addCliOption( "--settings" );
        verifier.addCliOption( "settings.xml" );

        verifier.deleteArtifacts( "org.apache.maven.its.it0146" );

        verifier.addCliOption( "-X" );

        verifier.deleteDirectory( "target" );

        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        verifier.assertFilePresent( "target/appassembler/repo/dep-0.1-20110726.105319-1.jar" );
    }


    public void testitNonTimestampedNameWithInstalledSNAPSHOT()
        throws Exception
    {
        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.deleteArtifacts( "org.apache.maven.its.it0146" );
        verifier.addCliOption( "-f" );
        verifier.addCliOption( "project/pom.xml" );
        verifier.deleteDirectory( "project/target" );
        verifier.setLogFileName( "log2.txt" );

        verifier.executeGoal( "install" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        verifier = newVerifier( testDir.getAbsolutePath() );

        Properties properties = verifier.newDefaultFilterProperties();
        properties.setProperty( "@host@", InetAddress.getLocalHost().getCanonicalHostName() );
        properties.setProperty( "@port@", Integer.toString( port ) );

        verifier.filterFile( "settings-template.xml", "settings.xml", "UTF-8", properties );

        verifier.addCliOption( "--settings" );
        verifier.addCliOption( "settings.xml" );
        verifier.setLogFileName( "log3.txt" );


        verifier.addCliOption( "-X" );

        verifier.deleteDirectory( "target" );

        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        verifier.assertFilePresent( "target/appassembler/repo/dep-0.1-SNAPSHOT.jar" );
    }
}
