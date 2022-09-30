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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-6558">MNG-6558</a>.
 */
public class MavenITmng6558ToolchainsBuildingEventTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng6558ToolchainsBuildingEventTest()
    {
        super( "[3.6.1,)" );
    }

    /**
     * Verify that <code>ToolchainsBuildingRequest</code> and <code>ToolchainsBuildingResult</code> events are sent to event spy.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testit()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-6558" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setForkJvm( true );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.setSystemProperty( "maven.ext.class.path", "spy-0.1.jar" );
        verifier.addCliOption( "-X" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        List<String> lines = verifier.loadLines( "target/spy.log", "UTF-8" );
        assertTrue( lines.toString(), lines.get( 0 ).startsWith( "init" ) );
        assertTrue( lines.toString(), lines.get( lines.size() - 1 ).startsWith( "close" ) );
        assertTrue( lines.toString(),
            lines.contains( "event: org.apache.maven.toolchain.building.DefaultToolchainsBuildingRequest" ) );
        assertTrue( lines.toString(),
            lines.contains( "event: org.apache.maven.toolchain.building.DefaultToolchainsBuildingResult" ) );
    }

}
