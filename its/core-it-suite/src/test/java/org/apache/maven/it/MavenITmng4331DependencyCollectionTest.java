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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4331">MNG-4331</a>.
 *
 * @author Benjamin Bentmann
 */
public class MavenITmng4331DependencyCollectionTest extends AbstractMavenIntegrationTestCase {

    public MavenITmng4331DependencyCollectionTest() {
        super("[3.0-alpha-3,)");
    }

    /**
     * Test that @requiresDependencyCollection works for a goal that is bound into a very early lifecycle phase
     * like "validate" where none of the reactor projects have an artifact file. The Enforcer Plugin is the
     * real world example for this use case.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitEarlyLifecyclePhase() throws Exception {
        File testDir = extractResources("/mng-4331");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteArtifacts("org.apache.maven.its.mng4331");
        verifier.deleteDirectory("sub-2/target");
        verifier.setLogFileName("log-lifecycle.txt");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        List<String> artifacts = verifier.loadLines("sub-2/target/compile.txt");
        assertTrue(artifacts.contains("org.apache.maven.its.mng4331:sub-1:jar:0.1"), artifacts.toString());
        assertEquals(1, artifacts.size());
    }

    /**
     * Test that @requiresDependencyCollection works for an aggregator goal that is invoked from the command line.
     * The Release Plugin is the real world example for this use case.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitCliAggregator() throws Exception {
        File testDir = extractResources("/mng-4331");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.deleteArtifacts("org.apache.maven.its.mng4331");
        verifier.addCliArgument("-Ddepres.projectArtifacts=target/@artifactId@.txt");
        verifier.setLogFileName("log-aggregator.txt");
        verifier.addCliArgument(
                "org.apache.maven.its.plugins:maven-it-plugin-dependency-collection:2.1-SNAPSHOT:aggregate-test");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        List<String> artifacts = verifier.loadLines("target/sub-2.txt");
        assertTrue(artifacts.contains("org.apache.maven.its.mng4331:sub-1:jar:0.1"), artifacts.toString());
        assertEquals(1, artifacts.size());

        artifacts = verifier.loadLines("target/sub-1.txt");
        assertEquals(0, artifacts.size());

        artifacts = verifier.loadLines("target/test.txt");
        assertEquals(0, artifacts.size());
    }
}
