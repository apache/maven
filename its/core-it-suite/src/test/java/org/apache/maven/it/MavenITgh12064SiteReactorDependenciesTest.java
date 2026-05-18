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

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * This is a test set for <a href="https://github.com/apache/maven/issues/12064">GH-12064</a>.
 */
class MavenITgh12064SiteReactorDependenciesTest extends AbstractMavenIntegrationTestCase {

    @Test
    void testSiteLifecycleResolvesReactorDependencies() throws Exception {
        File testDir = extractResources("/gh-12064-site-reactor-dependencies");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteDirectory("producer/target");
        verifier.deleteDirectory("consumer/target");
        verifier.deleteArtifacts("org.apache.maven.its.gh12064");
        verifier.addCliArgument("site");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        verifier.verifyFilePresent("consumer/target/site/dependencies.html");
        assertFalse(new File(testDir, "producer/target/classes").exists(), "site must not require compile first");
    }
}
