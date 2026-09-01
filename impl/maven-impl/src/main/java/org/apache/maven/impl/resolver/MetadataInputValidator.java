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
package org.apache.maven.impl.resolver;

import java.io.IOException;

import org.apache.maven.api.metadata.Snapshot;
import org.apache.maven.api.metadata.SnapshotVersion;
import org.apache.maven.api.metadata.Versioning;

/**
 * Validates metadata content parsed from remote {@code maven-metadata.xml} files before the values
 * are used to compose filesystem paths or artifact coordinates.
 * <p>
 * Repository metadata is not covered by the model validator (it is not a POM), so version tokens,
 * snapshot timestamps, and relocation coordinates must be checked at the point of use. Values that
 * would map onto filesystem path-traversal segments ({@code ..}), separators ({@code /}, {@code \}),
 * drive-letter delimiters ({@code :}), or ISO control characters are rejected.
 *
 * @since 4.0.0
 */
public final class MetadataInputValidator {

    private MetadataInputValidator() {}

    /**
     * Returns {@code true} if the value is unsafe for use as a coordinate component in a filesystem
     * path: it is {@code ".."}, contains a separator ({@code /}, {@code \}, {@code :}), or contains
     * an ISO control character.
     */
    public static boolean isInvalidCoordinateComponent(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        if ("..".equals(value) || value.contains("/") || value.contains("\\") || value.contains(":")) {
            return true;
        }
        for (int i = 0; i < value.length(); i++) {
            if (Character.isISOControl(value.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Validates all version-related tokens inside a parsed {@link Versioning} element.
     *
     * @throws IOException if any token is invalid
     */
    public static void validateVersioning(Versioning versioning) throws IOException {
        if (versioning == null) {
            return;
        }
        validateVersionToken(versioning.getLatest(), "latest version");
        validateVersionToken(versioning.getRelease(), "release version");
        for (String version : versioning.getVersions()) {
            validateVersionToken(version, "version");
        }
        for (SnapshotVersion snapshotVersion : versioning.getSnapshotVersions()) {
            validateVersionToken(snapshotVersion.getVersion(), "snapshot version");
        }
        Snapshot snapshot = versioning.getSnapshot();
        if (snapshot != null) {
            validateVersionToken(snapshot.getTimestamp(), "snapshot timestamp");
        }
    }

    /**
     * Validates a single version token from repository metadata.
     *
     * @throws IOException if the token contains path-traversal sequences, separators, or control characters
     */
    public static void validateVersionToken(String value, String description) throws IOException {
        if (isInvalidCoordinateComponent(value)) {
            throw new IOException("Rejecting metadata with invalid " + description + " '" + value
                    + "': must not contain '..', '/', '\\', ':' or control characters");
        }
    }
}
