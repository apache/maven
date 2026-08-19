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

import org.eclipse.aether.util.version.GenericVersionScheme;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Which versions are excluded when a plugin is resolved without a declared version.
 */
class DefaultPluginVersionResolverTest {

    private final DefaultPluginVersionResolver resolver =
            new DefaultPluginVersionResolver(null, null, null, new GenericVersionScheme());

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
}
