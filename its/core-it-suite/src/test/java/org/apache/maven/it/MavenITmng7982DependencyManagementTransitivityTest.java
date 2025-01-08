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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-7982">MNG-7982</a>.
 *
 * @author Didier Loiseau
 */
public class MavenITmng7982DependencyManagementTransitivityTest extends AbstractMavenIntegrationTestCase {

    public MavenITmng7982DependencyManagementTransitivityTest() {
        super("[4.0.0-beta-5,)");
    }

    /**
     * Verify the effective dependency versions determined when using the transitive dependency management (default).
     * <p>
     * By default, the dependency management of transitive dependencies should be taken into account.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitWithTransitiveDependencyManager() throws Exception {
        File testDir = extractResources("/mng-7982-transitive-dependency-management");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.deleteArtifacts("org.apache.maven.its.mng7982", "maven-core-it");
        verifier.addCliArgument("-s");
        verifier.addCliArgument("settings.xml");
        verifier.filterFile("settings-template.xml", "settings.xml");
        verifier.addCliArgument("verify");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        List<String> bClasspath = verifier.loadLines("b/target/classpath.txt");
        assertTrue(bClasspath.contains("a-1.jar"), bClasspath.toString());
        assertFalse(bClasspath.contains("a-2.jar"), bClasspath.toString());

        List<String> cClasspath = verifier.loadLines("c/target/classpath.txt");
        assertTrue(cClasspath.contains("b-1.jar"), cClasspath.toString());
        assertFalse(cClasspath.contains("a-1.jar"), cClasspath.toString());
        assertTrue(cClasspath.contains("a-2.jar"), cClasspath.toString());

        List<String> dClasspath = verifier.loadLines("d/target/classpath.txt");
        assertTrue(dClasspath.contains("c-1.jar"), dClasspath.toString());
        assertTrue(dClasspath.contains("b-1.jar"), dClasspath.toString());
        assertFalse(dClasspath.contains("a-1.jar"), dClasspath.toString());
        assertTrue(dClasspath.contains("a-2.jar"), dClasspath.toString());

        List<String> eClasspath = verifier.loadLines("e/target/classpath.txt");
        assertTrue(eClasspath.contains("d-1.jar"), eClasspath.toString());
        assertTrue(eClasspath.contains("c-1.jar"), eClasspath.toString());
        assertTrue(eClasspath.contains("b-1.jar"), eClasspath.toString());
        assertFalse(eClasspath.contains("a-1.jar"), eClasspath.toString());
        assertTrue(eClasspath.contains("a-2.jar"), eClasspath.toString());
    }

    /**
     * Verify the effective dependency versions determined when disabling the transitive dependency management (Maven 2/3 behavior).
     * <p>
     * When transitivity is disabled, the dependency management of transitive dependencies should is ignored.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitWithTransitiveDependencyManagerDisabled() throws Exception {
        File testDir = extractResources("/mng-7982-transitive-dependency-management");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.deleteArtifacts("org.apache.maven.its.mng7982", "maven-core-it");
        verifier.addCliArgument("-s");
        verifier.addCliArgument("settings.xml");
        verifier.filterFile("settings-template.xml", "settings.xml");
        verifier.addCliArgument("verify");
        verifier.addCliArgument("-Dmaven.resolver.dependencyManagerTransitivity=false");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        List<String> bClasspath = verifier.loadLines("b/target/classpath.txt");
        assertTrue(bClasspath.contains("a-1.jar"), bClasspath.toString());
        assertFalse(bClasspath.contains("a-2.jar"), bClasspath.toString());

        List<String> cClasspath = verifier.loadLines("c/target/classpath.txt");
        assertTrue(cClasspath.contains("b-1.jar"), cClasspath.toString());
        assertFalse(cClasspath.contains("a-1.jar"), cClasspath.toString());
        assertTrue(cClasspath.contains("a-2.jar"), cClasspath.toString());

        List<String> dClasspath = verifier.loadLines("d/target/classpath.txt");
        assertTrue(dClasspath.contains("c-1.jar"), dClasspath.toString());
        assertTrue(dClasspath.contains("b-1.jar"), dClasspath.toString());
        // dependency management of c is ignored
        assertTrue(dClasspath.contains("a-1.jar"), dClasspath.toString());
        assertFalse(dClasspath.contains("a-2.jar"), dClasspath.toString());

        List<String> eClasspath = verifier.loadLines("e/target/classpath.txt");
        assertTrue(eClasspath.contains("d-1.jar"), eClasspath.toString());
        assertTrue(eClasspath.contains("c-1.jar"), eClasspath.toString());
        assertTrue(eClasspath.contains("b-1.jar"), eClasspath.toString());
        // dependency management of c is ignored
        assertTrue(dClasspath.contains("a-1.jar"), dClasspath.toString());
        assertFalse(dClasspath.contains("a-2.jar"), dClasspath.toString());
    }
}
