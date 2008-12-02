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

import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * 
 * @author Benjamin Bentmann
 * @version $Id$
 */
public class MavenIT0142DirectDependencyScopesTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenIT0142DirectDependencyScopesTest()
    {
    }

    /**
     * Test that the different scopes of direct dependencies end up on the right class paths.
     */
    public void testit0142()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/it0142" );

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.deleteArtifacts( "org.apache.maven.its.it0142" );
        verifier.filterFile( "pom.xml", "pom.xml", "UTF-8", verifier.newDefaultFilterProperties() );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        /*
         * NOTE: Class path ordering is another issue (MNG-1412), so we merely check set containment here.
         */

        List compileArtifacts = verifier.loadLines( "target/compile-artifacts.txt", "UTF-8" );
        assertEquals( 3, compileArtifacts.size() );
        assertTrue( compileArtifacts.toString(), compileArtifacts.contains( "org.apache.maven.its.it0142:system:jar:0.1" ) );
        assertTrue( compileArtifacts.toString(), compileArtifacts.contains( "org.apache.maven.its.it0142:provided:jar:0.1" ) );
        assertTrue( compileArtifacts.toString(), compileArtifacts.contains( "org.apache.maven.its.it0142:compile:jar:0.1" ) );

        List compileClassPath = verifier.loadLines( "target/compile-cp.txt", "UTF-8" );
        assertEquals( 4, compileClassPath.size() );
        assertTrue( compileClassPath.toString(), compileClassPath.contains( "classes" ) );
        assertTrue( compileClassPath.toString(), compileClassPath.contains( "system-0.1.jar" ) );
        assertTrue( compileClassPath.toString(), compileClassPath.contains( "provided-0.1.jar" ) );
        assertTrue( compileClassPath.toString(), compileClassPath.contains( "compile-0.1.jar" ) );

        List runtimeArtifacts = verifier.loadLines( "target/runtime-artifacts.txt", "UTF-8" );
        assertEquals( 2, runtimeArtifacts.size() );
        assertTrue( runtimeArtifacts.toString(), runtimeArtifacts.contains( "org.apache.maven.its.it0142:compile:jar:0.1" ) );
        assertTrue( runtimeArtifacts.toString(), runtimeArtifacts.contains( "org.apache.maven.its.it0142:runtime:jar:0.1" ) );

        List runtimeClassPath = verifier.loadLines( "target/runtime-cp.txt", "UTF-8" );
        assertEquals( 3, runtimeClassPath.size() );
        assertTrue( runtimeClassPath.toString(), runtimeClassPath.contains( "classes" ) );
        assertTrue( runtimeClassPath.toString(), runtimeClassPath.contains( "compile-0.1.jar" ) );
        assertTrue( runtimeClassPath.toString(), runtimeClassPath.contains( "runtime-0.1.jar" ) );

        List testArtifacts = verifier.loadLines( "target/test-artifacts.txt", "UTF-8" );
        assertEquals( 5, testArtifacts.size() );
        assertTrue( testArtifacts.toString(), testArtifacts.contains( "org.apache.maven.its.it0142:system:jar:0.1" ) );
        assertTrue( testArtifacts.toString(), testArtifacts.contains( "org.apache.maven.its.it0142:provided:jar:0.1" ) );
        assertTrue( testArtifacts.toString(), testArtifacts.contains( "org.apache.maven.its.it0142:compile:jar:0.1" ) );
        assertTrue( testArtifacts.toString(), testArtifacts.contains( "org.apache.maven.its.it0142:runtime:jar:0.1" ) );
        assertTrue( testArtifacts.toString(), testArtifacts.contains( "org.apache.maven.its.it0142:test:jar:0.1" ) );

        List testClassPath = verifier.loadLines( "target/test-cp.txt", "UTF-8" );
        assertEquals( 7, testClassPath.size() );
        assertTrue( testClassPath.toString(), testClassPath.contains( "classes" ) );
        assertTrue( testClassPath.toString(), testClassPath.contains( "test-classes" ) );
        assertTrue( testClassPath.toString(), testClassPath.contains( "system-0.1.jar" ) );
        assertTrue( testClassPath.toString(), testClassPath.contains( "provided-0.1.jar" ) );
        assertTrue( testClassPath.toString(), testClassPath.contains( "compile-0.1.jar" ) );
        assertTrue( testClassPath.toString(), testClassPath.contains( "runtime-0.1.jar" ) );
        assertTrue( testClassPath.toString(), testClassPath.contains( "test-0.1.jar" ) );
    }

}
