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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-2720">MNG-2720</a>.
 *
 * This test will ensure that running the 'package' phase on a multi-module build with child
 * interdependency will result in one child using the JAR of the other child in its compile
 * classpath, NOT the target/classes directory. This is critical, since sibling projects might
 * use literally ANY artifact produced by another module project, and limiting to target/classes
 * and target/test-classes eliminates many of the options that would be possible if the dependent
 * sibling were built on its own.
 *
 * @author jdcasey
 *
 */
public class MavenITmng2720SiblingClasspathArtifactsTest extends AbstractMavenIntegrationTestCase {

    public MavenITmng2720SiblingClasspathArtifactsTest() {
        super("[2.1.0,)");
    }

    @Test
    public void testIT() throws Exception {
        File testDir = extractResources("/mng-2720");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.deleteDirectory("child2/target");
        verifier.deleteDirectory("child3/target");
        verifier.addCliArgument("initialize");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        List<String> classPath;

        classPath = verifier.loadLines("child2/target/compile.txt");
        assertMainJar(classPath);

        classPath = verifier.loadLines("child2/target/runtime.txt");
        assertMainJar(classPath);

        classPath = verifier.loadLines("child2/target/test.txt");
        assertMainJar(classPath);

        classPath = verifier.loadLines("child3/target/compile.txt");
        assertTestJar(classPath);

        classPath = verifier.loadLines("child3/target/runtime.txt");
        assertTestJar(classPath);

        classPath = verifier.loadLines("child3/target/test.txt");
        assertTestJar(classPath);
    }

    private void assertMainJar(List<String> classPath) {
        assertTrue(classPath.contains("main.jar"), classPath.toString());
        assertFalse(classPath.contains("main"), classPath.toString());
        assertFalse(classPath.contains("test.jar"), classPath.toString());
        assertFalse(classPath.contains("test"), classPath.toString());
    }

    private void assertTestJar(List<String> classPath) {
        assertFalse(classPath.contains("main.jar"), classPath.toString());
        assertFalse(classPath.contains("main"), classPath.toString());
        assertTrue(classPath.contains("test.jar"), classPath.toString());
        assertFalse(classPath.contains("test"), classPath.toString());
    }
}
