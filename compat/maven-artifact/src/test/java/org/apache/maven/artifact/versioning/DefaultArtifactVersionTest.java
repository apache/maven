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
package org.apache.maven.artifact.versioning;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test DefaultArtifactVersion.
 *
 */
class DefaultArtifactVersionTest {
    private ArtifactVersion newArtifactVersion(String version) {
        return new DefaultArtifactVersion(version);
    }

    private void checkVersionParsing(
            String version, int major, int minor, int incremental, int buildnumber, String qualifier) {
        ArtifactVersion artifactVersion = newArtifactVersion(version);
        String parsed = "'" + version + "' parsed as ('" + artifactVersion.getMajorVersion() + "', '"
                + artifactVersion.getMinorVersion() + "', '" + artifactVersion.getIncrementalVersion() + "', '"
                + artifactVersion.getBuildNumber() + "', '" + artifactVersion.getQualifier() + "'), ";
        assertEquals(major, artifactVersion.getMajorVersion(), parsed + "check major version");
        assertEquals(minor, artifactVersion.getMinorVersion(), parsed + "check minor version");
        assertEquals(incremental, artifactVersion.getIncrementalVersion(), parsed + "check incremental version");
        assertEquals(buildnumber, artifactVersion.getBuildNumber(), parsed + "check build number");
        assertEquals(qualifier, artifactVersion.getQualifier(), parsed + "check qualifier");
        assertEquals(version, artifactVersion.toString(), "check " + version + " string value");
    }

    @Test
    void testVersionParsing() {
        checkVersionParsing("1", 1, 0, 0, 0, null);
        checkVersionParsing("1.2", 1, 2, 0, 0, null);
        checkVersionParsing("1.2.3", 1, 2, 3, 0, null);
        checkVersionParsing("1.2.3-1", 1, 2, 3, 1, null);
        checkVersionParsing("1.2.3-alpha-1", 1, 2, 3, 0, "alpha-1");
        checkVersionParsing("1.2-alpha-1", 1, 2, 0, 0, "alpha-1");
        checkVersionParsing("1.2-alpha-1-20050205.060708-1", 1, 2, 0, 0, "alpha-1-20050205.060708-1");
        checkVersionParsing("RELEASE", 0, 0, 0, 0, "RELEASE");
        checkVersionParsing("2.0-1", 2, 0, 0, 1, null);

        // 0 at the beginning of a number has a special handling
        checkVersionParsing("02", 0, 0, 0, 0, "02");
        checkVersionParsing("0.09", 0, 0, 0, 0, "0.09");
        checkVersionParsing("0.2.09", 0, 0, 0, 0, "0.2.09");
        checkVersionParsing("2.0-01", 2, 0, 0, 0, "01");

        // version schemes not really supported: fully transformed as qualifier
        checkVersionParsing("1.0.1b", 0, 0, 0, 0, "1.0.1b");
        checkVersionParsing("1.0M2", 0, 0, 0, 0, "1.0M2");
        checkVersionParsing("1.0RC2", 0, 0, 0, 0, "1.0RC2");
        checkVersionParsing("1.1.2.beta1", 1, 1, 2, 0, "beta1");
        checkVersionParsing("1.7.3.beta1", 1, 7, 3, 0, "beta1");
        checkVersionParsing("1.7.3.0", 0, 0, 0, 0, "1.7.3.0");
        checkVersionParsing("1.7.3.0-1", 0, 0, 0, 0, "1.7.3.0-1");
        checkVersionParsing("PATCH-1193602", 0, 0, 0, 0, "PATCH-1193602");
        checkVersionParsing("5.0.0alpha-2006020117", 0, 0, 0, 0, "5.0.0alpha-2006020117");
        checkVersionParsing("1.0.0.-SNAPSHOT", 0, 0, 0, 0, "1.0.0.-SNAPSHOT");
        checkVersionParsing("1..0-SNAPSHOT", 0, 0, 0, 0, "1..0-SNAPSHOT");
        checkVersionParsing("1.0.-SNAPSHOT", 0, 0, 0, 0, "1.0.-SNAPSHOT");
        checkVersionParsing(".1.0-SNAPSHOT", 0, 0, 0, 0, ".1.0-SNAPSHOT");

        checkVersionParsing("1.2.3.200705301630", 0, 0, 0, 0, "1.2.3.200705301630");
        checkVersionParsing("1.2.3-200705301630", 1, 2, 3, 0, "200705301630");
    }

