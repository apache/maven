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

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * Verifies that {@code maven.lifecycle.filter} can target plugins by artifactId ({@code :A})
 * and by goal prefix.
 *
 * @see <a href="https://github.com/apache/maven/issues/12536">MNG-12536</a>
 */
class MavenITmng12536LifecycleFilterCoordinateTest extends AbstractMavenIntegrationTestCase {

    @Test
    void filterByArtifactIdSkipsSurefire() throws Exception {
        Path testDir = extractResources("/mng-12536-lifecycle-filter-coordinate");

        Verifier verifier = newVerifier(testDir);
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.addCliArgument("-Dmaven.lifecycle.filter=:maven-surefire-plugin");
        verifier.addCliArgument("test");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        verifier.verifyTextNotInLog("Tests run:");
    }

    @Test
    void filterByPrefixSkipsSurefire() throws Exception {
        Path testDir = extractResources("/mng-12536-lifecycle-filter-coordinate");

        Verifier verifier = newVerifier(testDir);
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.addCliArgument("-Dmaven.lifecycle.filter=surefire");
        verifier.addCliArgument("test");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        verifier.verifyTextNotInLog("Tests run:");
    }
}
