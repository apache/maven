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
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-5137">MNG-5137</a>.
 *
 * @author Benjamin Bentmann
 */
public class MavenITmng5137ReactorResolutionInForkedBuildTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng5137ReactorResolutionInForkedBuildTest()
    {
        super( "[3.0.4,)" );
    }

    /**
     * Verify that reactor resolution also works within a forked multi-module lifecycle, i.e. a lifecycle fork caused
     * by an aggregator mojo. Here, reactor resolution needs to search the forked project instances for build output,
     * not the project instances from the main build.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testit()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-5137" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.deleteDirectory( "producer/target" );
        verifier.deleteDirectory( "consumer/target" );
        verifier.deleteArtifacts( "org.apache.maven.its.mng5137" );
        verifier.executeGoal(
                "org.apache.maven.its.plugins:maven-it-plugin-fork:2.1-SNAPSHOT:fork-lifecycle-aggregator" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        List<String> compile = verifier.loadLines( "consumer/target/compile.txt", "UTF-8" );

        assertTrue( compile.toString(), compile.contains( "producer/pom.xml" ) );
    }

}
