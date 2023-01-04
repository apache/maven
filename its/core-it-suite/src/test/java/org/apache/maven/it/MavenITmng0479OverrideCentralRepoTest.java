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
import org.apache.maven.shared.verifier.VerificationException;

import java.io.File;
import java.util.Properties;

import org.junit.jupiter.api.Test;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-479">MNG-479</a>.
 *
 * @author Benjamin Bentmann
 *
 */
public class MavenITmng0479OverrideCentralRepoTest
    extends AbstractMavenIntegrationTestCase
{
    public MavenITmng0479OverrideCentralRepoTest()
    {
        super( "[2.0.3,3.0-alpha-1),[3.0-beta-3,)" );
    }

    /**
     *  Verify that using the same repo id allows to override "central". This test checks the effective model.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitModel()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-0479" );

        // Phase 1: Ensure the test plugin is downloaded before the test cuts off access to central
        File child1 = new File( testDir, "setup" );
        Verifier verifier = newVerifier( child1.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );

        verifier.executeGoal( "org.apache.maven.its.plugins:maven-it-plugin-expression:2.1-SNAPSHOT:eval" );
        verifier.verifyErrorFreeLog();

        // Phase 2: Now run the test
        File child2 = new File( testDir, "test" );
        verifier = newVerifier( child2.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );

        verifier.filterFile( "settings-template.xml", "settings.xml", "UTF-8", verifier.newDefaultFilterProperties() );
        verifier.addCliOption( "--settings" );
        verifier.addCliOption( "settings.xml" );
        verifier.executeGoal( "org.apache.maven.its.plugins:maven-it-plugin-expression:2.1-SNAPSHOT:eval" );
        verifier.verifyErrorFreeLog();

        verifier.verifyFilePresent( "target/expression.properties" );
        Properties props = verifier.loadProperties( "target/expression.properties" );

        int count = Integer.parseInt( props.getProperty( "project.repositories", "0" ) );
        assertTrue( count > 0 );
        for ( int i = 0; i < count; i++ )
        {
            String key = "project.repositories." + i;
            if ( "central".equals( props.getProperty( key + ".id" ) ) )
            {
                assertEquals( "mng-0479", props.getProperty( key + ".name" ) );
                assertTrue( props.getProperty( key + ".url" ).endsWith( "/target/mng-0479" ) );

                assertEquals( "false", props.getProperty( key + ".releases.enabled" ) );
                assertEquals( "ignore", props.getProperty( key + ".releases.checksumPolicy" ) );
                assertEquals( "always", props.getProperty( key + ".releases.updatePolicy" ) );

                assertEquals( "true", props.getProperty( key + ".snapshots.enabled" ) );
                assertEquals( "fail", props.getProperty( key + ".snapshots.checksumPolicy" ) );
                assertEquals( "never", props.getProperty( key + ".snapshots.updatePolicy" ) );
            }
        }

        count = Integer.parseInt( props.getProperty( "project.pluginRepositories", "0" ) );
        for ( int i = 0; i < count; i++ )
        {
            String key = "project.pluginRepositories." + i;
            if ( "central".equals( props.getProperty( key + ".id" ) ) )
            {
                assertEquals( "mng-0479", props.getProperty( key + ".name" ) );
                assertTrue( props.getProperty( key + ".url" ).endsWith( "/target/mng-0479" ) );

                assertEquals( "false", props.getProperty( key + ".releases.enabled" ) );
                assertEquals( "ignore", props.getProperty( key + ".releases.checksumPolicy" ) );
                assertEquals( "always", props.getProperty( key + ".releases.updatePolicy" ) );

                assertEquals( "true", props.getProperty( key + ".snapshots.enabled" ) );
                assertEquals( "fail", props.getProperty( key + ".snapshots.checksumPolicy" ) );
                assertEquals( "never", props.getProperty( key + ".snapshots.updatePolicy" ) );
            }
        }
    }

    /**
     *  Verify that using the same repo id allows to override "central". This test checks the actual repo access.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitResolution()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-0479" );

        Verifier verifier = newVerifier( new File( testDir, "test-1" ).getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.deleteArtifacts( "org.apache.maven.its.mng0479" );
        verifier.filterFile( "settings-template.xml", "settings.xml", "UTF-8", verifier.newDefaultFilterProperties() );
        verifier.addCliOption( "--settings" );
        verifier.addCliOption( "settings.xml" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();

        verifier.verifyFilePresent( "target/touch.txt" );
        verifier.verifyArtifactPresent( "org.apache.maven.its.mng0479", "parent", "0.1-SNAPSHOT", "pom" );
        verifier.verifyArtifactPresent( "org.apache.maven.its.mng0479", "a", "0.1-SNAPSHOT", "jar" );
        verifier.verifyArtifactPresent( "org.apache.maven.its.mng0479", "a", "0.1-SNAPSHOT", "pom" );
        verifier.verifyArtifactPresent( "org.apache.maven.its.mng0479", "a-parent", "0.1-SNAPSHOT", "pom" );
        verifier.verifyArtifactPresent( "org.apache.maven.its.mng0479", "b", "0.1-SNAPSHOT", "jar" );
        verifier.verifyArtifactPresent( "org.apache.maven.its.mng0479", "b", "0.1-SNAPSHOT", "pom" );

        verifier = newVerifier( new File( testDir, "test-2" ).getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.filterFile( "settings-template.xml", "settings.xml", "UTF-8", verifier.newDefaultFilterProperties() );
        verifier.addCliOption( "--settings" );
        verifier.addCliOption( "settings.xml" );
        try
        {
            verifier.executeGoal( "validate" );
            verifier.verifyErrorFreeLog();
            fail( "Build should have failed to resolve parent POM" );
        }
        catch ( VerificationException e )
        {
            // expected
        }

        verifier.verifyArtifactNotPresent( "org.apache.maven.its.mng0479", "parent", "0.1", "pom" );

        verifier = newVerifier( new File( testDir, "test-3" ).getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.filterFile( "settings-template.xml", "settings.xml", "UTF-8", verifier.newDefaultFilterProperties() );
        verifier.addCliOption( "--settings" );
        verifier.addCliOption( "settings.xml" );
        try
        {
            verifier.executeGoal( "org.apache.maven.its.mng0479:maven-mng0479-plugin:0.1-SNAPSHOT:touch" );
            verifier.verifyErrorFreeLog();
            fail( "Build should have failed to resolve direct dependency" );
        }
        catch ( VerificationException e )
        {
            // expected
        }

        verifier.verifyArtifactNotPresent( "org.apache.maven.its.mng0479", "a", "0.1", "jar" );
        verifier.verifyArtifactNotPresent( "org.apache.maven.its.mng0479", "a", "0.1", "pom" );

        verifier = newVerifier( new File( testDir, "test-4" ).getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.filterFile( "settings-template.xml", "settings.xml", "UTF-8", verifier.newDefaultFilterProperties() );
        verifier.addCliOption( "--settings" );
        verifier.addCliOption( "settings.xml" );
        try
        {
            verifier.executeGoal( "org.apache.maven.its.mng0479:maven-mng0479-plugin:0.1-SNAPSHOT:touch" );
            verifier.verifyErrorFreeLog();
            fail( "Build should have failed to resolve transitive dependency" );
        }
        catch ( VerificationException e )
        {
            // expected
        }

        verifier.verifyArtifactNotPresent( "org.apache.maven.its.mng0479", "b", "0.1", "jar" );
        verifier.verifyArtifactNotPresent( "org.apache.maven.its.mng0479", "b", "0.1", "pom" );
    }

}
