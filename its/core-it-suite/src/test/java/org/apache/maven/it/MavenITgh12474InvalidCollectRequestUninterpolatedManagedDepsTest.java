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
 * Verify that inheriting a managed dependency whose version contains an unresolved
 * {@code ${…}} property placeholder does not cause an "Invalid Collect Request: null"
 * build failure, even when a plugin (such as {@code maven-enforcer-plugin}) constructs
 * its own {@code CollectRequest} from {@code project.getDependencyManagement()}.
 * <p>
 * This is a regression test for a problem where the resolver's {@code MavenValidator}
 * rejected uninterpolated managed dependencies during {@code collectDependencies()}.
 * The previous fix (#12305) filtered these entries in {@code ArtifactDescriptorReaderDelegate}
 * and {@code DefaultProjectDependenciesResolver}, but third-party plugins that build
 * their own {@code CollectRequest} directly from the project model still hit the error.
 * The fix for this issue (#12474) filters uninterpolated managed dependencies at the
 * model builder level, so all consumers see clean data.
 *
 * @see <a href="https://github.com/apache/maven/issues/12474">gh-12474</a>
 */
public class MavenITgh12474InvalidCollectRequestUninterpolatedManagedDepsTest extends AbstractMavenIntegrationTestCase {

    @Test
    public void testInheritedUninterpolatedManagedDepsWithEnforcer() throws Exception {
        Path testDir = extractResources("/gh-12474-invalid-collect-request-managed-deps");

        Verifier verifier = newVerifier(testDir);
        verifier.deleteArtifacts("org.apache.maven.its.gh12474");
        verifier.filterFile("settings-template.xml", "settings.xml");
        verifier.addCliArgument("--settings");
        verifier.addCliArgument("settings.xml");
        verifier.addCliArgument("package");
        verifier.execute();
        verifier.verifyErrorFreeLog();
    }
}
