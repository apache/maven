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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-612">MNG-612</a>.
 *
 * @author Mark Hobson
 *
 */
public class MavenITmng0612NewestConflictResolverTest
    extends AbstractMavenIntegrationTestCase
{
    public MavenITmng0612NewestConflictResolverTest()
    {
        super( ALL_MAVEN_VERSIONS );
    }

    /**
     * Test that ensures the newest-wins conflict resolver is used.
     *
     * @throws Exception in case of failure
     */
    public void testitMNG612()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-0612/dependency" );
        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.executeGoal( "install" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-0612/plugin" );
        verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.executeGoal( "install" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-0612/project" );
        verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.executeGoal( "verify" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
    }
}
