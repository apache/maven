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
import java.util.Properties;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4199">MNG-4199</a>.
 *
 * @author Benjamin Bentmann
 *
 */
public class MavenITmng4199CompileMeetsRuntimeScopeTest
    extends AbstractMavenIntegrationTestCase
{

    /*
     * NOTE: Class path ordering is another issue (MNG-1412), so we merely check set containment here.
     */

    public MavenITmng4199CompileMeetsRuntimeScopeTest()
    {
        super( ALL_MAVEN_VERSIONS );
    }

    /**
     * Test that the core properly handles goals with different requirements on dependency resolution. In particular
     * verify that the different dependency scopes are not erroneously collapsed/combined into just a single scope.
     * The problem is that scope "runtime" is not a superset of scope "compile".
     *
     * @throws Exception in case of failure
     */
    public void testit()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-4199" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.deleteArtifacts( "org.apache.maven.its.mng4199" );
        Properties filterProps = verifier.newDefaultFilterProperties();
        verifier.filterFile( "pom-template.xml", "pom.xml", "UTF-8", filterProps );
        verifier.filterFile( "settings-template.xml", "settings.xml", "UTF-8", filterProps );
        verifier.addCliOption( "--settings" );
        verifier.addCliOption( "settings.xml" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        List<String> compileArtifacts = verifier.loadLines( "target/compile-artifacts.txt", "UTF-8" );
        assertTrue( compileArtifacts.toString(), compileArtifacts.contains( "org.apache.maven.its.mng4199:system:jar:0.1" ) );
        assertTrue( compileArtifacts.toString(), compileArtifacts.contains( "org.apache.maven.its.mng4199:provided:jar:0.1" ) );
        assertTrue( compileArtifacts.toString(), compileArtifacts.contains( "org.apache.maven.its.mng4199:compile:jar:0.1" ) );
        assertFalse( compileArtifacts.toString(), compileArtifacts.contains( "org.apache.maven.its.mng4199:runtime:jar:0.1" ) );
        assertEquals( 3, compileArtifacts.size() );

        List<String> compileClassPath = verifier.loadLines( "target/compile-cp.txt", "UTF-8" );
        assertTrue( compileClassPath.toString(), compileClassPath.contains( "system-0.1.jar" ) );
        assertTrue( compileClassPath.toString(), compileClassPath.contains( "provided-0.1.jar" ) );
        assertTrue( compileClassPath.toString(), compileClassPath.contains( "compile-0.1.jar" ) );
        assertFalse( compileClassPath.toString(), compileClassPath.contains( "runtime-0.1.jar" ) );
        assertEquals( 4, compileClassPath.size() );

        List<String> runtimeArtifacts = verifier.loadLines( "target/runtime-artifacts.txt", "UTF-8" );
        assertFalse( runtimeArtifacts.toString(), runtimeArtifacts.contains( "org.apache.maven.its.mng4199:system:jar:0.1" ) );
        assertFalse( runtimeArtifacts.toString(), runtimeArtifacts.contains( "org.apache.maven.its.mng4199:provided:jar:0.1" ) );
        assertTrue( runtimeArtifacts.toString(), runtimeArtifacts.contains( "org.apache.maven.its.mng4199:compile:jar:0.1" ) );
        assertTrue( runtimeArtifacts.toString(), runtimeArtifacts.contains( "org.apache.maven.its.mng4199:runtime:jar:0.1" ) );
        assertEquals( 2, runtimeArtifacts.size() );

        List<String> runtimeClassPath = verifier.loadLines( "target/runtime-cp.txt", "UTF-8" );
        assertFalse( runtimeClassPath.toString(), runtimeClassPath.contains( "system-0.1.jar" ) );
        assertFalse( runtimeClassPath.toString(), runtimeClassPath.contains( "provided-0.1.jar" ) );
        assertTrue( runtimeClassPath.toString(), runtimeClassPath.contains( "compile-0.1.jar" ) );
        assertTrue( runtimeClassPath.toString(), runtimeClassPath.contains( "runtime-0.1.jar" ) );
        assertEquals( 3, runtimeClassPath.size() );
    }

}
