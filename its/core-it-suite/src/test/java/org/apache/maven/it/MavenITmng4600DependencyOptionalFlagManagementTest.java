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
import java.util.Properties;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4600">MNG-4600</a>.
 *
 * @author Benjamin Bentmann
 */
public class MavenITmng4600DependencyOptionalFlagManagementTest extends AbstractMavenIntegrationTestCase {

    public MavenITmng4600DependencyOptionalFlagManagementTest() {
        super("[2.0.3,3.0-alpha-1),[3.0-beta-1,)");
    }

    /**
     * Verify that a dependency's optional flag is not subject to dependency management. This part of the test checks
     * the effective model.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitModel() throws Exception {
        File testDir = extractResources("/mng-4600/model");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        Properties props = verifier.loadProperties("target/pom.properties");
        assertEquals("dep", props.getProperty("project.dependencies.0.artifactId"));
        assertEquals("false", props.getProperty("project.dependencies.0.optional"));
    }

    /**
     * Verify that a transitive dependency's optional flag is not subject to dependency management of the root artifact.
     * This part of the test checks the artifact collector.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitResolution() throws Exception {
        File testDir = extractResources("/mng-4600/resolution");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.deleteArtifacts("org.apache.maven.its.mng4600");
        verifier.filterFile("settings-template.xml", "settings.xml");
        verifier.addCliArgument("--settings");
        verifier.addCliArgument("settings.xml");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        List<String> classpath = verifier.loadLines("target/classpath.txt");
        assertTrue(classpath.contains("direct-0.2.jar"), classpath.toString());
        assertTrue(classpath.contains("transitive-0.1.jar"), classpath.toString());
    }
}
