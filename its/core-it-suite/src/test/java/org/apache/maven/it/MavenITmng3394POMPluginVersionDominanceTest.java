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

import org.junit.jupiter.api.Test;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-3394">MNG-3394</a>:
 * it checks that plugin versions in the POM obey the correct order
 * of precedence. Specifically, that mojos in the default lifecycle
 * bindings can find plugin versions in the pluginManagement section
 * when the build/plugins section is missing that plugin, and that
 * plugin versions in build/plugins override those in build/pluginManagement.
 */
public class MavenITmng3394POMPluginVersionDominanceTest
    extends AbstractMavenIntegrationTestCase
{

    private static final String BASEDIR_PREFIX = "/mng-3394/";

    public MavenITmng3394POMPluginVersionDominanceTest()
    {
        super( "(2.0.8,)" ); // only test in 2.0.9+
    }

    @Test
    public void testitMNG3394a ()
        throws Exception
    {
        //testShouldUsePluginVersionFromPluginMgmtForLifecycleMojoWhenNotInBuildPlugins
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), BASEDIR_PREFIX + "lifecycleMojoVersionInPluginMgmt" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.executeGoal( "process-resources" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        verifier.verifyFilePresent( "target/resources-resources.txt" );
    }

    @Test
    public void testitMNG3394b()
        throws Exception
    {
        //testShouldPreferPluginVersionFromBuildPluginsOverThatInPluginMgmt
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), BASEDIR_PREFIX + "preferBuildPluginOverPluginMgmt" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );

        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.executeGoal( "clean" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        verifier.verifyFilePresent( "target/clean-clean.txt" );
    }

}
