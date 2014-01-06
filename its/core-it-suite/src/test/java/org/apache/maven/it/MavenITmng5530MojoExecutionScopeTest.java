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

public class MavenITmng5530MojoExecutionScopeTest
    extends AbstractMavenIntegrationTestCase
{
    public MavenITmng5530MojoExecutionScopeTest()
    {
        super( "[3.1.2,)" );
    }

    public void test_copyfiles()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-5530");
        File pluginDir = new File( testDir, "plugin" );
        File projectDir = new File( testDir, "basic" );

        Verifier verifier;

        // install the test plugin
        verifier = newVerifier( pluginDir.getAbsolutePath(), "remote" );
        verifier.executeGoal( "install" );
        verifier.resetStreams();
        verifier.verifyErrorFreeLog();

        // build the test project
        verifier = newVerifier( projectDir.getAbsolutePath(), "remote" );
        verifier.executeGoal( "package" );
        verifier.resetStreams();
        verifier.verifyErrorFreeLog();

        //verifier.assertFilePresent( "target/execution-failure.txt" );
        verifier.assertFilePresent( "target/execution-success.txt" );
        verifier.assertFilePresent( "target/execution-before.txt" );
    }
}
