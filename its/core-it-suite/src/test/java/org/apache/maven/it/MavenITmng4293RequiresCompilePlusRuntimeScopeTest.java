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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4293">MNG-4293</a>.
 *
 * @author Benjamin Bentmann
 */
public class MavenITmng4293RequiresCompilePlusRuntimeScopeTest extends AbstractMavenIntegrationTestCase {

    /*
     * NOTE: Class path ordering is another issue (MNG-1412), so we merely check set containment here.
     */

    public MavenITmng4293RequiresCompilePlusRuntimeScopeTest() {
        super("[3.0-alpha-3,)");
    }

    /**
     * Test support of "@requiresDependencyResolution compile+runtime".
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testit() throws Exception {
        File testDir = extractResources("/mng-4293");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.deleteArtifacts("org.apache.maven.its.mng4293");
        verifier.filterFile("pom-template.xml", "pom.xml");
        verifier.filterFile("settings-template.xml", "settings.xml");
        verifier.addCliArgument("--settings");
        verifier.addCliArgument("settings.xml");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        List<String> compileClassPath = verifier.loadLines("target/compile-cp.txt");
        assertTrue(compileClassPath.contains("system-0.1.jar"), compileClassPath.toString());
        assertTrue(compileClassPath.contains("provided-0.1.jar"), compileClassPath.toString());
        assertTrue(compileClassPath.contains("compile-0.1.jar"), compileClassPath.toString());
        assertFalse(compileClassPath.contains("test-0.1.jar"), compileClassPath.toString());

        List<String> runtimeClassPath = verifier.loadLines("target/runtime-cp.txt");
        assertTrue(runtimeClassPath.contains("compile-0.1.jar"), runtimeClassPath.toString());
        assertTrue(runtimeClassPath.contains("runtime-0.1.jar"), runtimeClassPath.toString());
        assertFalse(runtimeClassPath.contains("test-0.1.jar"), runtimeClassPath.toString());
    }
}
