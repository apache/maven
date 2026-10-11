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

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-6058">MNG-6058</a>.
 */
public class MavenITmng6058TestScopeMediationTest extends AbstractMavenIntegrationTestCase {

    /**
     * Verify that a test dependency overrides a transitive dependency of the application only while testing.
     * The project depends on {@code a:2.0}, which depends on {@code b:2.0}, and declares {@code b:1.0} with test
     * scope. The compile and the runtime classpath must keep {@code b:2.0}, while the test classpath uses
     * {@code b:1.0}. The three classpaths are requested by mojos of a single build, one of them running after the
     * test one, as every mojo must get the dependency graph of the scopes it asked for.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitTestDependencyDoesNotOverrideApplicationDependencies() throws Exception {
        Path testDir = extractResources("mng-6058");

        Verifier verifier = newVerifier(testDir);
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.deleteArtifacts("org.apache.maven.its.mng6058");
        verifier.addCliArgument("-s");
        verifier.addCliArgument("settings.xml");
        verifier.filterFile("settings-template.xml", "settings.xml");
        verifier.addCliArgument("process-classes");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        List<String> compileClassPath = verifier.loadLines("target/compile.txt");
        assertTrue(compileClassPath.contains("a-2.0.jar"), compileClassPath.toString());
        assertTrue(compileClassPath.contains("b-2.0.jar"), compileClassPath.toString());
        assertFalse(compileClassPath.contains("b-1.0.jar"), compileClassPath.toString());

        List<String> runtimeClassPath = verifier.loadLines("target/runtime.txt");
        assertTrue(runtimeClassPath.contains("a-2.0.jar"), runtimeClassPath.toString());
        assertTrue(runtimeClassPath.contains("b-2.0.jar"), runtimeClassPath.toString());
        assertFalse(runtimeClassPath.contains("b-1.0.jar"), runtimeClassPath.toString());

        List<String> testClassPath = verifier.loadLines("target/test.txt");
        assertTrue(testClassPath.contains("a-2.0.jar"), testClassPath.toString());
        assertTrue(testClassPath.contains("b-1.0.jar"), testClassPath.toString());
        assertFalse(testClassPath.contains("b-2.0.jar"), testClassPath.toString());
    }
}
