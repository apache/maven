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

import org.apache.maven.it.util.ResourceExtractor;

public class MavenITmng5561PluginRelocationLosesConfigurationTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng5561PluginRelocationLosesConfigurationTest()
    {
        super( "[3.8.5,)" );
    }

    public void testit()
            throws Exception
    {
        File testDir =
                ResourceExtractor.simpleExtractResources( getClass(),
                                                          "/mng-5561-plugin-relocation-loses-configuration" );
        File oldPluginWithRelocationDir = new File( testDir, "old-plugin-with-relocation" );
        File newPluginDir = new File( testDir, "new-plugin" );
        File projectDir = new File( testDir, "project" );

        Verifier verifier;

        verifier = newVerifier( oldPluginWithRelocationDir.getAbsolutePath() );
        verifier.executeGoal( "install" );
        verifier.resetStreams();
        verifier.verifyErrorFreeLog();

        verifier = newVerifier( newPluginDir.getAbsolutePath() );
        verifier.executeGoal( "install" );
        verifier.resetStreams();
        verifier.verifyErrorFreeLog();

        verifier = newVerifier( projectDir.getAbsolutePath() );
        verifier.executeGoal( "verify" );
        verifier.resetStreams();
        verifier.verifyErrorFreeLog();
        verifier.verifyTextInLog( "[WARNING] Hello from Maven!" );
    }
}
