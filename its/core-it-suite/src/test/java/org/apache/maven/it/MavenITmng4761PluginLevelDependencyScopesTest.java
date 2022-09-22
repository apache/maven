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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4761">MNG-4761</a>.
 *
 * @author jdcasey
 */
public class MavenITmng4761PluginLevelDependencyScopesTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng4761PluginLevelDependencyScopesTest()
    {
        super( "(2.2.1,2.99)(3.0-beta-2,)" );
    }

    /**
     * Verify that plugin-level dependencies specified in a user's pom.xml DO NOT use compile scope.
     * Using any scope other than runtime for plugin dependencies may favor them and their transitive
     * dependencies inappropriately, leading to unpredictable results.
     *
     * Plugin-dependency scope should be DISREGARDED, and runtime scope should be forced.
     *
     * @throws Exception in case of failure
     */
    public void testit()
        throws Exception
    {
        final File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-4761" );

        final Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.deleteArtifacts( "org.apache.maven.its.mng4761" );
        verifier.filterFile( "settings-template.xml", "settings.xml", "UTF-8", verifier.newDefaultFilterProperties() );
        verifier.addCliOption( "-s" );
        verifier.addCliOption( "settings.xml" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
    }

}
