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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-3775">MNG-3775</a>.
 *
 * @author Benjamin Bentmann
 */
public class MavenITmng3775ConflictResolutionBacktrackingTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng3775ConflictResolutionBacktrackingTest()
    {
        super( "[3.0,)" );
    }

    @Test
    public void testitABC()
        throws Exception
    {
        testit( "test-abc" );
    }

    @Test
    public void testitACB()
        throws Exception
    {
        testit( "test-acb" );
    }

    @Test
    public void testitBAC()
        throws Exception
    {
        testit( "test-bac" );
    }

    @Test
    public void testitBCA()
        throws Exception
    {
        testit( "test-bca" );
    }

    @Test
    public void testitCAB()
        throws Exception
    {
        testit( "test-cab" );
    }

    @Test
    public void testitCBA()
        throws Exception
    {
        testit( "test-cba" );
    }

    /**
     * Verify that conflict resolution doesn't select nodes which are children of eventually disabled nodes.
     * In other words, when a subtree gets disabled, all previously selected winners among the children need to
     * be revised.
     */
    private void testit( String project )
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-3775" );

        Verifier verifier = newVerifier( new File( testDir, project ).getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.deleteArtifacts( "org.apache.maven.its.mng3775" );
        verifier.addCliOption( "-s" );
        verifier.addCliOption( "settings.xml" );
        verifier.filterFile( "../settings-template.xml", "settings.xml", "UTF-8", verifier.newDefaultFilterProperties() );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();

        List<String> test = verifier.loadLines( "target/test.txt", "UTF-8" );

        assertTrue( project + " > " + test.toString(), test.contains( "a-0.1.jar" ) );
        assertTrue( project + " > " + test.toString(), test.contains( "b-0.1.jar" ) );
        assertTrue( project + " > " + test.toString(), test.contains( "x-0.1.jar" ) );
        assertTrue( project + " > " + test.toString(), test.contains( "c-0.1.jar" ) );
    }

}
