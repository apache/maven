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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-947">MNG-947</a>.
 *
 * @author Benjamin Bentmann
 */
public class MavenITmng0947OptionalDependencyTest extends AbstractMavenIntegrationTestCase {

    public MavenITmng0947OptionalDependencyTest() {
        super(ALL_MAVEN_VERSIONS);
    }

    /**
     * Verify that direct optional dependencies are included in the project class paths while transitive optional
     * dependencies are excluded.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testit() throws Exception {
        // failingMavenVersions("(,3.1.0-alpha-1)");

        File testDir = extractResources("/mng-0947");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.deleteArtifacts("org.apache.maven.its.mng0947");
        verifier.filterFile("settings-template.xml", "settings.xml");
        verifier.addCliArgument("--settings");
        verifier.addCliArgument("settings.xml");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        List<String> compile = verifier.loadLines("target/compile.txt");
        assertTrue(compile.contains("org.apache.maven.its.mng0947:d:jar:0.1 (optional)"), compile.toString());
        assertTrue(compile.contains("org.apache.maven.its.mng0947:e:jar:0.1 (optional)"), compile.toString());
        assertEquals(2, compile.size());

        List<String> runtime = verifier.loadLines("target/runtime.txt");
        assertTrue(runtime.contains("org.apache.maven.its.mng0947:c:jar:0.1"), runtime.toString());
        assertTrue(runtime.contains("org.apache.maven.its.mng0947:d:jar:0.1 (optional)"), runtime.toString());
        assertTrue(runtime.contains("org.apache.maven.its.mng0947:e:jar:0.1 (optional)"), runtime.toString());
        assertEquals(3, runtime.size());

        List<String> test = verifier.loadLines("target/test.txt");
        assertTrue(test.contains("org.apache.maven.its.mng0947:c:jar:0.1"), test.toString());
        assertTrue(test.contains("org.apache.maven.its.mng0947:d:jar:0.1 (optional)"), test.toString());
        assertTrue(test.contains("org.apache.maven.its.mng0947:e:jar:0.1 (optional)"), test.toString());
        assertEquals(3, test.size());
    }
}
