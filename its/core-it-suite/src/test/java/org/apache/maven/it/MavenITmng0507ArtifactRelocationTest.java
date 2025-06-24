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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-507">MNG-507</a>.
 *
 * @author Brett Porter
 *
 */
public class MavenITmng0507ArtifactRelocationTest extends AbstractMavenIntegrationTestCase {
    public MavenITmng0507ArtifactRelocationTest() {
        super(ALL_MAVEN_VERSIONS);
    }

    /**
     * Test artifact relocation.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitMNG507() throws Exception {
        File testDir = extractResources("/mng-0507");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.deleteArtifacts("org.apache.maven", "maven-core-it-support", "1.1");
        verifier.deleteArtifacts("org.apache.maven", "maven-core-it-support-old-location", "1.1");
        verifier.addCliArgument("--settings");
        verifier.addCliArgument("settings.xml");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        verifier.verifyArtifactPresent("org.apache.maven", "maven-core-it-support", "1.1", "jar");
        verifier.verifyArtifactPresent("org.apache.maven", "maven-core-it-support", "1.1", "pom");
        verifier.verifyArtifactPresent("org.apache.maven", "maven-core-it-support-old-location", "1.1", "pom");
        verifier.verifyArtifactNotPresent("org.apache.maven", "maven-core-it-support-old-location", "1.1", "jar");

        List<String> artifacts = verifier.loadLines("target/artifacts.txt");
        assertTrue(artifacts.contains("org.apache.maven:maven-core-it-support:jar:1.1"), artifacts.toString());
    }
}
