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
package org.apache.maven.plugin.version.internal;

import org.apache.maven.plugin.MavenPluginManager;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.version.DefaultPluginVersionRequest;
import org.eclipse.aether.util.version.GenericVersionScheme;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Which versions are excluded when a plugin is resolved without a declared version.
 */
class DefaultPluginVersionResolverTest {

    private final DefaultPluginVersionResolver resolver =
            new DefaultPluginVersionResolver(null, null, null, new GenericVersionScheme());

    /**
     * A resolver for which every candidate passes the compatibility check, so the tests observe the
     * selection order alone.
     */
    private static DefaultPluginVersionResolver resolverAcceptingEveryVersion() throws Exception {
        MavenPluginManager pluginManager = Mockito.mock(MavenPluginManager.class);
        Mockito.when(pluginManager.getPluginDescriptor(Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(new PluginDescriptor());
        return new DefaultPluginVersionResolver(null, null, pluginManager, new GenericVersionScheme());
    }

    private static DefaultPluginVersionResolver.Versions versions(String release, String... available) {
        DefaultPluginVersionResolver.Versions versions = new DefaultPluginVersionResolver.Versions();
        versions.releaseVersion = release;
        for (String version : available) {
            versions.versions.put(version, null);
        }
        return versions;
    }

    private static String select(DefaultPluginVersionResolver resolver, DefaultPluginVersionResolver.Versions versions)
            throws Exception {
        DefaultPluginVersionResult result = new DefaultPluginVersionResult();
        resolver.selectVersion(
                result,
                new DefaultPluginVersionRequest()
                        .setGroupId("org.apache.maven.plugins")
                        .setArtifactId("maven-it-plugin"),
                versions);
        return result.getVersion();
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "1.0-alpha-1",
                "1.0-alpha1",
                "1.0-beta-1",
                "4.0.0-beta-1",
                "1.0-milestone-1",
                "1.0-M1",
                "1.0-rc-1",
                "1.0-rc1",
                "1.0-cr1",
                "1.0-b2", // short form, as GenericQualifiers reads it
                "1.0.beta.1", // the scheme treats '.' and '-' as the same separator
                "1.0_alpha_1",
                "1.0-SNAPSHOT"
            })
    void preReleaseQualifiersAreExcluded(String version) {
        assertTrue(resolver.isPreRelease(version), version + " should count as a pre-release");
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "1.0",
                "1.0.1",
                "3.15.0",
                "4.0.0",
                "33.7.0-jre", // vendor/classifier qualifier, not a pre-release
                "1.0-arc", // contains "rc" but is not a release candidate
                "1.0-sp1", // service pack: sorts *after* the release
                "1.0-final", // alias of the release itself
                "1.0-ga"
            })
    void releasesAreNotExcluded(String version) {
        assertFalse(resolver.isPreRelease(version), version + " should not count as a pre-release");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "-", "not-a-version"})
    void unparseableVersionsAreNotExcluded(String version) {
        assertFalse(resolver.isPreRelease(version), version + " should not count as a pre-release");
    }

    @Test
    void stableVersionIsDetectedAmongPreReleases() {
        DefaultPluginVersionResolver.Versions versions = new DefaultPluginVersionResolver.Versions();
        versions.versions.put("2.0-beta-1", null);
        versions.versions.put("2.0-beta-2", null);
        assertFalse(resolver.hasStableVersion(versions), "only pre-releases available");

        versions.versions.put("1.0-SNAPSHOT", null);
        assertFalse(resolver.hasStableVersion(versions), "snapshots are not a stable fallback either");

        versions.versions.put("1.0", null);
        assertTrue(resolver.hasStableVersion(versions), "1.0 is a stable fallback");
    }

    @Test
    void aStableVersionWinsOverThePreReleaseNamedByTheMetadata() throws Exception {
        assertEquals(
                "1.0",
                select(resolverAcceptingEveryVersion(), versions("2.0-beta-1", "1.0", "2.0-beta-1")),
                "metadata says 2.0-beta-1 is the RELEASE, but 1.0 is the newest stable one");
    }

    @Test
    void theNewestStableVersionIsSelected() throws Exception {
        assertEquals(
                "1.2", select(resolverAcceptingEveryVersion(), versions("2.0-beta-1", "1.0", "1.2", "2.0-beta-1")));
    }

    @Test
    void aPreReleaseIsUsedWhenNoStableVersionExists() throws Exception {
        assertEquals(
                "2.0-beta-2",
                select(resolverAcceptingEveryVersion(), versions("2.0-beta-2", "2.0-beta-1", "2.0-beta-2")),
                "a plugin that only ever published pre-releases must still resolve");
    }

    @Test
    void aSnapshotIsTheLastResort() throws Exception {
        assertEquals(
                "1.0-SNAPSHOT",
                select(resolverAcceptingEveryVersion(), versions("", "1.0-SNAPSHOT")),
                "snapshots rank below pre-releases");
    }

    @Test
    void aStableReleaseVersionIsTakenAsIs() throws Exception {
        assertEquals(
                "2.0",
                select(resolverAcceptingEveryVersion(), versions("2.0", "1.0", "2.0")),
                "no search is needed when the metadata already names a stable version");
    }
}
