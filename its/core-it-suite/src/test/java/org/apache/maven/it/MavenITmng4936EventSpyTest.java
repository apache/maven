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
import java.util.List;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4936">MNG-4936</a>.
 *
 * @author Benjamin Bentmann
 */
public class MavenITmng4936EventSpyTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng4936EventSpyTest()
    {
        super( "[3.0.2,)" );
    }

    /**
     * Verify that loading of an event spy extension from CLI works.
     *
     * @throws Exception in case of failure
     */
    public void testit()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-4936" );

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
        assertTrue( lines.toString(), lines.get( 0 ).toString().startsWith( "init" ) );
        assertTrue( lines.toString(), lines.get( lines.size() - 1 ).toString().startsWith( "close" ) );
        assertTrue( lines.toString(),
            lines.contains( "event: org.apache.maven.settings.building.DefaultSettingsBuildingRequest" ) );
        assertTrue( lines.toString(),
            lines.contains( "event: org.apache.maven.settings.building.DefaultSettingsBuildingResult" ) );
        assertTrue( lines.toString(),
            lines.contains( "event: org.apache.maven.execution.DefaultMavenExecutionRequest" ) );
        assertTrue( lines.toString(),
            lines.contains( "event: org.apache.maven.execution.DefaultMavenExecutionResult" ) );
        assertTrue( lines.toString(),
            lines.contains( "event: org.apache.maven.lifecycle.internal.DefaultExecutionEvent" ) );
    }

}
