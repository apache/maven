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
 * Regression test for <a href="https://github.com/apache/maven/issues/13068">apache/maven#13068</a>.
 * The forward port in <a href="https://github.com/apache/maven/pull/12335">apache/maven#12335</a> added
 * {@code resolveCoreExtensionAndFlatten} and {@code resolvePluginAndFlatten} to
 * {@code PluginDependenciesResolver} as abstract methods in 4.0.0-rc-6.
 * <p>
 * That interface is documented as internal, but it is the only hook Maven offers for influencing plugin
 * resolution, and every major Java IDE overrides it: IntelliJ IDEA, Eclipse m2e and NetBeans. Because such
 * implementations live out of tree and are compiled against one Maven while running on another, adding
 * abstract methods turns every plugin resolution into an {@code AbstractMethodError}.
 * <p>
 * This test builds a core extension against maven-core 4.0.0-rc-5 — the last release before those methods
 * existed — installs it, and then runs a build that uses it. If the newer interface methods are abstract,
 * the build fails before any goal executes.
 */
class MavenITgh13068LegacyPluginDependenciesResolverTest extends AbstractMavenIntegrationTestCase {

    MavenITgh13068LegacyPluginDependenciesResolverTest() {
        // resolvePluginAndFlatten / resolveCoreExtensionAndFlatten were introduced in 4.0.0-rc-6
        super("[4.0.0-rc-6,)");
    }

    @Test
    void legacyImplementationStillWorks() throws Exception {
        File testDir = extractResources("/gh-13068-legacy-plugin-dependencies-resolver");

        Verifier extensionVerifier = newVerifier(new File(testDir, "extension").getPath());
        extensionVerifier.deleteArtifacts("org.apache.maven.its.gh-13068");
        extensionVerifier.addCliArgument("install");
        extensionVerifier.execute();
        extensionVerifier.verifyErrorFreeLog();

        Verifier clientVerifier = newVerifier(new File(testDir, "client").getPath());
        clientVerifier.setAutoclean(false);
        clientVerifier.addCliArgument("clean");
        clientVerifier.execute();
        clientVerifier.verifyErrorFreeLog();

        // the extension must actually have displaced the default component, otherwise this test
        // would pass without ever exercising the interface
        clientVerifier.verifyTextInLog("[gh-13068] legacy PluginDependenciesResolver installed");
        clientVerifier.verifyTextInLog("[gh-13068] resolvePlugin reached for maven-clean-plugin");
    }
}
