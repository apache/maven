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
import java.nio.file.Files;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-3043">MNG-3043</a>.
 *
 * @author Benjamin Bentmann
 */
public class MavenITmng3043BestEffortReactorResolutionTest extends AbstractMavenIntegrationTestCase {

    public MavenITmng3043BestEffortReactorResolutionTest() {
        super("[3.0-alpha-3,)");
    }

    /**
     * Test that dependencies on attached artifacts like a test JAR or an EJB client JAR which have not been built
     * yet, i.e. in build phases prior to "package" like "test", are satisfied from the output directories of the
     * projects in the reactor. This is meant as a best effort to provide a class path for compilation or testing.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitTestPhase() throws Exception {
        File testDir = extractResources("/mng-3043");
        Files.createDirectories(testDir.toPath().resolve(".mvn"));

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.deleteDirectory("consumer-a/target");
        verifier.deleteDirectory("consumer-b/target");
        verifier.deleteDirectory("consumer-c/target");
        verifier.deleteArtifacts("org.apache.maven.its.mng3043", null);
        verifier.setLogFileName("log-test.txt");
        verifier.addCliArgument("test");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        List<String> classpath;

        classpath = verifier.loadLines("consumer-a/target/compile.txt");
        assertContains(classpath, new String[] {"classes-test"});
        assertNotContains(classpath, new String[] {"classes-main"});
        classpath = verifier.loadLines("consumer-a/target/runtime.txt");
        assertContains(classpath, new String[] {"classes-test"});
        assertNotContains(classpath, new String[] {"classes-main"});
        classpath = verifier.loadLines("consumer-a/target/test.txt");
        assertContains(classpath, new String[] {"classes-test"});
        assertNotContains(classpath, new String[] {"classes-main"});

        classpath = verifier.loadLines("consumer-b/target/compile.txt");
        assertContains(classpath, new String[] {"classes-main"});
        assertNotContains(classpath, new String[] {"classes-test"});
        classpath = verifier.loadLines("consumer-b/target/runtime.txt");
        assertContains(classpath, new String[] {"classes-main"});
        assertNotContains(classpath, new String[] {"classes-test"});
        classpath = verifier.loadLines("consumer-b/target/test.txt");
        assertContains(classpath, new String[] {"classes-main"});
        assertNotContains(classpath, new String[] {"classes-test"});

        classpath = verifier.loadLines("consumer-c/target/compile.txt");
        assertContains(classpath, new String[] {"classes-main"});
        assertContains(classpath, new String[] {"classes-test"});
        classpath = verifier.loadLines("consumer-c/target/runtime.txt");
        assertContains(classpath, new String[] {"classes-main"});
        assertContains(classpath, new String[] {"classes-test"});
        classpath = verifier.loadLines("consumer-c/target/test.txt");
        assertContains(classpath, new String[] {"classes-main"});
        assertContains(classpath, new String[] {"classes-test"});
    }

    /**
     * Test that dependency resolution still uses the actual artifact files once these have been
     * assembled/attached in the "package" phase. This ensures the class path is accurate and not locked to
     * the output directories of the best effort model from above.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitPackagePhase() throws Exception {
        File testDir = extractResources("/mng-3043");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.deleteDirectory("consumer-a/target");
        verifier.deleteDirectory("consumer-b/target");
        verifier.deleteDirectory("consumer-c/target");
        verifier.deleteArtifacts("org.apache.maven.its.mng3043", null);
        verifier.setLogFileName("log-package.txt");
        verifier.addCliArgument("package");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        String prefix = "";

        List<String> classpath;

        classpath = verifier.loadLines("consumer-a/target/compile.txt");
        assertContains(classpath, new String[] {prefix + "tests.jar"});
        assertNotContains(classpath, new String[] {prefix + "client.jar"});
        classpath = verifier.loadLines("consumer-a/target/runtime.txt");
        assertContains(classpath, new String[] {prefix + "tests.jar"});
        assertNotContains(classpath, new String[] {prefix + "client.jar"});
        classpath = verifier.loadLines("consumer-a/target/test.txt");
        assertContains(classpath, new String[] {prefix + "tests.jar"});
        assertNotContains(classpath, new String[] {prefix + "client.jar"});

        classpath = verifier.loadLines("consumer-b/target/compile.txt");
        assertContains(classpath, new String[] {prefix + "client.jar"});
        assertNotContains(classpath, new String[] {prefix + "tests.jar"});
        classpath = verifier.loadLines("consumer-b/target/runtime.txt");
        assertContains(classpath, new String[] {prefix + "client.jar"});
        assertNotContains(classpath, new String[] {prefix + "tests.jar"});
        classpath = verifier.loadLines("consumer-b/target/test.txt");
        assertContains(classpath, new String[] {prefix + "client.jar"});
        assertNotContains(classpath, new String[] {prefix + "tests.jar"});

        classpath = verifier.loadLines("consumer-c/target/compile.txt");
        assertContains(classpath, new String[] {prefix + "client.jar"});
        assertContains(classpath, new String[] {prefix + "tests.jar"});
        classpath = verifier.loadLines("consumer-c/target/runtime.txt");
        assertContains(classpath, new String[] {prefix + "client.jar"});
        assertContains(classpath, new String[] {prefix + "tests.jar"});
        classpath = verifier.loadLines("consumer-c/target/test.txt");
        assertContains(classpath, new String[] {prefix + "client.jar"});
        assertContains(classpath, new String[] {prefix + "tests.jar"});
    }

    /**
     * Test that dependency resolution still uses the actual artifact files once these have been
     * assembled/attached in the "package" phase. This ensures the class path is accurate and not locked to
     * the output directories of the best effort model from above.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitPackagePhasesSlitted() throws Exception {
        requiresMavenVersion("[4.0.0-beta-4,)");

        File testDir = extractResources("/mng-3043");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.deleteDirectory("consumer-a/target");
        verifier.deleteDirectory("consumer-b/target");
        verifier.deleteDirectory("consumer-c/target");
        verifier.deleteArtifacts("org.apache.maven.its.mng3043", null);
        verifier.setLogFileName("log-package-pre.txt");
        verifier.addCliArguments("--also-make", "--projects", ":dependency", "package");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.setLogFileName("log-package-pre.txt");
        verifier.addCliArguments("--projects", ":consumer-a,:consumer-b,:consumer-c", "package");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        String prefix = "dependency-0.1-SNAPSHOT-";

        List<String> classpath;

        classpath = verifier.loadLines("consumer-a/target/compile.txt");
        assertContains(classpath, new String[] {prefix + "tests.jar"});
        assertNotContains(classpath, new String[] {prefix + "client.jar"});
        classpath = verifier.loadLines("consumer-a/target/runtime.txt");
        assertContains(classpath, new String[] {prefix + "tests.jar"});
        assertNotContains(classpath, new String[] {prefix + "client.jar"});
        classpath = verifier.loadLines("consumer-a/target/test.txt");
        assertContains(classpath, new String[] {prefix + "tests.jar"});
        assertNotContains(classpath, new String[] {prefix + "client.jar"});

        classpath = verifier.loadLines("consumer-b/target/compile.txt");
        assertContains(classpath, new String[] {prefix + "client.jar"});
        assertNotContains(classpath, new String[] {prefix + "tests.jar"});
        classpath = verifier.loadLines("consumer-b/target/runtime.txt");
        assertContains(classpath, new String[] {prefix + "client.jar"});
        assertNotContains(classpath, new String[] {prefix + "tests.jar"});
        classpath = verifier.loadLines("consumer-b/target/test.txt");
        assertContains(classpath, new String[] {prefix + "client.jar"});
        assertNotContains(classpath, new String[] {prefix + "tests.jar"});

        classpath = verifier.loadLines("consumer-c/target/compile.txt");
        assertContains(classpath, new String[] {prefix + "client.jar"});
        assertContains(classpath, new String[] {prefix + "tests.jar"});
        classpath = verifier.loadLines("consumer-c/target/runtime.txt");
        assertContains(classpath, new String[] {prefix + "client.jar"});
        assertContains(classpath, new String[] {prefix + "tests.jar"});
        classpath = verifier.loadLines("consumer-c/target/test.txt");
        assertContains(classpath, new String[] {prefix + "client.jar"});
        assertContains(classpath, new String[] {prefix + "tests.jar"});
    }

    private void assertContains(List<String> collection, String[] items) {
        for (String item : items) {
            assertContains(collection, item);
        }
    }

    private void assertContains(List<String> collection, String item) {
        assertTrue(collection.contains(item), item + " missing in " + collection);
    }

    private void assertNotContains(List<String> collection, String[] items) {
        for (String item : items) {
            assertNotContains(collection, item);
        }
    }

    private void assertNotContains(List<String> collection, String item) {
        assertFalse(collection.contains(item), item + " present in " + collection);
    }
}
