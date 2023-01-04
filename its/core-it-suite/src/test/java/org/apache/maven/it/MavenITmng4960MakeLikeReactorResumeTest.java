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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4960">MNG-4960</a>.
 *
 * @author Benjamin Bentmann
 */
public class MavenITmng4960MakeLikeReactorResumeTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng4960MakeLikeReactorResumeTest()
    {
        super( "[2.1.0,3.0-alpha-1),[3.0.2,)" );
    }

    /**
     * Verify that the make-like reactor mode doesn't omit the selected projects when building their prerequisites
     * as well and resuming from one of them.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitFromUpstream()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-4960" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.deleteDirectory( "mod-a/target" );
        verifier.deleteDirectory( "mod-b/target" );
        verifier.addCliArgument( "--projects" );
        verifier.addCliArgument( "mod-b" );
        verifier.addCliArgument( "--also-make" );
        verifier.addCliArgument( "--resume-from" );
        verifier.addCliArgument( "mod-a" );
        verifier.setLogFileName( "log-up.txt" );
        verifier.addCliArgument( "validate" );
        verifier.execute();
        verifier.verifyErrorFreeLog();

        verifier.verifyFilePresent( "mod-a/target/touch.txt" );
        verifier.verifyFilePresent( "mod-b/target/touch.txt" );
        verifier.verifyFileNotPresent( "target/touch.txt" );
    }

    /**
     * Verify that the make-like reactor mode omits the selected project when building its dependents
     * as well and resuming from one of them.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitFromDownstream()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-4960" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.deleteDirectory( "mod-a/target" );
        verifier.deleteDirectory( "mod-b/target" );
        verifier.addCliArgument( "--projects" );
        verifier.addCliArgument( "mod-a" );
        verifier.addCliArgument( "--also-make-dependents" );
        verifier.addCliArgument( "--resume-from" );
        verifier.addCliArgument( "mod-b" );
        verifier.setLogFileName( "log-down.txt" );
        verifier.addCliArgument( "validate" );
        verifier.execute();
        verifier.verifyErrorFreeLog();

        verifier.verifyFileNotPresent( "mod-a/target/touch.txt" );
        verifier.verifyFilePresent( "mod-b/target/touch.txt" );
        verifier.verifyFileNotPresent( "target/touch.txt" );
    }

}
