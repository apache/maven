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
package org.apache.maven.repository.internal;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TimeZone;

import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.Snapshot;
import org.apache.maven.artifact.repository.metadata.SnapshotVersion;
import org.apache.maven.artifact.repository.metadata.Versioning;
import org.eclipse.aether.artifact.Artifact;

/**
 * Maven remote GAV level metadata.
 */
final class RemoteSnapshotMetadata extends MavenSnapshotMetadata {
    public static final String DEFAULT_SNAPSHOT_TIMESTAMP_FORMAT = "yyyyMMdd.HHmmss";

    public static final TimeZone DEFAULT_SNAPSHOT_TIME_ZONE = TimeZone.getTimeZone("Etc/UTC");

    private final Map<String, SnapshotVersion> versions = new LinkedHashMap<>();

    private final Integer buildNumber;

    RemoteSnapshotMetadata(Artifact artifact, boolean legacyFormat, Date timestamp, Integer buildNumber) {
        super(createRepositoryMetadata(artifact, legacyFormat), null, legacyFormat, timestamp);
        this.buildNumber = buildNumber;
    }

    private RemoteSnapshotMetadata(
            Metadata metadata, File file, boolean legacyFormat, Date timestamp, Integer buildNumber) {
        super(metadata, file, legacyFormat, timestamp);
        this.buildNumber = buildNumber;
    }

    @Override
    public MavenMetadata setFile(File file) {
        return new RemoteSnapshotMetadata(metadata, file, legacyFormat, timestamp, buildNumber);
    }

    public String getExpandedVersion(Artifact artifact) {
        String key = getKey(artifact.getClassifier(), artifact.getExtension());
        return versions.get(key).getVersion();
    }

    @Override
    protected void merge(Metadata recessive) {
        Snapshot snapshot;
        String lastUpdated;

        if (metadata.getVersioning() == null) {
            DateFormat utcDateFormatter = new SimpleDateFormat(DEFAULT_SNAPSHOT_TIMESTAMP_FORMAT);
            utcDateFormatter.setCalendar(new GregorianCalendar());
            utcDateFormatter.setTimeZone(DEFAULT_SNAPSHOT_TIME_ZONE);

            snapshot = new Snapshot();
            snapshot.setBuildNumber(buildNumber != null ? buildNumber : getBuildNumber(recessive) + 1);
            snapshot.setTimestamp(utcDateFormatter.format(timestamp));

            Versioning versioning = new Versioning();
            versioning.setSnapshot(snapshot);
            versioning.setLastUpdatedTimestamp(timestamp);
            lastUpdated = versioning.getLastUpdated();

            metadata.setVersioning(versioning);
        } else {
            snapshot = metadata.getVersioning().getSnapshot();
            lastUpdated = metadata.getVersioning().getLastUpdated();
        }

        for (Artifact artifact : artifacts) {
            String version = artifact.getVersion();

            if (version.endsWith(SNAPSHOT)) {
                String qualifier = snapshot.getTimestamp() + '-' + snapshot.getBuildNumber();
                version = version.substring(0, version.length() - SNAPSHOT.length()) + qualifier;
            }

            SnapshotVersion sv = new SnapshotVersion();
            sv.setClassifier(artifact.getClassifier());
            sv.setExtension(artifact.getExtension());
            sv.setVersion(version);
            sv.setUpdated(lastUpdated);

            versions.put(getKey(sv.getClassifier(), sv.getExtension()), sv);
        }

        artifacts.clear();

        Versioning versioning = recessive.getVersioning();
        if (versioning != null) {
            for (SnapshotVersion sv : versioning.getSnapshotVersions()) {
                String key = getKey(sv.getClassifier(), sv.getExtension());
                if (!versions.containsKey(key)) {
                    versions.put(key, sv);
                }
            }
        }

        if (!legacyFormat) {
            metadata.getVersioning().setSnapshotVersions(new ArrayList<>(versions.values()));
        }
        // just carry-on as-is
        if (!recessive.getPlugins().isEmpty()) {
            metadata.setPlugins(new ArrayList<>(recessive.getPlugins()));
        }
    }

    private static int getBuildNumber(Metadata metadata) {
        int number = 0;

        Versioning versioning = metadata.getVersioning();
        if (versioning != null) {
            Snapshot snapshot = versioning.getSnapshot();
            if (snapshot != null && snapshot.getBuildNumber() > 0) {
                number = snapshot.getBuildNumber();
            }
        }

        return number;
    }
}
