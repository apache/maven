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

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-1088">MNG-1088</a>.
 *
 * @author Brett Porter
 *
 */
public class MavenITmng1088ReactorPluginResolutionTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng1088ReactorPluginResolutionTest()
    {
        super( ALL_MAVEN_VERSIONS );
    }

    /**
     * Test that the plugin manager falls back to resolution from the repository if a plugin is part of the reactor
     * (i.e. an active project artifact) but the lifecycle has not been executed far enough to produce a file for
     * the plugin (i.e. a phase before "compile").
     *
     * @throws Exception in case of failure
     */
    public void testitMNG1088()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-1088" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "client/target" );
        verifier.deleteArtifacts( "org.apache.maven.its.mng1088" );
        verifier.filterFile( "settings-template.xml", "settings.xml", "UTF-8", verifier.newDefaultFilterProperties() );
        verifier.addCliOption( "--settings" );
        verifier.addCliOption( "settings.xml" );
        // NOTE: It's essential part of the test to invoke a phase before "compile"
        verifier.executeGoal( "initialize" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        verifier.verifyFilePresent( "client/target/touch.txt" );
    }

}
