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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-3586">MNG-3586</a>.
 *
 * @author Benjamin Bentmann
 *
 */
public class MavenITmng3586SystemScopePluginDependencyTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng3586SystemScopePluginDependencyTest()
    {
        super( ALL_MAVEN_VERSIONS );
    }

    /**
     * Test that plugin dependencies with scope system are part of the plugin class realm. This test checks
     * dependencies that are declared in the plugin POM.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitFromPlugin()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-3586/test-1" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.deleteArtifacts( "org.apache.maven.its.mng3586" );
        verifier.getSystemProperties().setProperty( "test.home", testDir.getAbsolutePath() );
        verifier.filterFile( "settings-template.xml", "settings.xml", "UTF-8", verifier.newDefaultFilterProperties() );
        verifier.addCliOption( "--settings" );
        verifier.addCliOption( "settings.xml" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        Properties props = verifier.loadProperties( "target/it.properties" );
        assertEquals( "PASSED", props.getProperty( "test" ) );
    }

    /**
     * Test that plugin dependencies with scope system are part of the plugin class realm. This test checks
     * dependencies that are declared in the project POM that uses the plugin.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitFromProject()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-3586/test-2" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.getSystemProperties().setProperty( "test.home", testDir.getAbsolutePath() );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        Properties props = verifier.loadProperties( "target/pcl.properties" );
        assertEquals( "1", props.getProperty( "maven-core-it.properties.count" ) );
    }

}
