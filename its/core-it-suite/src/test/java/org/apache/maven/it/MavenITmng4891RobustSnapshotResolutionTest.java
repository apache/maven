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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4891">MNG-4891</a>.
 *
 * @author Benjamin Bentmann
 */
public class MavenITmng4891RobustSnapshotResolutionTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng4891RobustSnapshotResolutionTest()
    {
        super( "[2.0.3,3.0-alpha-1),[3.0.1,)" );
    }

    /**
     * Verify that resolution of a local snapshot still succeeds even if the maven-metadata-local.xml has been
     * corrupted by a remote repository that misuses the same repo id, i.e. "local".
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testit()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-4891" );

        Verifier verifier = newVerifier( new File( testDir, "producer" ).getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.deleteArtifacts( "org.apache.maven.its.mng4891" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        verifier = newVerifier( new File( testDir, "consumer" ).getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        List<String> artifacts = verifier.loadLines( "target/artifacts.txt", "UTF-8" );

        assertTrue( artifacts.toString(), artifacts.contains( "org.apache.maven.its.mng4891:producer:jar:0.1-SNAPSHOT" ) );
    }

}
