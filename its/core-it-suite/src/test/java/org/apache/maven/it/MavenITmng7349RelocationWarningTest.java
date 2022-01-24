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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.it.util.ResourceExtractor;

public class MavenITmng7349RelocationWarningTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng7349RelocationWarningTest()
    {
        super( "[3.8.5,)" );
    }

    public void testit()
            throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(),
                    "/mng-7349-relocation-warning" );
        File artifactsDir = new File( testDir, "artifacts" );
        File projectDir = new File( testDir, "project" );

        Verifier verifier;

        verifier = newVerifier( artifactsDir.getAbsolutePath() );
        verifier.executeGoal( "install" );
        verifier.resetStreams();
        verifier.verifyErrorFreeLog();

        verifier = newVerifier( projectDir.getAbsolutePath() );
        verifier.executeGoal( "verify" );
        verifier.resetStreams();
        verifier.verifyErrorFreeLog();
        List<String> lines = verifier.loadLines( verifier.getLogFileName(), "UTF-8" );
        List<String> relocated = new ArrayList<>();
        for (String line : lines) {
            if (line.contains("has been relocated")) {
                relocated.add(line);
            }
        }
        assertEquals("Expected 2 relocations, but found multiple",
                     2, relocated.size());
        assertTrue("Expected the relocation messages to be logged",
                    relocated.get(0).contains("Test relocation reason for old-plugin"));
        assertTrue("Expected the relocation messages to be logged",
                    relocated.get(1).contains("Test relocation reason for old-dep"));
    }
}
