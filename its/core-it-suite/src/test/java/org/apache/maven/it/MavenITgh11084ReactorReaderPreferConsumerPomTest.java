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

import org.junit.jupiter.api.Test;

/**
 * This is a test set for GH-11084.
 * @since 4.0.0-rc-2
 *
 */
class MavenITgh11084ReactorReaderPreferConsumerPomTest extends AbstractMavenIntegrationTestCase {

    @Test
    void partialReactorShouldResolveUsingConsumerPom() throws Exception {
        File testDir = extractResources("/gh-11084-reactorreader-prefer-consumer-pom");

        // First build module a to populate project-local-repo with artifacts including consumer POM
        Verifier v1 = newVerifier(testDir.getAbsolutePath());
        v1.addCliArguments("clean", "package", "-X");
        v1.setLogFileName("log-1.txt");
        v1.execute();
        v1.verifyErrorFreeLog();

        // Now build only module b; ReactorReader should pick consumer POM from project-local-repo
        Verifier v2 = newVerifier(testDir.getAbsolutePath());
        v2.setLogFileName("log-2.txt");
        v2.addCliArguments("clean", "compile", "-f", "b", "-X");
        v2.execute();
        v2.verifyErrorFreeLog();
    }
}
