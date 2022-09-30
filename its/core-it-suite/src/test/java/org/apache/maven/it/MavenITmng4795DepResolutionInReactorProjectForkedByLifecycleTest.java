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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4795">MNG-4795</a>.
 *
 * @author Benjamin Bentmann
 */
public class MavenITmng4795DepResolutionInReactorProjectForkedByLifecycleTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng4795DepResolutionInReactorProjectForkedByLifecycleTest()
    {
        super( "[2.0.3,3.0-alpha-1),[3.0-beta-4,)" );
    }

    /**
     * Test that reactor projects forked by an aggregator mojo bound to a lifecycle phase are subject to dependency
     * resolution as required by their respective build plugins.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testit()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-4795" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.deleteDirectory( "sub/target" );
        verifier.executeGoal( "process-sources" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        List<String> compile0 = verifier.loadLines( "sub/target/compile-0.txt", "UTF-8" );
        assertTrue( compile0.toString(), compile0.contains( "maven-core-it-support-1.0.jar" ) );

        List<String> compile1 = verifier.loadLines( "sub/target/compile-1.txt", "UTF-8" );
        assertTrue( compile1.toString(), compile1.contains( "maven-core-it-support-1.0.jar" ) );
    }

}
