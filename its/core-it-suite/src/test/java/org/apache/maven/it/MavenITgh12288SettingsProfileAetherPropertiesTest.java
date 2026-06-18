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

import org.codehaus.plexus.util.FileUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests proving that {@code aether.*} properties declared in the
 * {@code <properties>} block of a {@code settings.xml} profile are honored
 * by the resolver at local repository manager initialization, regardless of
 * which settings.xml-only activation channel was used.
 *
 * <p>Two activation channels are covered:
 * <ul>
 *   <li>{@code <activation><activeByDefault>true</activeByDefault></activation>}
 *       on the profile itself;</li>
 *   <li>{@code <activeProfiles><activeProfile>...</activeProfile></activeProfiles>}
 *       at the top of {@code settings.xml}.</li>
 * </ul>
 *
 * <p>In both cases the same profile sets:
 * <pre>
 *   aether.enhancedLocalRepository.split       = true
 *   aether.enhancedLocalRepository.localPrefix = it-custom-prefix
 * </pre>
 * and the test asserts that {@code mvn install} writes the installed pom
 * under {@code <localRepo>/it-custom-prefix/&lt;groupId-path&gt;/...} rather
 * than the flat or default-split layout.
 *
 * <p>The same properties on the same profile work correctly when the
 * profile is activated via {@code -P <id>} on the CLI; only the
 * settings.xml activation channels fail, which is what these tests guard
 * against.
 */
public class MavenITgh12288SettingsProfileAetherPropertiesTest extends AbstractMavenIntegrationTestCase {

    MavenITgh12288SettingsProfileAetherPropertiesTest() {
        super("(4.0.0-rc-5,)");
    }

    @Test
    public void testActiveByDefaultProfile() throws Exception {
        runAndAssertCustomPrefix("settings-active-by-default.xml");
    }

    @Test
    public void testActiveProfilesList() throws Exception {
        runAndAssertCustomPrefix("settings-active-profiles-list.xml");
    }

    @Test
    public void testActiveByDefaultDeactivatedViaCli() throws Exception {
        File testDir = extractResources("/settings-profile-aether-properties");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.setLogFileName("log-deactivation.txt");
        verifier.deleteDirectory("target");
        verifier.deleteArtifacts("org.apache.maven.its.settings.profile.aether");

        // Sibling tests in this class install under the same custom prefix; clear it to
        // avoid false positives if those tests ran first.
        File customPrefixSubtree = new File(verifier.getLocalRepository(), "it-custom-prefix");
        FileUtils.deleteDirectory(customPrefixSubtree);

        verifier.addCliArgument("--settings");
        verifier.addCliArgument("settings-active-by-default.xml");
        verifier.addCliArgument("-P!aether-split-via-settings");
        verifier.addCliArgument("install");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        File localRepo = new File(verifier.getLocalRepository());
        String gavRelativePath = "org/apache/maven/its/settings/profile/aether/test-artifact/1.0/test-artifact-1.0.pom";
        File flatLayout = new File(localRepo, gavRelativePath);
        File customPrefix = new File(localRepo, "it-custom-prefix/" + gavRelativePath);

        assertTrue(
                flatLayout.exists(),
                "Expected artifact at flat layout (profile deactivated via -P !), but not found at " + flatLayout);

        assertFalse(
                customPrefix.exists(),
                "Found artifact at custom prefix " + customPrefix + " — deactivation via -P ! was ignored.");
    }

    private void runAndAssertCustomPrefix(String settingsFile) throws Exception {
        File testDir = extractResources("/settings-profile-aether-properties");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.deleteArtifacts("org.apache.maven.its.settings.profile.aether");

        verifier.addCliArgument("--settings");
        verifier.addCliArgument(settingsFile);
        verifier.addCliArgument("install");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        File localRepo = new File(verifier.getLocalRepository());
        String gavRelativePath = "org/apache/maven/its/settings/profile/aether/test-artifact/1.0/test-artifact-1.0.pom";

        File expectedAtCustomPrefix = new File(localRepo, "it-custom-prefix/" + gavRelativePath);
        File flatLayout = new File(localRepo, gavRelativePath);
        File defaultSplitPrefix = new File(localRepo, "installed/" + gavRelativePath);

        assertTrue(
                expectedAtCustomPrefix.exists(),
                "Expected install to use custom localPrefix 'it-custom-prefix' from "
                        + settingsFile
                        + ", but artifact not found at "
                        + expectedAtCustomPrefix);

        assertFalse(
                flatLayout.exists(),
                "Found artifact at flat layout "
                        + flatLayout
                        + " — indicates the settings.xml profile properties did not reach the resolver"
                        + " session config in time for LRM init.");

        assertFalse(
                defaultSplitPrefix.exists(),
                "Found artifact at default split-LRM prefix "
                        + defaultSplitPrefix
                        + " — indicates split=true was honored but localPrefix was silently dropped.");
    }
}
