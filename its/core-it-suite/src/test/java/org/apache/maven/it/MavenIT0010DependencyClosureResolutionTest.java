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

public class MavenIT0010DependencyClosureResolutionTest extends AbstractMavenIntegrationTestCase {
    public MavenIT0010DependencyClosureResolutionTest() {
        super(ALL_MAVEN_VERSIONS);
    }

    /**
     * Since the artifact resolution does not use the project builder, we must
     * ensure that the full hierarchy of all dependencies is resolved. This
     * includes the dependencies of the parent-pom's of dependencies.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testit0010() throws Exception {
        File testDir = extractResources("/it0010");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.deleteArtifacts("org.apache.maven.its.it0010");
        verifier.filterFile("settings-template.xml", "settings.xml");
        verifier.addCliArgument("--settings");
        verifier.addCliArgument("settings.xml");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        verifier.verifyArtifactPresent("org.apache.maven.its.it0010", "a", "0.1", "jar");
        verifier.verifyArtifactPresent("org.apache.maven.its.it0010", "b", "0.2", "jar");
        verifier.verifyArtifactPresent("org.apache.maven.its.it0010", "parent", "1.0", "pom");

        List<String> artifacts = verifier.loadLines("target/compile.txt");
        assertTrue(artifacts.contains("org.apache.maven.its.it0010:a:jar:0.1"), artifacts.toString());
        assertTrue(artifacts.contains("org.apache.maven.its.it0010:b:jar:0.2"), artifacts.toString());
    }
}