    @Test
    void testVersionComparing() {
        assertVersionEqual("1", "1");
        assertVersionOlder("1", "2");
        assertVersionOlder("1.5", "2");
        assertVersionOlder("1", "2.5");
        assertVersionEqual("1", "1.0");
        assertVersionEqual("1", "1.0.0");
        assertVersionOlder("1.0", "1.1");
        assertVersionOlder("1.1", "1.2");
        assertVersionOlder("1.0.0", "1.1");
        assertVersionOlder("1.1", "1.2.0");

        assertVersionOlder("1.1.2.alpha1", "1.1.2");
        assertVersionOlder("1.1.2.alpha1", "1.1.2.beta1");
        assertVersionOlder("1.1.2.beta1", "1.2");

        assertVersionOlder("1.0-alpha-1", "1.0");
        assertVersionOlder("1.0-alpha-1", "1.0-alpha-2");
        assertVersionOlder("1.0-alpha-2", "1.0-alpha-15");
        assertVersionOlder("1.0-alpha-1", "1.0-beta-1");

        assertVersionOlder("1.0-beta-1", "1.0-SNAPSHOT");
        assertVersionOlder("1.0-SNAPSHOT", "1.0");
        assertVersionOlder("1.0-alpha-1-SNAPSHOT", "1.0-alpha-1");

        assertVersionOlder("1.0", "1.0-1");
        assertVersionOlder("1.0-1", "1.0-2");
        assertVersionEqual("2.0-0", "2.0");
        assertVersionOlder("2.0", "2.0-1");
        assertVersionOlder("2.0.0", "2.0-1");
        assertVersionOlder("2.0-1", "2.0.1");

        assertVersionOlder("2.0.1-klm", "2.0.1-lmn");
        assertVersionOlder("2.0.1", "2.0.1-xyz");
        assertVersionOlder("2.0.1-xyz-1", "2.0.1-1-xyz");

        assertVersionOlder("2.0.1", "2.0.1-123");
        assertVersionOlder("2.0.1-xyz", "2.0.1-123");

        assertVersionOlder("1.2.3-10000000000", "1.2.3-10000000001");
        assertVersionOlder("1.2.3-1", "1.2.3-10000000001");
        assertVersionOlder("2.3.0-v200706262000", "2.3.0-v200706262130"); // org.eclipse:emf:2.3.0-v200706262000
        // org.eclipse.wst.common_core.feature_2.0.0.v200706041905-7C78EK9E_EkMNfNOd2d8qq
        assertVersionOlder("2.0.0.v200706041905-7C78EK9E_EkMNfNOd2d8qq", "2.0.0.v200706041906-7C78EK9E_EkMNfNOd2d8qq");
    }

    @Test
    void testVersionSnapshotComparing() {
        assertVersionEqual("1-SNAPSHOT", "1-SNAPSHOT");
        assertVersionOlder("1-SNAPSHOT", "2-SNAPSHOT");
        assertVersionOlder("1.5-SNAPSHOT", "2-SNAPSHOT");
        assertVersionOlder("1-SNAPSHOT", "2.5-SNAPSHOT");
        assertVersionEqual("1-SNAPSHOT", "1.0-SNAPSHOT");
        assertVersionEqual("1-SNAPSHOT", "1.0.0-SNAPSHOT");
        assertVersionOlder("1.0-SNAPSHOT", "1.1-SNAPSHOT");
        assertVersionOlder("1.1-SNAPSHOT", "1.2-SNAPSHOT");
        assertVersionOlder("1.0.0-SNAPSHOT", "1.1-SNAPSHOT");
        assertVersionOlder("1.1-SNAPSHOT", "1.2.0-SNAPSHOT");

        // assertVersionOlder( "1.0-alpha-1-SNAPSHOT", "1.0-SNAPSHOT" );
        assertVersionOlder("1.0-alpha-1-SNAPSHOT", "1.0-alpha-2-SNAPSHOT");
        assertVersionOlder("1.0-alpha-1-SNAPSHOT", "1.0-beta-1-SNAPSHOT");

        assertVersionOlder("1.0-beta-1-SNAPSHOT", "1.0-SNAPSHOT-SNAPSHOT");
        assertVersionOlder("1.0-SNAPSHOT-SNAPSHOT", "1.0-SNAPSHOT");
        assertVersionOlder("1.0-alpha-1-SNAPSHOT-SNAPSHOT", "1.0-alpha-1-SNAPSHOT");

        assertVersionOlder("1.0-SNAPSHOT", "1.0-1-SNAPSHOT");
        assertVersionOlder("1.0-1-SNAPSHOT", "1.0-2-SNAPSHOT");
        // assertVersionEqual( "2.0-0-SNAPSHOT", "2.0-SNAPSHOT" );
        assertVersionOlder("2.0-SNAPSHOT", "2.0-1-SNAPSHOT");
        assertVersionOlder("2.0.0-SNAPSHOT", "2.0-1-SNAPSHOT");
        assertVersionOlder("2.0-1-SNAPSHOT", "2.0.1-SNAPSHOT");

        assertVersionOlder("2.0.1-klm-SNAPSHOT", "2.0.1-lmn-SNAPSHOT");
        // assertVersionOlder( "2.0.1-xyz-SNAPSHOT", "2.0.1-SNAPSHOT" );
        assertVersionOlder("2.0.1-SNAPSHOT", "2.0.1-123-SNAPSHOT");
        assertVersionOlder("2.0.1-xyz-SNAPSHOT", "2.0.1-123-SNAPSHOT");
    }

    @Test
    void testSnapshotVsReleases() {
        assertVersionOlder("1.0-RC1", "1.0-SNAPSHOT");
        assertVersionOlder("1.0-rc1", "1.0-SNAPSHOT");
        assertVersionOlder("1.0-rc-1", "1.0-SNAPSHOT");
    }

    @Test
    void testHashCode() {
        ArtifactVersion v1 = newArtifactVersion("1");
        ArtifactVersion v2 = newArtifactVersion("1.0");
        assertTrue(v1.equals(v2));
        assertEquals(v1.hashCode(), v2.hashCode());
    }

    @Test
    void testEqualsNullSafe() {
        assertFalse(newArtifactVersion("1").equals(null));
    }

    @Test
    void testEqualsTypeSafe() {
        assertFalse(newArtifactVersion("1").equals("non-an-artifact-version-instance"));
    }

    private void assertVersionOlder(String left, String right) {
        assertTrue(
                newArtifactVersion(left).compareTo(newArtifactVersion(right)) < 0,
                left + " should be older than " + right);
        assertTrue(
                newArtifactVersion(right).compareTo(newArtifactVersion(left)) > 0,
                right + " should be newer than " + left);
    }

    private void assertVersionEqual(String left, String right) {
        assertTrue(
                newArtifactVersion(left).compareTo(newArtifactVersion(right)) == 0,
                left + " should be equal to " + right);
        assertTrue(
                newArtifactVersion(right).compareTo(newArtifactVersion(left)) == 0,
                right + " should be equal to " + left);
    }
}
