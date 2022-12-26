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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4452">MNG-4452</a>.
 *
 * @author Benjamin Bentmann
 */
public class MavenITmng4452ResolutionOfSnapshotWithClassifierTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng4452ResolutionOfSnapshotWithClassifierTest()
    {
        super( "[3.0-beta-4,)" );
    }

    /**
     * Test that snapshot artifacts with classifiers can be successfully resolved from remote repos with (unique
     * snapshots) when the last deployment to that repo didn't include that particular classifier. In other words,
     * the metadata in the repository needs to properly keep track of all snapshots and not just the last deployed
     * one. The same goes for snapshots that differ only by file extension.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testit()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-4452" );

        Verifier verifier = newVerifier( new File( testDir, "producer" ).getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.deleteArtifacts( "org.apache.maven.its.mng4452" );
        verifier.addCliArgument( "-Dmng4452.type=jar" );
        verifier.addCliArgument( "-Dmng4452.classifier=unix" );
        verifier.setLogFileName( "log-1.txt" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.addCliArgument( "-Dmng4452.type=jar" );
        verifier.addCliArgument( "-Dmng4452.classifier=win" );
        verifier.setLogFileName( "log-2.txt" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.addCliArgument( "-Dmng4452.type=war" );
        verifier.addCliArgument( "-Dmng4452.classifier=win" );
        verifier.setLogFileName( "log-3.txt" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        verifier = newVerifier( new File( testDir, "consumer" ).getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.deleteArtifacts( "org.apache.maven.its.mng4452" );
        verifier.addCliOption( "-s" );
        verifier.addCliOption( "settings.xml" );
        verifier.filterFile( "settings-template.xml", "settings.xml", "UTF-8", verifier.newDefaultFilterProperties() );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        List<String> artifacts = verifier.loadLines( "target/artifacts.txt", "UTF-8" );
        assertTrue( artifacts.toString(),
            artifacts.contains( "org.apache.maven.its.mng4452:producer:jar:unix:0.1-SNAPSHOT" ) );
        assertTrue( artifacts.toString(),
            artifacts.contains( "org.apache.maven.its.mng4452:producer:jar:win:0.1-SNAPSHOT" ) );
        assertTrue( artifacts.toString(),
            artifacts.contains( "org.apache.maven.its.mng4452:producer:war:win:0.1-SNAPSHOT" ) );
    }

}
