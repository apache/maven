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
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.server.NetworkConnector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.util.security.Constraint;
import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.util.security.Password;
import org.eclipse.jetty.servlet.ServletContextHandler;

import static org.eclipse.jetty.servlet.ServletContextHandler.SECURITY;
import static org.eclipse.jetty.servlet.ServletContextHandler.SESSIONS;
import static org.eclipse.jetty.util.security.Constraint.__BASIC_AUTH;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-553">MNG-553</a>.
 *
 * @author Benjamin Bentmann
 */
public class MavenITmng0553SettingsAuthzEncryptionTest
    extends AbstractMavenIntegrationTestCase
{

    private File testDir;

    private Server server;

    private int port;

    public MavenITmng0553SettingsAuthzEncryptionTest()
    {
        super( "[2.1.0,3.0-alpha-1),[3.0-alpha-3,)" );
    }

    @Override
    protected void setUp()
        throws Exception
    {
        testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-0553" );

        Constraint constraint = new Constraint( __BASIC_AUTH, "user" );
        constraint.setAuthenticate( true );

        ConstraintMapping constraintMapping = new ConstraintMapping();
        constraintMapping.setConstraint( constraint );
        constraintMapping.setPathSpec( "/*" );

        HashLoginService userRealm = new HashLoginService( "TestRealm" );
        userRealm.putUser( "testuser", new Password( "testtest" ), new String[] { "user" } );

        server = new Server( 0 );
        ServletContextHandler ctx = new ServletContextHandler( server, "/", SESSIONS | SECURITY );
        ConstraintSecurityHandler securityHandler = (ConstraintSecurityHandler) ctx.getSecurityHandler();
        securityHandler.setLoginService( userRealm );
        securityHandler.setAuthMethod( __BASIC_AUTH );
        securityHandler.setConstraintMappings( new ConstraintMapping[] { constraintMapping } );

        ResourceHandler repoHandler = new ResourceHandler();
        repoHandler.setResourceBase( new File( testDir, "repo" ).getAbsolutePath() );

        HandlerList handlerList = new HandlerList();
        handlerList.addHandler( securityHandler );
        handlerList.addHandler( repoHandler );
        handlerList.addHandler( new DefaultHandler() );

        server.setHandler( handlerList );
        server.start();
        if ( server.isFailed() )
        {
            fail( "Couldn't bind the server socket to a free port!" );
        }
        port = ( (NetworkConnector) server.getConnectors()[0] ).getLocalPort();
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
     * Test that the encrypted auth infos given in the settings.xml are decrypted.
     *
     * @throws Exception in case of failure
     */
    public void testitBasic()
        throws Exception
    {
        testDir = new File( testDir, "test-1" );

        Properties filterProps = new Properties();
        filterProps.setProperty( "@port@", Integer.toString( port ) );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteArtifacts( "org.apache.maven.its.mng0553" );
        verifier.assertArtifactNotPresent( "org.apache.maven.its.mng0553", "a", "0.1-SNAPSHOT", "jar" );
        verifier.filterFile( "settings-template.xml", "settings.xml", "UTF-8", filterProps );
        setUserHome( verifier, new File( testDir, "userhome" ) );
        verifier.addCliOption( "--settings" );
        verifier.addCliOption( "settings.xml" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        verifier.assertArtifactPresent( "org.apache.maven.its.mng0553", "a", "0.1-SNAPSHOT", "jar" );
    }

    /**
     * Test that the encrypted auth infos given in the settings.xml are decrypted when the master password resides
     * in an external file.
     *
     * @throws Exception in case of failure
     */
    public void testitRelocation()
        throws Exception
    {
        testDir = new File( testDir, "test-2" );

        Properties filterProps = new Properties();
        filterProps.setProperty( "@port@", Integer.toString( port ) );
        // NOTE: The upper-case scheme name is essential part of the test
        String secUrl = "FILE://" + new File( testDir, "relocated-settings-security.xml" ).toURI().getRawPath();
        filterProps.setProperty( "@relocation@", secUrl );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteArtifacts( "org.apache.maven.its.mng0553" );
        verifier.assertArtifactNotPresent( "org.apache.maven.its.mng0553", "a", "0.1-SNAPSHOT", "jar" );

        // NOTE: The tilde ~ in the file name is essential part of the test
        verifier.filterFile( "security-template.xml", "settings~security.xml", "UTF-8", filterProps );
        verifier.filterFile( "settings-template.xml", "settings.xml", "UTF-8", filterProps );

        verifier.getSystemProperties().setProperty( "settings.security",
            new File( testDir, "settings~security.xml" ).getAbsolutePath() );
        verifier.addCliOption( "--settings" );
        verifier.addCliOption( "settings.xml" );
        // NOTE: The selection of the Turkish language for the JVM locale is essential part of the test
        verifier.executeGoal( "validate", Collections.singletonMap( "MAVEN_OPTS", "-Duser.language=tr" ) );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        verifier.assertArtifactPresent( "org.apache.maven.its.mng0553", "a", "0.1-SNAPSHOT", "jar" );
    }

    /**
     * Test that the CLI supports generation of encrypted (master) passwords.
     *
     * @throws Exception in case of failure
     */
    public void testitEncryption()
        throws Exception
    {
        requiresMavenVersion( "[2.1.0,3.0-alpha-1),[3.0-alpha-7,)" );

        testDir = new File( testDir, "test-3" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        setUserHome( verifier, new File( testDir, "userhome" ) );
        verifier.addCliOption( "--encrypt-master-password" );
        verifier.addCliOption( "test" );
        verifier.setLogFileName( "log-emp.txt" );
        verifier.executeGoal( "-e" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        List<String> log = verifier.loadLines( verifier.getLogFileName(), null );
        assertNotNull( findPassword( log ) );

        verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        setUserHome( verifier, new File( testDir, "userhome" ) );
        verifier.addCliOption( "--encrypt-password" );
        verifier.addCliOption( "testpass" );
        verifier.setLogFileName( "log-ep.txt" );
        verifier.executeGoal( "-e" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        log = verifier.loadLines( verifier.getLogFileName(), null );
        assertNotNull( findPassword( log ) );
    }

    private String findPassword( List<String> log )
    {
        for ( String line : log )
        {
            if ( line.matches( ".*\\{[A-Za-z0-9+/=]+\\}.*" ) )
            {
                return line;
            }
        }

        return null;
    }

    private void setUserHome( Verifier verifier, File home )
    {
        // NOTE: We set the user.home directory instead of say settings.security to reflect Maven's normal behavior
        String path = home.getAbsolutePath();
        if ( path.indexOf( ' ' ) < 0 )
        {
            verifier.setEnvironmentVariable( "MAVEN_OPTS", "-Duser.home=" + path );
        }
        else
        {
            verifier.setEnvironmentVariable( "MAVEN_OPTS", "\"-Duser.home=" + path + "\"" );
        }
    }
}
