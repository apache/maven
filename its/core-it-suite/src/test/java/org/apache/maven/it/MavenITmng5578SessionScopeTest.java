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

import org.junit.jupiter.api.Test;

public class MavenITmng5578SessionScopeTest
    extends AbstractMavenIntegrationTestCase
{
    public MavenITmng5578SessionScopeTest()
    {
        super( "[3.2.4,)" );
    }

    @Test
    public void testBasic()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-5578-session-scope" );
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
        verifier.addCliOption( "-Dit-build-extensions=false" );
        verifier.executeGoal( "package" );
        verifier.resetStreams();
        verifier.verifyErrorFreeLog();
    }

    @Test
    public void testBasic_multithreaded()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-5578-session-scope" );
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
        verifier.addCliOption( "-Dit-build-extensions=false" );
        verifier.addCliOption( "--builder" );
        verifier.addCliOption( "multithreaded" );
        verifier.addCliOption( "-T" );
        verifier.addCliOption( "1" );
        verifier.executeGoal( "package" );
        verifier.resetStreams();
        verifier.verifyErrorFreeLog();
    }

    @Test
    public void testBasic_buildExtension()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-5578-session-scope" );
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
        verifier.addCliOption( "-Dit-build-extensions=true" );
        verifier.executeGoal( "package" );
        verifier.resetStreams();
        verifier.verifyErrorFreeLog();
    }

    @Test
    public void testExtension()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-5578-session-scope" );
        File extensionDir = new File( testDir, "extension" );
        File pluginDir = new File( testDir, "extension-plugin" );
        File projectDir = new File( testDir, "extension-project" );

        Verifier verifier;

        // install the test extension
        verifier = newVerifier( extensionDir.getAbsolutePath(), "remote" );
        verifier.executeGoal( "install" );
        verifier.resetStreams();
        verifier.verifyErrorFreeLog();

        // install the test plugin
        verifier = newVerifier( pluginDir.getAbsolutePath(), "remote" );
        verifier.executeGoal( "install" );
        verifier.resetStreams();
        verifier.verifyErrorFreeLog();

        // build the test project
        verifier = newVerifier( projectDir.getAbsolutePath(), "remote" );
        verifier.addCliOption( "-Dmaven.ext.class.path=" + new File( extensionDir, "target/classes" ).getAbsolutePath() );
        verifier.setForkJvm( true ); // verifier does not support custom realms in embedded mode
        verifier.executeGoal( "package" );
        verifier.resetStreams();
        verifier.verifyErrorFreeLog();

    }
}
