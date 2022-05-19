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

import java.io.File;

import org.apache.maven.it.util.ResourceExtractor;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-3106">MNG-3106</a>:
 * it tests that profiles with multiple activators are activated
 * when any of the activators are on.
 *
 */
public class MavenITmng3106ProfileMultipleActivatorsTest
    extends AbstractMavenIntegrationTestCase
{
    public MavenITmng3106ProfileMultipleActivatorsTest()
    {
        super( "(2.0.9,3.2.2)" );
    }

    /**
     * Test build with two profiles, each with more than one activator.
     * The profiles should be activated even though only one of the activators
     * returns true.
     *
     * @throws Exception in case of failure
     */
    public void testProfilesWithMultipleActivators()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-3106" );

        Verifier verifier;

        verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.addCliOption( "-Dprofile1.on=true" );
        verifier.executeGoal( "validate" );

        verifier.verifyErrorFreeLog();
        verifier.verifyFilePresent( "target/profile1/touch.txt" );
        verifier.verifyFilePresent( "target/profile2/touch.txt" );
        verifier.resetStreams();
    }

}
