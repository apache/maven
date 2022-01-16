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
import java.util.Properties;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-674">MNG-674</a> and
 * <a href="https://issues.apache.org/jira/browse/MNG-4997">MNG-4997</a>.
 *
 * @author John Casey
 *
 */
public class MavenITmng0674PluginParameterAliasTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng0674PluginParameterAliasTest()
    {
        super( "[3.0,)" );
    }

    /**
     * Test parameter alias usage for lifecycle-bound goal execution.
     *
     * @throws Exception in case of failure
     */
    public void testitLifecycle()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-0674" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.setLogFileName( "log-lifecycle.txt" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        Properties props = verifier.loadProperties( "target/config.properties" );
        assertEquals( "MNG-674-1", props.getProperty( "aliasParam" ) );
        assertEquals( "MNG-674-2", props.getProperty( "aliasDefaultExpressionParam" ) );
        assertEquals( "MNG-4997", props.getProperty( "aliasStringParams.0" ) );
    }

    /**
     * Test parameter alias usage for direct goal execution from CLI.
     *
     * @throws Exception in case of failure
     */
    public void testitCli()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-0674" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.setLogFileName( "log-cli.txt" );
        verifier.executeGoal( "org.apache.maven.its.plugins:maven-it-plugin-configuration:config" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        Properties props = verifier.loadProperties( "target/config.properties" );
        assertEquals( "MNG-674-1", props.getProperty( "aliasParam" ) );
        assertEquals( "MNG-674-2", props.getProperty( "aliasDefaultExpressionParam" ) );
        assertEquals( "MNG-4997", props.getProperty( "aliasStringParams.0" ) );
    }

}
