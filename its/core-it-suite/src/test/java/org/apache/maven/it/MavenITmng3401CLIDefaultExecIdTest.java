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
 *  http://www.apache.org/licenses/LICENSE-2.0
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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-3401">MNG-3401</a>.
 *
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 * @author jdcasey
 */
public class MavenITmng3401CLIDefaultExecIdTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng3401CLIDefaultExecIdTest()
    {
        super( "[2.2.0,)" );
    }

    /**
     * Test that the configuration of an execution block with the id "default-cli" applies to direct CLI
     * invocations of a goal as well if the plugin is configured under build/plugins.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitWithoutPluginManagement()
        throws Exception
    {
        testit( "without-mgmt" );
    }

    /**
     * Test that the configuration of an execution block with the id "default-cli" applies to direct CLI
     * invocations of a goal as well if the plugin is configured under build/pluginManagement/plugins.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitWithPluginManagement()
        throws Exception
    {
        testit( "with-mgmt" );
    }

    private void testit( String project )
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-3401/" + project );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.executeGoal( "org.apache.maven.its.plugins:maven-it-plugin-configuration:2.1-SNAPSHOT:config" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        Properties props = verifier.loadProperties( "target/config.properties" );

        assertEquals( "PASSED", props.getProperty( "stringParam" ) );

        assertEquals( "4", props.getProperty( "stringParams" ) );
        assertEquals( "a", props.getProperty( "stringParams.0" ) );
        assertEquals( "c", props.getProperty( "stringParams.1" ) );
        assertEquals( "b", props.getProperty( "stringParams.2" ) );
        assertEquals( "d", props.getProperty( "stringParams.3" ) );

        assertEquals( "maven-core-it", props.getProperty( "defaultParam" ) );
    }

}
