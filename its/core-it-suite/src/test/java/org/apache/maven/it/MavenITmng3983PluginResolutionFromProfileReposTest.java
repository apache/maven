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

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-3983">MNG-3983</a>.
 *
 * @author Benjamin Bentmann
 *
 */
public class MavenITmng3983PluginResolutionFromProfileReposTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng3983PluginResolutionFromProfileReposTest()
    {
        super( ALL_MAVEN_VERSIONS );
    }

    /**
     * Test that plugins can be resolved from remote plugin repositories defined by (active) profiles in the POM.
     *
     * @throws Exception in case of failure
     */
    public void testitFromPom()
        throws Exception
    {
        requiresMavenVersion( "[2.0,3.0-alpha-1),[3.0-alpha-3,)" );

        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-3983/test-1" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setForkJvm( true ); // Don't lock up plugin files in class loader within current JVM
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.deleteArtifacts( "org.apache.maven.its.mng3983" );
        verifier.filterFile( "pom.xml", "pom.xml", "UTF-8", verifier.newDefaultFilterProperties() );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        verifier.verifyArtifactPresent( "org.apache.maven.its.mng3983", "p", "0.1", "jar" );
    }

    /**
     * Test that plugins can be resolved from remote plugin repositories defined by (active) profiles in settings.xml.
     *
     * @throws Exception in case of failure
     */
    public void testitFromSettings()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-3983/test-3" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setForkJvm( true ); // Don't lock up plugin files in class loader within current JVM
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.deleteArtifacts( "org.apache.maven.its.mng3983" );
        verifier.filterFile( "settings.xml", "settings.xml", "UTF-8", verifier.newDefaultFilterProperties() );
        verifier.addCliOption( "--settings" );
        verifier.addCliOption( "settings.xml" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        verifier.verifyArtifactPresent( "org.apache.maven.its.mng3983", "p", "0.1", "jar" );
    }

}
