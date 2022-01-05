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
package org.apache.maven.artifact.repository.metadata.validator;

import javax.inject.Named;
import javax.inject.Singleton;

import java.util.Collection;

import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.Plugin;
import org.apache.maven.artifact.repository.metadata.SnapshotVersion;
import org.apache.maven.artifact.repository.metadata.validator.MetadataProblemCollector.Severity;

/**
 * Validates repository metadata on different levels.
 *
 * @see <a href="https://maven.apache.org/ref/current/maven-repository-metadata/index.html">Repository Metadata Model</a>
 * @see <a href="https://maven.apache.org/ref/current/maven-repository-metadata/repository-metadata.html">Repository Metadata Descriptor</a>
 */
@Named
@Singleton
public class DefaultMetadataValidator implements MetadataValidator {

    @Override
    public void validate(Metadata metadata, MetadataProblemCollector problems) {
        // some heuristic to determine level
        final Level level;
        if (!metadata.getPlugins().isEmpty()) {
            level = Level.GROUP_ID;
        } else if (metadata.getVersion() != null) {
            level = Level.VERSION;
        } else {
            level = Level.ARTIFACT_ID;
        }
        validate(metadata, level, null, problems);
    }

    @Override
    public void validate(Metadata metadata, Level level, Boolean isSnapshot, MetadataProblemCollector problems) {
        switch (level) {
            case GROUP_ID:
                for (Plugin plugin : metadata.getPlugins()) {
                    validateStringNotEmpty(
                            problems, "plugins.plugin.name", plugin.getName(), plugin.getArtifactId(), level);
                    validateStringNotEmpty(
                            problems, "plugins.plugin.prefix", plugin.getPrefix(), plugin.getName(), level);
                    validateStringNotEmpty(
                            problems, "plugins.plugin.artifactId", plugin.getArtifactId(), plugin.getName(), level);
                }
                validateNullOrEmptyCollection(problems, "groupId", metadata.getGroupId(), null, level);
                validateNullOrEmptyCollection(problems, "artifactId", metadata.getArtifactId(), null, level);
                validateNullOrEmptyCollection(problems, "version", metadata.getVersion(), null, level);
                validateNullOrEmptyCollection(problems, "versioning", metadata.getVersioning(), null, level);
                break;
            case ARTIFACT_ID:
                validateStringNotEmpty(problems, "groupId", metadata.getGroupId(), null, level);
                validateStringNotEmpty(problems, "artifactId", metadata.getArtifactId(), null, level);
                validateNullOrEmptyCollection(problems, "version", metadata.getVersion(), null, level);
                if (validateNotNull(problems, "versioning", metadata.getVersioning(), null, level)) {
                    validateNotEmptyCollection(
                            problems,
                            "versioning.versions",
                            metadata.getVersioning().getVersions(),
                            null,
                            level);
                    validateNullOrEmptyCollection(
                            problems,
                            "versioning.snapshotVersions",
                            metadata.getVersioning().getSnapshotVersions(),
                            null,
                            level);
                }
                validateNullOrEmptyCollection(problems, "plugins", metadata.getPlugins(), null, level);
                // TODO: release or latest is mandatory?
                break;
            default:
                if (isSnapshot == Boolean.FALSE) {
                    validateNullOrEmptyCollection(problems, "groupId", metadata.getGroupId(), null, level);
                    validateNullOrEmptyCollection(problems, "artifactId", metadata.getArtifactId(), null, level);
                    validateNullOrEmptyCollection(problems, "version", metadata.getVersion(), null, level);
                    validateNullOrEmptyCollection(problems, "versioning", metadata.getVersioning(), null, level);

                } else {
                    validateStringNotEmpty(problems, "groupId", metadata.getGroupId(), null, level);
                    validateStringNotEmpty(problems, "artifactId", metadata.getArtifactId(), null, level);
                    validateStringNotEmpty(problems, "version", metadata.getArtifactId(), null, level);
                    if (validateNotNull(problems, "versioning", metadata.getVersioning(), null, level)) {
                        for (SnapshotVersion version : metadata.getVersioning().getSnapshotVersions()) {
                            validateStringNotEmpty(
                                    problems,
                                    "versioning.snapshotVersions.snapshotVersion.extension",
                                    version.getExtension(),
                                    version.getVersion(),
                                    level);
                            validateStringNotEmpty(
                                    problems,
                                    "versioning.snapshotVersions.snapshotVersion.value",
                                    version.getVersion(),
                                    null,
                                    level);
                            // TODO validate updated timestamp?
                        }
                        validateNotNull(
                                problems,
                                "versioning.snapshotVersions",
                                metadata.getVersioning().getSnapshotVersions(),
                                null,
                                level);
                    }
                }
                validateNullOrEmptyCollection(problems, "plugins", metadata.getPlugins(), null, level);
                break;
        }
    }

    // ----------------------------------------------------------------------
    // Field validation
    // ----------------------------------------------------------------------

    /**
     * Asserts:
     * <p/>
     * <ul>
     * <li><code>string.length != null</code>
     * <li><code>string.length > 0</code>
     * </ul>
     */
    private static boolean validateStringNotEmpty(
            MetadataProblemCollector problems, String fieldName, String string, String sourceHint, Level level) {
        if (!validateNotNull(problems, fieldName, string, sourceHint, level)) {
            return false;
        }

        if (string.length() > 0) {
            return true;
        }

        addViolation(problems, Severity.ERROR, fieldName, sourceHint, level, "is missing");

        return false;
    }

    /**
     * Asserts:
     * <p/>
     * <ul>
     * <li><code>string != null</code>
     * </ul>
     */
    private static boolean validateNotNull(
            MetadataProblemCollector problems, String fieldName, Object object, String sourceHint, Level level) {
        if (object != null) {
            return true;
        }

        addViolation(problems, Severity.ERROR, fieldName, sourceHint, level, "is missing");

        return false;
    }

    /**
     * Asserts:
     * <p/>
     * <ul>
     * <li><code>string != null</code>
     * </ul>
     */
    private static boolean validateNullOrEmptyCollection(
            MetadataProblemCollector problems, String fieldName, Object object, String sourceHint, Level level) {
        if (object == null
                || (object instanceof Collection
                        && Collection.class.cast(object).isEmpty())) {
            return true;
        }

        addViolation(problems, Severity.WARNING, fieldName, sourceHint, level, "is unused");

        return false;
    }

    /**
     * Asserts:
     * <p/>
     * <ul>
     * <li><code>string != null</code>
     * </ul>
     */
    private static boolean validateNotEmptyCollection(
            MetadataProblemCollector problems,
            String fieldName,
            Collection<?> collection,
            String sourceHint,
            Level level) {
        if (!collection.isEmpty()) {
            return true;
        }

        addViolation(problems, Severity.WARNING, fieldName, sourceHint, level, "is empty collection");

        return false;
    }

    private static void addViolation(
            MetadataProblemCollector problems,
            Severity severity,
            String fieldName,
            String sourceHint,
            Level level,
            String message) {
        StringBuilder buffer = new StringBuilder(256);
        buffer.append('\'').append(fieldName).append('\'');

        if (sourceHint != null) {
            buffer.append(" for ").append(sourceHint);
        }
        if (level != null) {
            buffer.append(" on repository metadata level ").append(level);
        }
        buffer.append(' ').append(message);

        problems.add(severity, buffer.toString(), -1, -1, null);
    }
}
