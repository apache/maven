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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4872">MNG-4872</a>.
 *
 * @author Benjamin Bentmann
 */
public class MavenITmng4872ReactorResolutionAttachedWithExclusionsTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng4872ReactorResolutionAttachedWithExclusionsTest()
    {
        super( "[3.0-beta-1,)" );
    }

    /**
     * Test that resolution of (attached) artifacts from the reactor doesn't cause exclusions to be lost.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testit()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-4872" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "consumer/target" );
        verifier.deleteArtifacts( "org.apache.maven.its.mng4872" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();

        List<String> artifacts = verifier.loadLines( "consumer/target/artifacts.txt", "UTF-8" );

        assertTrue( artifacts.toString(), artifacts.contains( "org.apache.maven.its.mng4872:producer:jar:0.1" ) );
        assertTrue( artifacts.toString(), artifacts.contains( "org.apache.maven.its.mng4872:producer:jar:shaded:0.1" ) );
        assertFalse( artifacts.toString(), artifacts.contains( "org.apache.maven.its.mng4872:excluded:jar:0.1" ) );
    }

}
