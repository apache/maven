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
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.Deque;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4326">MNG-4326</a>.
 *
 * @author Benjamin Bentmann
 */
public class MavenITmng4326LocalSnapshotSuppressesRemoteCheckTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng4326LocalSnapshotSuppressesRemoteCheckTest()
    {
        super( "[3.0-beta-1,)" );
    }

    /**
     * Verify that locally built/installed snapshot artifacts suppress remote update checks (as long as the local copy
     * still satifies the update policy configured for the remote repository).
     */
    public void testit()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-4326" );

        // setup: install a local snapshot
        Verifier verifier = newVerifier( new File( testDir, "dependency" ).getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteArtifacts( "org.apache.maven.its.mng4326" );
        verifier.deleteDirectory( "target" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        final Deque<String> uris = new ConcurrentLinkedDeque<>();

        Handler repoHandler = new AbstractHandler()
        {
            public void handle( String target, HttpServletRequest request, HttpServletResponse response, int dispatch )
                throws IOException
            {
                PrintWriter writer = response.getWriter();

                String uri = request.getRequestURI();

                if ( uri.startsWith( "/repo/org/apache/maven/its/mng4326" ) && !uri.endsWith( ".md5" ) && !uri.endsWith(
                    ".sha1" ) )
                {
                    uris.add( uri.substring( 34 ) );
                }

                if ( uri.endsWith( "dep/0.1-SNAPSHOT/maven-metadata.xml" ) )
                {
                    java.text.DateFormat fmt = new java.text.SimpleDateFormat( "yyyyMMddHHmmss" );
                    fmt.setTimeZone( java.util.TimeZone.getTimeZone( "UTC" ) );
                    String now = fmt.format( new Date( System.currentTimeMillis() + 3000 ) );

                    response.setStatus( HttpServletResponse.SC_OK );
                    writer.println( "<metadata>" );
                    writer.println( "  <groupId>org.apache.maven.its.mng4326</groupId>" );
                    writer.println( "  <artifactId>dep</artifactId>" );
                    writer.println( "  <version>0.1-SNAPSHOT</version>" );
                    writer.println( "  <versioning>" );
                    writer.println( "    <snapshot>" );
                    writer.println( "      <timestamp>20100329.235556</timestamp>" );
                    writer.println( "      <buildNumber>1</buildNumber>" );
                    writer.println( "    </snapshot>" );
                    writer.println( "    <lastUpdated>" + now + "</lastUpdated>" );
                    writer.println( "  </versioning>" );
                    writer.println( "</metadata>" );
                }
                else if ( uri.endsWith( ".pom" ) )
                {
                    response.setStatus( HttpServletResponse.SC_OK );
                    writer.println( "<project>" );
                    writer.println( "  <modelVersion>4.0.0</modelVersion>" );
                    writer.println( "  <groupId>org.apache.maven.its.mng4326</groupId>" );
                    writer.println( "  <artifactId>dep</artifactId>" );
                    writer.println( "  <version>0.1-SNAPSHOT</version>" );
                    writer.println( "</project>" );
                }
                else if ( uri.endsWith( ".jar" ) )
                {
                    response.setStatus( HttpServletResponse.SC_OK );
                    writer.println( "empty" );
                }
                else
                {
                    response.setStatus( HttpServletResponse.SC_NOT_FOUND );
                }

                ( (Request) request ).setHandled( true );
            }
        };

        Server server = new Server( 0 );
        server.setHandler( repoHandler );

        try
        {
            server.start();
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
            // test 1: resolve snapshot, just built local copy should suppress daily remote update check
            verifier = newVerifier( new File( testDir, "test" ).getAbsolutePath() );
            verifier.setAutoclean( false );
            Properties filterProps = verifier.newDefaultFilterProperties();
            filterProps.setProperty( "@port@", Integer.toString( port ) );
            verifier.filterFile( "settings-template.xml", "settings.xml", "UTF-8", filterProps );
            verifier.addCliOption( "--settings" );
            verifier.addCliOption( "settings.xml" );
            verifier.setLogFileName( "log-daily.txt" );
            verifier.deleteDirectory( "target" );
            verifier.executeGoal( "validate" );
            verifier.verifyErrorFreeLog();

            List<String> cp = verifier.loadLines( "target/classpath.txt", "UTF-8" );
            assertTrue( cp.toString(), cp.contains( "dep-0.1-SNAPSHOT.jar" ) );

            assertFalse( uris.toString(), uris.contains( "/dep/0.1-SNAPSHOT/maven-metadata.xml" ) );
            assertFalse( uris.toString(), uris.contains( "/dep/0.1-SNAPSHOT/dep-0.1-20100329.235556-1.jar" ) );

            uris.clear();

            // test 2: force snapshot updates, remote metadata and artifacts should be fetched
            verifier.addCliOption( "-U" );
            verifier.setLogFileName( "log-force.txt" );
            verifier.deleteDirectory( "target" );
            verifier.executeGoal( "validate" );
            verifier.verifyErrorFreeLog();

            cp = verifier.loadLines( "target/classpath.txt", "UTF-8" );
            assertTrue( cp.toString(), cp.contains( "dep-0.1-SNAPSHOT.jar" ) );

            assertTrue( uris.toString(), uris.contains( "/dep/0.1-SNAPSHOT/maven-metadata.xml" ) );
            assertTrue( uris.toString(), uris.contains( "/dep/0.1-SNAPSHOT/dep-0.1-20100329.235556-1.jar" ) );
        }
        finally
        {
            verifier.resetStreams();
            server.stop();
        }
    }
}
