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

import java.io.File;
import java.util.Properties;

import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.server.NetworkConnector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.security.Constraint;
import org.eclipse.jetty.util.security.Password;
import org.junit.jupiter.api.Test;

import static org.eclipse.jetty.servlet.ServletContextHandler.SECURITY;
import static org.eclipse.jetty.servlet.ServletContextHandler.SESSIONS;
import static org.eclipse.jetty.util.security.Constraint.__BASIC_AUTH;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4561">MNG-4561</a>.
 *
 * @author Benjamin Bentmann
 */
public class MavenITmng4561MirroringOfPluginRepoTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng4561MirroringOfPluginRepoTest()
    {
        super( "[2.0.3,3.0-alpha-1),[3.0-alpha-7,)" );
    }

    /**
     * Test that repositories contributed by plugin POMs during transitive dependency resolution are subject to
     * mirror, proxy and authentication configuration.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testit()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-4561" );

        Constraint constraint = new Constraint();
        constraint.setName( Constraint.__BASIC_AUTH );
        constraint.setRoles( new String[] { "user" } );
        constraint.setAuthenticate( true );

        ConstraintMapping constraintMapping = new ConstraintMapping();
        constraintMapping.setConstraint( constraint );
        constraintMapping.setPathSpec( "/*" );

        HashLoginService userRealm = new HashLoginService( "TestRealm" );
        userRealm.putUser( "testuser", new Password( "testtest" ), new String[] { "user" } );

        Server server = new Server( 0 );
        ServletContextHandler ctx = new ServletContextHandler( server, "/", SESSIONS | SECURITY );
        ConstraintSecurityHandler securityHandler = (ConstraintSecurityHandler) ctx.getSecurityHandler();
        securityHandler.setLoginService( userRealm );
        securityHandler.setAuthMethod( __BASIC_AUTH );
        securityHandler.setConstraintMappings( new ConstraintMapping[] { constraintMapping } );

        ResourceHandler repoHandler = new ResourceHandler();
        repoHandler.setResourceBase( testDir.getAbsolutePath() );

        HandlerList handlerList = new HandlerList();
        handlerList.addHandler( securityHandler );
        handlerList.addHandler( repoHandler );
        handlerList.addHandler( new DefaultHandler() );

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
            verifier.deleteArtifacts( "org.apache.maven.its.mng4561" );
            Properties filterProps = verifier.newDefaultFilterProperties();
            filterProps.setProperty( "@port@", Integer.toString( port ) );
            verifier.filterFile( "settings-template.xml", "settings.xml", "UTF-8", filterProps );
            verifier.addCliOption( "-s" );
            verifier.addCliOption( "settings.xml" );
            verifier.executeGoal( "validate" );
            verifier.verifyErrorFreeLog();
            verifier.resetStreams();

            verifier.verifyArtifactPresent( "org.apache.maven.its.mng4561", "plugin-dep", "0.1", "jar" );
            verifier.verifyArtifactPresent( "org.apache.maven.its.mng4561", "plugin-dep", "0.1", "pom" );
            verifier.verifyFilePresent( "target/touch.txt" );
        }
        finally
        {
            server.stop();
            server.join();
        }
    }
}
