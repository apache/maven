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

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4789">MNG-4789</a>.
 *
 * @author Benjamin Bentmann
 */
public class MavenITmng4789ScopeInheritanceMeetsConflictTest extends AbstractMavenIntegrationTestCase {

    /**
     * Test that scope inheritance considers the effective scope of parent nodes as enforced by direct dependency
     * declarations. The exception is the test scope, which only applies to the test class path
     * (<a href="https://issues.apache.org/jira/browse/MNG-6058">MNG-6058</a>): a test dependency does not remove a
     * transitive dependency of the application, nor its dependencies, from the other class paths.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testit() throws Exception {
        Path testDir = extractResources("mng-4789");

        Verifier verifier = newVerifier(testDir);
        verifier.setAutoclean(false);
        verifier.deleteArtifacts("org.apache.maven.its.mng4789");
        verifier.deleteDirectory("target");
        verifier.addCliArgument("-s");
        verifier.addCliArgument("settings.xml");
        verifier.filterFile("settings-template.xml", "settings.xml");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        List<String> compile = verifier.loadLines("target/compile.txt");
        assertTrue(compile.contains("a-0.1.jar"), compile.toString());
        assertTrue(compile.contains("b-0.1.jar"), compile.toString());
        assertTrue(compile.contains("x-0.1.jar"), compile.toString());

        List<String> runtime = verifier.loadLines("target/runtime.txt");
        assertTrue(runtime.contains("a-0.1.jar"), runtime.toString());
        assertTrue(runtime.contains("b-0.1.jar"), runtime.toString());
        assertTrue(runtime.contains("x-0.1.jar"), runtime.toString());

        List<String> test = verifier.loadLines("target/test.txt");
        assertTrue(test.contains("a-0.1.jar"), test.toString());
        assertTrue(test.contains("b-0.1.jar"), test.toString());
        assertTrue(test.contains("x-0.1.jar"), test.toString());
    }
}
