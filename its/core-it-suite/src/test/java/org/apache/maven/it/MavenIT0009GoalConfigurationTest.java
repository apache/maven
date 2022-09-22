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

public class MavenIT0009GoalConfigurationTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenIT0009GoalConfigurationTest()
    {
        super( ALL_MAVEN_VERSIONS );
    }

    /**
     * Test plugin configuration and goal configuration that overrides what the
     * mojo has specified.
     *
     * @throws Exception in case of failure
     */
    public void testit0009()
        throws Exception
    {

        boolean supportSpaceInXml = matchesVersionRange( "[3.1.0,)");

        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/it0009" );
        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.executeGoal( "validate" );
        verifier.verifyFilePresent( supportSpaceInXml ? "target/  pluginItem  " : "target/pluginItem");
        verifier.verifyFilePresent( "target/goalItem" );
        verifier.verifyFileNotPresent( "target/bad-item" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
    }

}
