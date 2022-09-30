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

import org.junit.jupiter.api.Test;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-469">MNG-469</a>.
 *
 * @author Benjamin Bentmann
 *
 */
public class MavenITmng0469ReportConfigTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng0469ReportConfigTest()
    {
        super( "[2.0.0,)" );
    }

    /**
     * Test that {@code <build>} configuration dominates {@code <reporting>} configuration for build goals.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitBuildConfigDominantDuringBuild()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-0469/test1" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.deleteDirectory( "target" );
        verifier.setAutoclean( false );
        verifier.executeGoal( "org.apache.maven.its.plugins:maven-it-plugin-configuration:2.1-SNAPSHOT:config" );
        verifier.verifyFilePresent( "target/build.txt" );
        verifier.verifyFileNotPresent( "target/reporting.txt" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
    }

    /**
     * Test that {@code <build>} configuration does not affect report goals.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitBuildConfigIrrelevantForReports()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-0469/test2" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath(), "remote" );
        verifier.deleteDirectory( "target" );
        verifier.setAutoclean( false );
        if ( matchesVersionRange( "(,3.0-alpha-1)" ) )
        {
            verifier.executeGoal( "org.apache.maven.its.plugins:maven-it-plugin-site:2.1-SNAPSHOT:generate" );
            verifier.verifyFilePresent( "target/site/info.properties" );
        }
        else
        {
            verifier.executeGoal( "validate" );
            Properties props = verifier.loadProperties( "target/config.properties" );
            assertEquals( "maven-it-plugin-site", props.getProperty( "project.reporting.plugins.0.artifactId" ) );
            assertNotEquals( "fail.properties", props.getProperty( "project.reporting.plugins.0.configuration.children.infoFile.0.value" ) );
        }
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
    }

}
