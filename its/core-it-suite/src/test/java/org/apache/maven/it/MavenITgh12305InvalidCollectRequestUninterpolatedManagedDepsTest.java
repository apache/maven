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
 * Verify that importing a BOM whose dependencyManagement contains a managed dependency
 * with an unresolved {@code ${…}} property placeholder does not cause a build failure
 * when that managed dependency is never actually used.
 * <p>
 * This reproduces the "Invalid Collect Request: null" error seen with projects like
 * Apache Causeway, where the parent BOM declares {@code org.osgi:osgi.core:${osgi.version}}
 * without defining {@code osgi.version}. Maven 4's {@code MavenValidator} rejects
 * the uninterpolated version during {@code collectDependencies()}, even though the
 * dependency is never resolved.
 *
 * @see <a href="https://github.com/apache/maven/issues/12305">gh-12305</a>
 */
public class MavenITgh12305InvalidCollectRequestUninterpolatedManagedDepsTest extends AbstractMavenIntegrationTestCase {

    MavenITgh12305InvalidCollectRequestUninterpolatedManagedDepsTest() {
        super("[4.0.0-rc-3,)");
    }

    @Test
    public void testUninterpolatedManagedDepsFromImportedBom() throws Exception {
        File testDir = extractResources("/gh-12305-invalid-collect-request");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.deleteArtifacts("org.apache.maven.its.gh12305");
        verifier.filterFile("settings-template.xml", "settings.xml");
        verifier.addCliArgument("--settings");
        verifier.addCliArgument("settings.xml");
        verifier.addCliArgument("compile");
        verifier.execute();
        verifier.verifyErrorFreeLog();
    }
}
