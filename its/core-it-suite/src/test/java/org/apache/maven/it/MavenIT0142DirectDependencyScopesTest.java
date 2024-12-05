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
package org.apache.maven.it;

import java.io.File;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 *
 * @author Benjamin Bentmann
 *
 */
public class MavenIT0142DirectDependencyScopesTest extends AbstractMavenIntegrationTestCase {

    /*
     * NOTE: Class path ordering is another issue (MNG-1412), so we merely check set containment here.
     */

    public MavenIT0142DirectDependencyScopesTest() {
        super(ALL_MAVEN_VERSIONS);
    }

    /**
     * Test that the different scopes of direct dependencies end up on the right class paths.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testit0142() throws Exception {
        File testDir = ResourceExtractor.simpleExtractResources(getClass(), "/it0142");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.deleteArtifacts("org.apache.maven.its.it0142");
        verifier.filterFile("pom-template.xml", "pom.xml");
        verifier.filterFile("settings-template.xml", "settings.xml");
        verifier.addCliArgument("--settings");
        verifier.addCliArgument("settings.xml");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        List<String> compileArtifacts = verifier.loadLines("target/compile-artifacts.txt");
        assertTrue(
                compileArtifacts.contains("org.apache.maven.its.it0142:system:jar:0.1"), compileArtifacts.toString());
        assertTrue(
                compileArtifacts.contains("org.apache.maven.its.it0142:provided:jar:0.1"), compileArtifacts.toString());
        assertTrue(
                compileArtifacts.contains("org.apache.maven.its.it0142:compile:jar:0.1"), compileArtifacts.toString());
        assertEquals(3, compileArtifacts.size());

        List<String> compileClassPath = verifier.loadLines("target/compile-cp.txt");
        assertTrue(compileClassPath.contains("classes"), compileClassPath.toString());
        assertTrue(compileClassPath.contains("system-0.1.jar"), compileClassPath.toString());
        assertTrue(compileClassPath.contains("provided-0.1.jar"), compileClassPath.toString());
        assertTrue(compileClassPath.contains("compile-0.1.jar"), compileClassPath.toString());
        assertEquals(4, compileClassPath.size());

        List<String> runtimeArtifacts = verifier.loadLines("target/runtime-artifacts.txt");
        assertTrue(
                runtimeArtifacts.contains("org.apache.maven.its.it0142:compile:jar:0.1"), runtimeArtifacts.toString());
        assertTrue(
                runtimeArtifacts.contains("org.apache.maven.its.it0142:runtime:jar:0.1"), runtimeArtifacts.toString());
        assertTrue(
                runtimeArtifacts.contains("org.apache.maven.its.it0142:runtime:jar:retro:0.1"),
                runtimeArtifacts.toString());
        assertEquals(3, runtimeArtifacts.size());

        List<String> runtimeClassPath = verifier.loadLines("target/runtime-cp.txt");
        assertTrue(runtimeClassPath.contains("classes"), runtimeClassPath.toString());
        assertTrue(runtimeClassPath.contains("compile-0.1.jar"), runtimeClassPath.toString());
        assertTrue(runtimeClassPath.contains("runtime-0.1.jar"), runtimeClassPath.toString());
        assertTrue(runtimeClassPath.contains("runtime-0.1-retro.jar"), runtimeClassPath.toString());
        assertEquals(4, runtimeClassPath.size());

        List<String> testArtifacts = verifier.loadLines("target/test-artifacts.txt");
        assertTrue(testArtifacts.contains("org.apache.maven.its.it0142:system:jar:0.1"), testArtifacts.toString());
        assertTrue(testArtifacts.contains("org.apache.maven.its.it0142:provided:jar:0.1"), testArtifacts.toString());
        assertTrue(testArtifacts.contains("org.apache.maven.its.it0142:compile:jar:0.1"), testArtifacts.toString());
        assertTrue(testArtifacts.contains("org.apache.maven.its.it0142:runtime:jar:0.1"), testArtifacts.toString());
        assertTrue(
                testArtifacts.contains("org.apache.maven.its.it0142:runtime:jar:retro:0.1"), testArtifacts.toString());
        assertTrue(testArtifacts.contains("org.apache.maven.its.it0142:test:jar:0.1"), testArtifacts.toString());
        assertEquals(6, testArtifacts.size());

        List<String> testClassPath = verifier.loadLines("target/test-cp.txt");
        assertTrue(testClassPath.contains("classes"), testClassPath.toString());
        assertTrue(testClassPath.contains("test-classes"), testClassPath.toString());
        assertTrue(testClassPath.contains("system-0.1.jar"), testClassPath.toString());
        assertTrue(testClassPath.contains("provided-0.1.jar"), testClassPath.toString());
        assertTrue(testClassPath.contains("compile-0.1.jar"), testClassPath.toString());
        assertTrue(testClassPath.contains("runtime-0.1.jar"), testClassPath.toString());
        assertTrue(testClassPath.contains("runtime-0.1-retro.jar"), testClassPath.toString());
        assertTrue(testClassPath.contains("test-0.1.jar"), testClassPath.toString());
        assertEquals(8, testClassPath.size());
    }
}
