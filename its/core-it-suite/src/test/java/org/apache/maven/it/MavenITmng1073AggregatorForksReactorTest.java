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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-1073">MNG-1073</a>.
 *
 * @author Benjamin Bentmann
 */
public class MavenITmng1073AggregatorForksReactorTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng1073AggregatorForksReactorTest()
    {
        super( ALL_MAVEN_VERSIONS );
    }

    /**
     * Verify that aggregator mojos invoked from the CLI that fork the lifecycle do so for the entire reactor.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitForkLifecycle()
        throws Exception
    {
        // excluded 2.1.x and 2.2.x due to MNG-4325
        requiresMavenVersion( "[2.0,2.1.0),[3.0-alpha-3,)" );

        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-1073" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.deleteDirectory( "sub-1/target" );
        verifier.deleteDirectory( "sub-2/target" );
        verifier.setLogFileName( "log-lifecycle.txt" );
        verifier.addCliArgument( "org.apache.maven.its.plugins:maven-it-plugin-fork:2.1-SNAPSHOT:fork-lifecycle-aggregator" );
        verifier.execute();
        verifier.verifyErrorFreeLog();

        verifier.verifyFilePresent( "target/forked/touch.txt" );
        verifier.verifyFilePresent( "sub-1/target/forked/touch.txt" );
        verifier.verifyFilePresent( "sub-2/target/forked/touch.txt" );
    }

    /**
     * Verify that aggregator mojos invoked from the CLI that fork a goal do so for the entire reactor.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitForkGoal()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-1073" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.deleteDirectory( "sub-1/target" );
        verifier.deleteDirectory( "sub-2/target" );
        verifier.setLogFileName( "log-goal.txt" );
        verifier.addCliArgument( "org.apache.maven.its.plugins:maven-it-plugin-fork:2.1-SNAPSHOT:fork-goal-aggregator" );
        verifier.execute();
        verifier.verifyErrorFreeLog();

        verifier.verifyFilePresent( "target/touch.txt" );
        verifier.verifyFilePresent( "sub-1/target/touch.txt" );
        verifier.verifyFilePresent( "sub-2/target/touch.txt" );
    }

}
