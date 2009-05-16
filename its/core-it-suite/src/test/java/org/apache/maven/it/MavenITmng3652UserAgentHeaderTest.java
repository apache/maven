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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.Request;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.AbstractHandler;

public class MavenITmng3652UserAgentHeaderTest
    extends AbstractMavenIntegrationTestCase
{
    private Server server;

    private int port;

    private String userAgent;

    public MavenITmng3652UserAgentHeaderTest()
    {
        // TODO: re-instate feature in 3.0
        super( "[2.1.0-M1,3.0-alpha-1)" ); // 2.1.0-M1+
    }

    public void setUp()
        throws Exception
    {
        Handler handler = new AbstractHandler()
        {
            public void handle( String target, HttpServletRequest request, HttpServletResponse response, int dispatch )
                throws IOException, ServletException
            {
                System.out.println( "Handling URL: '" + request.getRequestURL() + "'" );
                
                userAgent = request.getHeader( "User-Agent" );
                
                System.out.println( "Got User-Agent: '" + userAgent + "'" );

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

        port = server.getConnectors()[0].getLocalPort();
    }

    protected void tearDown()
        throws Exception
    {
        super.tearDown();

        if ( server != null)
        {
            server.stop();
            server = null;
        }
    }

    /**
     * Test that the user agent header is configured in the wagon manager.
     */
    public void testmng3652_UnConfiguredHttp()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-3652" );
        File pluginDir = new File( testDir, "test-plugin" );
        File projectDir = new File( testDir, "test-project" );

        Verifier verifier = new Verifier( pluginDir.getAbsolutePath() );
        verifier.executeGoal( "install" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        verifier = new Verifier( projectDir.getAbsolutePath() );

        List cliOptions = new ArrayList();
        cliOptions.add( "-DtestPort=" + port );
        cliOptions.add( "-X" );
        verifier.setCliOptions( cliOptions );

        verifier.setLogFileName( "log-unConfiguredHttp.txt" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        String userAgent = this.userAgent;
        assertNotNull( userAgent );

        File touchFile = new File( projectDir, "target/touch.txt" );
        assertTrue( touchFile.exists() );

        List lines = verifier.loadFile( touchFile, false );

        // NOTE: system property for maven.version may not exist if you use -Dtest
        // surefire parameter to run this single test. Therefore, the plugin writes
        // the maven version into the check file.
        String mavenVersion = ( (String) lines.get( 0 ) ).substring( 0, 3 );
        String javaVersion = (String) lines.get( 1 );
        String os = (String) lines.get( 2 ) + " " + (String) lines.get( 3 );
        String artifactVersion = (String) lines.get( 4 );

//        System.out.println( "Comparing User-Agent '" + userAgent + "' to 'Apache-Maven/" + mavenVersion + " (Java "
//            + javaVersion + "; " + os + ")" + " maven-artifact/" + artifactVersion + "'" );
        assertEquals( "Comparing User-Agent '" + userAgent + "'", "Apache-Maven/" + mavenVersion + " (Java "
            + javaVersion + "; " + os + ")" + " maven-artifact/" + artifactVersion, userAgent );
    }

    public void testmng3652_UnConfiguredDAV()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-3652" );
        File pluginDir = new File( testDir, "test-plugin" );
        File projectDir = new File( testDir, "test-project" );

        Verifier verifier = new Verifier( pluginDir.getAbsolutePath() );
        verifier.executeGoal( "install" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        verifier = new Verifier( projectDir.getAbsolutePath() );

        // test webdav
        List cliOptions = new ArrayList();
        cliOptions.add( "-DtestPort=" + port );
        cliOptions.add( "-DtestProtocol=dav:http" );
        cliOptions.add( "-X" );
        verifier.setCliOptions( cliOptions );

        verifier.setLogFileName( "log-unConfiguredDAV.txt" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        File touchFile = new File( projectDir, "target/touch.txt" );
        assertTrue( touchFile.exists() );

        List lines = verifier.loadFile( touchFile, false );

        // NOTE: system property for maven.version may not exist if you use -Dtest
        // surefire parameter to run this single test. Therefore, the plugin writes
        // the maven version into the check file.
        String mavenVersion = ( (String) lines.get( 0 ) ).substring( 0, 3 );
        String javaVersion = (String) lines.get( 1 );
        String os = (String) lines.get( 2 ) + " " + (String) lines.get( 3 );
        String artifactVersion = (String) lines.get( 4 );

        String userAgent = this.userAgent;
        assertNotNull( userAgent );

//        System.out.println( "Comparing User-Agent '" + userAgent + "' to 'Apache-Maven/" + mavenVersion + " (Java "
//            + javaVersion + "; " + os + ")" + " maven-artifact/" + artifactVersion + "'" );
        assertEquals( "Comparing User-Agent '" + userAgent + "'", "Apache-Maven/" + mavenVersion + " (Java "
            + javaVersion + "; " + os + ")" + " maven-artifact/" + artifactVersion, userAgent );
    }

    public void testmng3652_ConfigurationInSettingsWithoutUserAgent()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-3652" );
        File pluginDir = new File( testDir, "test-plugin" );
        File projectDir = new File( testDir, "test-project" );

        Verifier verifier = new Verifier( pluginDir.getAbsolutePath() );
        verifier.executeGoal( "install" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        verifier = new Verifier( projectDir.getAbsolutePath() );

        // test settings with no config

        List cliOptions = new ArrayList();
        cliOptions.add( "-DtestPort=" + port );
        cliOptions.add( "--settings" );
        cliOptions.add( "settings-no-config.xml" );
        cliOptions.add( "-X" );
        verifier.setCliOptions( cliOptions );

        verifier.setLogFileName( "log-configWithoutUserAgent.txt" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        File touchFile = new File( projectDir, "target/touch.txt" );
        assertTrue( touchFile.exists() );

        List lines = verifier.loadFile( touchFile, false );

        // NOTE: system property for maven.version may not exist if you use -Dtest
        // surefire parameter to run this single test. Therefore, the plugin writes
        // the maven version into the check file.
        String mavenVersion = ( (String) lines.get( 0 ) ).substring( 0, 3 );
        String javaVersion = (String) lines.get( 1 );
        String os = (String) lines.get( 2 ) + " " + (String) lines.get( 3 );
        String artifactVersion = (String) lines.get( 4 );

        String userAgent = this.userAgent;
        assertNotNull( userAgent );

//        System.out.println( "Comparing User-Agent '" + userAgent + "' to 'Apache-Maven/" + mavenVersion + " (Java "
//            + javaVersion + "; " + os + ")" + " maven-artifact/" + artifactVersion + "'" );
        assertEquals( "Comparing User-Agent '" + userAgent + "'", "Apache-Maven/" + mavenVersion + " (Java "
            + javaVersion + "; " + os + ")" + " maven-artifact/" + artifactVersion, userAgent );
    }

    public void testmng3652_UserAgentConfiguredInSettings()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-3652" );
        File pluginDir = new File( testDir, "test-plugin" );
        File projectDir = new File( testDir, "test-project" );

        Verifier verifier = new Verifier( pluginDir.getAbsolutePath() );
        verifier.executeGoal( "install" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        verifier = new Verifier( projectDir.getAbsolutePath() );

        // test settings with config

        List cliOptions = new ArrayList();
        cliOptions.add( "-DtestPort=" + port );
        cliOptions.add( "--settings" );
        cliOptions.add( "settings.xml" );
        cliOptions.add( "-X" );
        verifier.setCliOptions( cliOptions );

        verifier.setLogFileName( "log-configWithUserAgent.txt" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        String userAgent = this.userAgent;
        assertNotNull( userAgent );

//        System.out.println( "Comparing User-Agent: '" + userAgent + "' to 'Maven Fu'" );
        assertEquals( "Maven Fu", userAgent );
    }
}
