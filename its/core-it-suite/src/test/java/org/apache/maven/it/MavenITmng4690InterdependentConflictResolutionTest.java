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

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4690">MNG-4690</a>.
 *
 * @author Benjamin Bentmann
 */
public class MavenITmng4690InterdependentConflictResolutionTest extends AbstractMavenIntegrationTestCase {

    public MavenITmng4690InterdependentConflictResolutionTest() {
        super("[2.0.9,)");
    }

    // Ideally, all six permutations of the three direct dependencies should yield the same result...

    @Test
    public void testitADX() throws Exception {
        requiresMavenVersion("[3.0-beta-3,)");
        testit("test-adx");
    }

    @Test
    public void testitAXD() throws Exception {
        testit("test-axd");
    }

    @Test
    public void testitDAX() throws Exception {
        requiresMavenVersion("[3.0-beta-3,)");
        testit("test-dax");
    }

    @Test
    public void testitDXA() throws Exception {
        testit("test-dxa");
    }

    @Test
    public void testitXAD() throws Exception {
        testit("test-xad");
    }

    @Test
    public void testitXDA() throws Exception {
        testit("test-xda");
    }

    /**
     * Verify that conflict resolution doesn't depend on the declaration order of dependencies (from distinct tree
     * levels) when the resolution of one conflict influences another conflict.
     */
    private void testit(String test) throws Exception {
        File testDir = extractResources("/mng-4690");

        Verifier verifier = newVerifier(new File(testDir, test).getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.deleteArtifacts("org.apache.maven.its.mng4690");
        verifier.addCliArgument("-s");
        verifier.addCliArgument("settings.xml");
        verifier.filterFile("../settings-template.xml", "settings.xml");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        List<String> classpath = verifier.loadLines("target/classpath.txt");

        assertTrue(classpath.contains("a-1.jar"), test + " > " + classpath.toString());
        assertTrue(classpath.contains("b-1.jar"), test + " > " + classpath.toString());
        assertTrue(classpath.contains("c-1.jar"), test + " > " + classpath.toString());
        assertTrue(classpath.contains("d-1.jar"), test + " > " + classpath.toString());

        assertTrue(classpath.contains("x-1.jar"), test + " > " + classpath.toString());
        assertTrue(classpath.contains("y-2.jar"), test + " > " + classpath.toString());
    }
}
