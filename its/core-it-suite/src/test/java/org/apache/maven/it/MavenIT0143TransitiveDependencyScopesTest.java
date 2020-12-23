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
import java.util.List;
import java.util.Properties;

/**
 *
 * @author Benjamin Bentmann
 *
 */
public class MavenIT0143TransitiveDependencyScopesTest
    extends AbstractMavenIntegrationTestCase
{

    /*
     * NOTE: Class path ordering is another issue (MNG-1412), so we merely check set containment here.
     */

    public MavenIT0143TransitiveDependencyScopesTest()
    {
        super( ALL_MAVEN_VERSIONS );
    }

    /**
     * Test that the different scopes of transitive dependencies end up on the right class paths when mediated from
     * a compile-scope dependency.
     */
    public void testitCompileScope()
        throws Exception
    {
        Verifier verifier = run( "compile" );
        String targetDir = "target-compile";

        /*
         * NOTE: Transitive compile dependencies end up in compile scope to support the case of a class in the direct
         * dependency that extends a class from the transitive dependency, i.e.
         * project imports A from direct dependency and A extends B from transitive dependency.
         */
        List<String> compileArtifacts = verifier.loadLines( targetDir + "/compile-artifacts.txt", "UTF-8" );
        assertTrue( compileArtifacts.toString(), compileArtifacts.contains( "org.apache.maven.its.it0143:direct:jar:0.1" ) );
        assertTrue( compileArtifacts.toString(), compileArtifacts.contains( "org.apache.maven.its.it0143:compile:jar:0.1" ) );
        assertEquals( 2, compileArtifacts.size() );

        List<String> compileClassPath = verifier.loadLines( targetDir + "/compile-cp.txt", "UTF-8" );
        assertTrue( compileClassPath.toString(), compileClassPath.contains( "classes" ) );
        assertTrue( compileClassPath.toString(), compileClassPath.contains( "direct-0.1.jar" ) );
        assertTrue( compileClassPath.toString(), compileClassPath.contains( "compile-0.1.jar" ) );
        assertEquals( 3, compileClassPath.size() );

        List<String> runtimeArtifacts = verifier.loadLines( targetDir + "/runtime-artifacts.txt", "UTF-8" );
        assertTrue( runtimeArtifacts.toString(), runtimeArtifacts.contains( "org.apache.maven.its.it0143:direct:jar:0.1" ) );
        assertTrue( runtimeArtifacts.toString(), runtimeArtifacts.contains( "org.apache.maven.its.it0143:compile:jar:0.1" ) );
        assertTrue( runtimeArtifacts.toString(), runtimeArtifacts.contains( "org.apache.maven.its.it0143:runtime:jar:0.1" ) );
        assertEquals( 3, runtimeArtifacts.size() );

        List<String> runtimeClassPath = verifier.loadLines( targetDir + "/runtime-cp.txt", "UTF-8" );
        assertTrue( runtimeClassPath.toString(), runtimeClassPath.contains( "classes" ) );
        assertTrue( runtimeClassPath.toString(), runtimeClassPath.contains( "direct-0.1.jar" ) );
        assertTrue( runtimeClassPath.toString(), runtimeClassPath.contains( "compile-0.1.jar" ) );
        assertTrue( runtimeClassPath.toString(), runtimeClassPath.contains( "runtime-0.1.jar" ) );
        assertEquals( 4, runtimeClassPath.size() );

        List<String> testArtifacts = verifier.loadLines( targetDir + "/test-artifacts.txt", "UTF-8" );
        assertTrue( testArtifacts.toString(), testArtifacts.contains( "org.apache.maven.its.it0143:direct:jar:0.1" ) );
        assertTrue( testArtifacts.toString(), testArtifacts.contains( "org.apache.maven.its.it0143:compile:jar:0.1" ) );
        assertTrue( testArtifacts.toString(), testArtifacts.contains( "org.apache.maven.its.it0143:runtime:jar:0.1" ) );
        assertEquals( 3, testArtifacts.size() );

        List<String> testClassPath = verifier.loadLines( targetDir + "/test-cp.txt", "UTF-8" );
        assertTrue( testClassPath.toString(), testClassPath.contains( "classes" ) );
        assertTrue( testClassPath.toString(), testClassPath.contains( "test-classes" ) );
        assertTrue( testClassPath.toString(), testClassPath.contains( "direct-0.1.jar" ) );
        assertTrue( testClassPath.toString(), testClassPath.contains( "compile-0.1.jar" ) );
        assertTrue( testClassPath.toString(), testClassPath.contains( "runtime-0.1.jar" ) );
        assertEquals( 5, testClassPath.size() );
    }

    /**
     * Test that the different scopes of transitive dependencies end up on the right class paths when mediated from
     * a provided-scope dependency.
     */
    public void testitProvidedScope()
        throws Exception
    {
        Verifier verifier = run( "provided" );
        String targetDir = "target-provided";

        List<String> compileArtifacts = verifier.loadLines( targetDir + "/compile-artifacts.txt", "UTF-8" );
        assertTrue( compileArtifacts.toString(), compileArtifacts.contains( "org.apache.maven.its.it0143:direct:jar:0.1" ) );
        assertTrue( compileArtifacts.toString(), compileArtifacts.contains( "org.apache.maven.its.it0143:compile:jar:0.1" ) );
        assertTrue( compileArtifacts.toString(), compileArtifacts.contains( "org.apache.maven.its.it0143:runtime:jar:0.1" ) );
        assertEquals( 3, compileArtifacts.size() );

        List<String> compileClassPath = verifier.loadLines( targetDir + "/compile-cp.txt", "UTF-8" );
        assertTrue( compileClassPath.toString(), compileClassPath.contains( "classes" ) );
        assertTrue( compileClassPath.toString(), compileClassPath.contains( "direct-0.1.jar" ) );
        assertTrue( compileClassPath.toString(), compileClassPath.contains( "compile-0.1.jar" ) );
        assertTrue( compileClassPath.toString(), compileClassPath.contains( "runtime-0.1.jar" ) );
        assertEquals( 4, compileClassPath.size() );

        List<String> runtimeArtifacts = verifier.loadLines( targetDir + "/runtime-artifacts.txt", "UTF-8" );
        assertEquals( 0, runtimeArtifacts.size() );

        List<String> runtimeClassPath = verifier.loadLines( targetDir + "/runtime-cp.txt", "UTF-8" );
        assertTrue( runtimeClassPath.toString(), runtimeClassPath.contains( "classes" ) );
        assertEquals( 1, runtimeClassPath.size() );

        List<String> testArtifacts = verifier.loadLines( targetDir + "/test-artifacts.txt", "UTF-8" );
        assertTrue( testArtifacts.toString(), testArtifacts.contains( "org.apache.maven.its.it0143:direct:jar:0.1" ) );
        assertTrue( testArtifacts.toString(), testArtifacts.contains( "org.apache.maven.its.it0143:compile:jar:0.1" ) );
        assertTrue( testArtifacts.toString(), testArtifacts.contains( "org.apache.maven.its.it0143:runtime:jar:0.1" ) );
        assertEquals( 3, testArtifacts.size() );

        List<String> testClassPath = verifier.loadLines( targetDir + "/test-cp.txt", "UTF-8" );
        assertTrue( testClassPath.toString(), testClassPath.contains( "classes" ) );
        assertTrue( testClassPath.toString(), testClassPath.contains( "test-classes" ) );
        assertTrue( testClassPath.toString(), testClassPath.contains( "direct-0.1.jar" ) );
        assertTrue( testClassPath.toString(), testClassPath.contains( "compile-0.1.jar" ) );
        assertTrue( testClassPath.toString(), testClassPath.contains( "runtime-0.1.jar" ) );
        assertEquals( 5, testClassPath.size() );
    }

    /**
     * Test that the different scopes of transitive dependencies end up on the right class paths when mediated from
     * a runtime-scope dependency.
     */
    public void testitRuntimeScope()
        throws Exception
    {
        Verifier verifier = run( "runtime" );
        String targetDir = "target-runtime";

        List<String> compileArtifacts = verifier.loadLines( targetDir + "/compile-artifacts.txt", "UTF-8" );
        assertEquals( 0, compileArtifacts.size() );

        List<String> compileClassPath = verifier.loadLines( targetDir + "/compile-cp.txt", "UTF-8" );
        assertTrue( compileClassPath.toString(), compileClassPath.contains( "classes" ) );
        assertEquals( 1, compileClassPath.size() );

        List<String> runtimeArtifacts = verifier.loadLines( targetDir + "/runtime-artifacts.txt", "UTF-8" );
        assertTrue( runtimeArtifacts.toString(), runtimeArtifacts.contains( "org.apache.maven.its.it0143:direct:jar:0.1" ) );
        assertTrue( runtimeArtifacts.toString(), runtimeArtifacts.contains( "org.apache.maven.its.it0143:compile:jar:0.1" ) );
        assertTrue( runtimeArtifacts.toString(), runtimeArtifacts.contains( "org.apache.maven.its.it0143:runtime:jar:0.1" ) );
        assertEquals( 3, runtimeArtifacts.size() );

        List<String> runtimeClassPath = verifier.loadLines( targetDir + "/runtime-cp.txt", "UTF-8" );
        assertTrue( runtimeClassPath.toString(), runtimeClassPath.contains( "classes" ) );
        assertTrue( runtimeClassPath.toString(), runtimeClassPath.contains( "direct-0.1.jar" ) );
        assertTrue( runtimeClassPath.toString(), runtimeClassPath.contains( "compile-0.1.jar" ) );
        assertTrue( runtimeClassPath.toString(), runtimeClassPath.contains( "runtime-0.1.jar" ) );
        assertEquals( 4, runtimeClassPath.size() );

        List<String> testArtifacts = verifier.loadLines( targetDir + "/test-artifacts.txt", "UTF-8" );
        assertTrue( testArtifacts.toString(), testArtifacts.contains( "org.apache.maven.its.it0143:direct:jar:0.1" ) );
        assertTrue( testArtifacts.toString(), testArtifacts.contains( "org.apache.maven.its.it0143:compile:jar:0.1" ) );
        assertTrue( testArtifacts.toString(), testArtifacts.contains( "org.apache.maven.its.it0143:runtime:jar:0.1" ) );
        assertEquals( 3, testArtifacts.size() );

        List<String> testClassPath = verifier.loadLines( targetDir + "/test-cp.txt", "UTF-8" );
        assertTrue( testClassPath.toString(), testClassPath.contains( "classes" ) );
        assertTrue( testClassPath.toString(), testClassPath.contains( "test-classes" ) );
        assertTrue( testClassPath.toString(), testClassPath.contains( "direct-0.1.jar" ) );
        assertTrue( testClassPath.toString(), testClassPath.contains( "compile-0.1.jar" ) );
        assertTrue( testClassPath.toString(), testClassPath.contains( "runtime-0.1.jar" ) );
        assertEquals( 5, testClassPath.size() );
    }

    /**
     * Test that the different scopes of transitive dependencies end up on the right class paths when mediated from
     * a test-scope dependency.
     */
    public void testitTestScope()
        throws Exception
    {
        Verifier verifier = run( "test" );
        String targetDir = "target-test";

        List<String> compileArtifacts = verifier.loadLines( targetDir + "/compile-artifacts.txt", "UTF-8" );
        assertEquals( 0, compileArtifacts.size() );

        List<String> compileClassPath = verifier.loadLines( targetDir + "/compile-cp.txt", "UTF-8" );
        assertTrue( compileClassPath.toString(), compileClassPath.contains( "classes" ) );
        assertEquals( 1, compileClassPath.size() );

        List<String> runtimeArtifacts = verifier.loadLines( targetDir + "/runtime-artifacts.txt", "UTF-8" );
        assertEquals( 0, runtimeArtifacts.size() );

        List<String> runtimeClassPath = verifier.loadLines( targetDir + "/runtime-cp.txt", "UTF-8" );
        assertTrue( runtimeClassPath.toString(), runtimeClassPath.contains( "classes" ) );
        assertEquals( 1, runtimeClassPath.size() );

        List<String> testArtifacts = verifier.loadLines( targetDir + "/test-artifacts.txt", "UTF-8" );
        assertTrue( testArtifacts.toString(), testArtifacts.contains( "org.apache.maven.its.it0143:direct:jar:0.1" ) );
        assertTrue( testArtifacts.toString(), testArtifacts.contains( "org.apache.maven.its.it0143:compile:jar:0.1" ) );
        assertTrue( testArtifacts.toString(), testArtifacts.contains( "org.apache.maven.its.it0143:runtime:jar:0.1" ) );
        assertEquals( 3, testArtifacts.size() );

        List<String> testClassPath = verifier.loadLines( targetDir + "/test-cp.txt", "UTF-8" );
        assertTrue( testClassPath.toString(), testClassPath.contains( "classes" ) );
        assertTrue( testClassPath.toString(), testClassPath.contains( "test-classes" ) );
        assertTrue( testClassPath.toString(), testClassPath.contains( "direct-0.1.jar" ) );
        assertTrue( testClassPath.toString(), testClassPath.contains( "compile-0.1.jar" ) );
        assertTrue( testClassPath.toString(), testClassPath.contains( "runtime-0.1.jar" ) );
        assertEquals( 5, testClassPath.size() );
    }

    private Verifier run( String scope )
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/it0143" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target-" + scope );
        verifier.deleteArtifacts( "org.apache.maven.its.it0143" );
        Properties filterProps = verifier.newDefaultFilterProperties();
        filterProps.setProperty( "@scope@", scope );
        verifier.filterFile( "pom-template.xml", "pom.xml", "UTF-8", filterProps );
        verifier.filterFile( "settings-template.xml", "settings.xml", "UTF-8", filterProps );
        verifier.addCliOption( "--settings" );
        verifier.addCliOption( "settings.xml" );
        verifier.setLogFileName( "log-" + scope + ".txt" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        return verifier;
    }

}
