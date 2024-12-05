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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4320">MNG-4320</a>.
 *
 * @author Benjamin Bentmann
 */
public class MavenITmng4320AggregatorAndDependenciesTest extends AbstractMavenIntegrationTestCase {

    public MavenITmng4320AggregatorAndDependenciesTest() {
        super(ALL_MAVEN_VERSIONS);
    }

    /**
     * Verify that for aggregator mojos invoked from the CLI that require dependency resolution the dependencies
     * of all projects in the reactor are resolved and not only the dependencies of the top-level project.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testit() throws Exception {
        File testDir = extractResources("/mng-4320");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.deleteArtifacts("org.apache.maven.its.mng4320");
        verifier.addCliArgument("-s");
        verifier.addCliArgument("settings.xml");
        verifier.filterFile("settings-template.xml", "settings.xml");
        verifier.addCliArgument("org.apache.maven.its.plugins:maven-it-plugin-dependency-resolution:aggregate-test");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        List<String> classpath;

        classpath = verifier.loadLines("target/sub-1.txt");
        assertTrue(classpath.contains("a-0.1.jar"), classpath.toString());

        classpath = verifier.loadLines("target/sub-2.txt");
        assertTrue(classpath.contains("b-0.2.jar"), classpath.toString());

        classpath = verifier.loadLines("target/aggregator.txt");
        assertFalse(classpath.contains("a-0.1.jar"), classpath.toString());
        assertFalse(classpath.contains("b-0.2.jar"), classpath.toString());
    }
}
