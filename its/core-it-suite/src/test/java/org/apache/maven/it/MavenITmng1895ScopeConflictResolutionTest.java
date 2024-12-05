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
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-1895">MNG-1895</a>.
 *
 * @author Benjamin Bentmann
 */
public class MavenITmng1895ScopeConflictResolutionTest extends AbstractMavenIntegrationTestCase {

    public MavenITmng1895ScopeConflictResolutionTest() {
        super("[2.0.3,)");
    }

    /**
     * Verify that for a dependency being referenced in two different scopes, the scope given directly in the POM
     * always wins, even if weaker than the scope of the transitive dependency.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitDirectVsIndirect() throws Exception {
        File testDir = extractResources("/mng-1895/direct-vs-indirect");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteArtifacts("org.apache.maven.its.mng1895");
        verifier.deleteDirectory("target");
        verifier.addCliArgument("-s");
        verifier.addCliArgument("settings.xml");
        verifier.filterFile("settings-template.xml", "settings.xml");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        List<String> compile = verifier.loadLines("target/compile.txt");
        assertTrue(compile.contains("a-0.1.jar"), compile.toString());
        assertFalse(compile.contains("b-0.1.jar"), compile.toString());
        assertFalse(compile.contains("c-0.1.jar"), compile.toString());
        assertTrue(compile.contains("d-0.1.jar"), compile.toString());

        List<String> runtime = verifier.loadLines("target/runtime.txt");
        assertFalse(runtime.contains("a-0.1.jar"), runtime.toString());
        assertTrue(runtime.contains("b-0.1.jar"), runtime.toString());
        assertFalse(runtime.contains("c-0.1.jar"), runtime.toString());
        assertTrue(runtime.contains("d-0.1.jar"), runtime.toString());

        List<String> test = verifier.loadLines("target/test.txt");
        assertTrue(test.contains("a-0.1.jar"), test.toString());
        assertTrue(test.contains("b-0.1.jar"), test.toString());
        assertTrue(test.contains("c-0.1.jar"), test.toString());
        assertTrue(test.contains("d-0.1.jar"), test.toString());
    }

    /**
     * Verify that for a dependency being referenced in compile and in runtime scope, compile scope wins.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitCompileVsRuntime() throws Exception {
        Verifier verifier = run("compile", "runtime");

        List<String> compile = verifier.loadLines("target/compile.txt");
        assertTrue(compile.contains("x-0.1.jar"), compile.toString());
        assertFalse(compile.contains("a-0.1.jar"), compile.toString());

        List<String> runtime = verifier.loadLines("target/runtime.txt");
        assertTrue(runtime.contains("x-0.1.jar"), runtime.toString());
        assertTrue(runtime.contains("a-0.1.jar"), runtime.toString());

        List<String> test = verifier.loadLines("target/test.txt");
        assertTrue(test.contains("x-0.1.jar"), test.toString());
        assertTrue(test.contains("a-0.1.jar"), test.toString());
    }

    /**
     * Verify that for a dependency being referenced in compile and in test scope, compile scope wins.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitCompileVsTest() throws Exception {
        Verifier verifier = run("compile", "test");

        List<String> compile = verifier.loadLines("target/compile.txt");
        assertTrue(compile.contains("x-0.1.jar"), compile.toString());
        assertFalse(compile.contains("a-0.1.jar"), compile.toString());

        List<String> runtime = verifier.loadLines("target/runtime.txt");
        assertTrue(runtime.contains("x-0.1.jar"), runtime.toString());
        assertFalse(runtime.contains("a-0.1.jar"), runtime.toString());

        List<String> test = verifier.loadLines("target/test.txt");
        assertTrue(test.contains("x-0.1.jar"), test.toString());
        assertTrue(test.contains("a-0.1.jar"), test.toString());
    }

    /**
     * Verify that for a dependency being referenced in compile and in provided scope, compile scope wins.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitCompileVsProvided() throws Exception {
        Verifier verifier = run("compile", "provided");

        List<String> compile = verifier.loadLines("target/compile.txt");
        assertTrue(compile.contains("x-0.1.jar"), compile.toString());
        assertTrue(compile.contains("a-0.1.jar"), compile.toString());

        List<String> runtime = verifier.loadLines("target/runtime.txt");
        assertTrue(runtime.contains("x-0.1.jar"), runtime.toString());
        assertFalse(runtime.contains("a-0.1.jar"), runtime.toString());

        List<String> test = verifier.loadLines("target/test.txt");
        assertTrue(test.contains("x-0.1.jar"), test.toString());
        assertTrue(test.contains("a-0.1.jar"), test.toString());
    }

    /**
     * Verify that for a dependency being referenced in runtime and in test scope, runtime scope wins.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitRuntimeVsTest() throws Exception {
        Verifier verifier = run("runtime", "test");

        List<String> compile = verifier.loadLines("target/compile.txt");
        assertFalse(compile.contains("x-0.1.jar"), compile.toString());
        assertFalse(compile.contains("a-0.1.jar"), compile.toString());

        List<String> runtime = verifier.loadLines("target/runtime.txt");
        assertTrue(runtime.contains("x-0.1.jar"), runtime.toString());
        assertFalse(runtime.contains("a-0.1.jar"), runtime.toString());

        List<String> test = verifier.loadLines("target/test.txt");
        assertTrue(test.contains("x-0.1.jar"), test.toString());
        assertTrue(test.contains("a-0.1.jar"), test.toString());
    }

    /**
     * Verify that for a dependency being referenced in runtime and in provided scope, runtime scope wins.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitRuntimeVsProvided() throws Exception {
        Verifier verifier = run("runtime", "provided");

        List<String> compile = verifier.loadLines("target/compile.txt");
        assertFalse(compile.contains("x-0.1.jar"), compile.toString());
        assertTrue(compile.contains("a-0.1.jar"), compile.toString());

        List<String> runtime = verifier.loadLines("target/runtime.txt");
        assertTrue(runtime.contains("x-0.1.jar"), runtime.toString());
        assertFalse(runtime.contains("a-0.1.jar"), runtime.toString());

        List<String> test = verifier.loadLines("target/test.txt");
        assertTrue(test.contains("x-0.1.jar"), test.toString());
        assertTrue(test.contains("a-0.1.jar"), test.toString());
    }

    /**
     * Verify that for a dependency being referenced in provided and in test scope, provided scope wins.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitProvidedVsTest() throws Exception {
        requiresMavenVersion("[3.0-beta-3,)"); // MNG-2686

        Verifier verifier = run("provided", "test");

        List<String> compile = verifier.loadLines("target/compile.txt");
        assertTrue(compile.contains("x-0.1.jar"), compile.toString());
        assertFalse(compile.contains("a-0.1.jar"), compile.toString());

        List<String> runtime = verifier.loadLines("target/runtime.txt");
        assertFalse(runtime.contains("x-0.1.jar"), runtime.toString());
        assertFalse(runtime.contains("a-0.1.jar"), runtime.toString());

        List<String> test = verifier.loadLines("target/test.txt");
        assertTrue(test.contains("x-0.1.jar"), test.toString());
        assertTrue(test.contains("a-0.1.jar"), test.toString());
    }

    private Verifier run(String scopeB, String scopeA) throws Exception {
        File testDir = extractResources("/mng-1895/strong-vs-weak");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteArtifacts("org.apache.maven.its.mng1895");
        verifier.deleteDirectory("target");
        verifier.addCliArgument("-s");
        verifier.addCliArgument("settings.xml");
        Map<String, String> props = verifier.newDefaultFilterMap();
        props.put("@scope.a@", scopeA);
        props.put("@scope.b@", scopeB);
        verifier.filterFile("settings-template.xml", "settings.xml", props);
        verifier.filterFile("pom-template.xml", "pom.xml", props);
        verifier.setLogFileName("log-" + scopeB + "-vs-" + scopeA + ".txt");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        return verifier;
    }
}
