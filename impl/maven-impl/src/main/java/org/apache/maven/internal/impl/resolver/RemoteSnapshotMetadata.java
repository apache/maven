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
package org.apache.maven.internal.impl.resolver;

import java.io.File;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.maven.api.metadata.Metadata;
import org.apache.maven.api.metadata.Snapshot;
import org.apache.maven.api.metadata.SnapshotVersion;
import org.apache.maven.api.metadata.Versioning;
import org.eclipse.aether.artifact.Artifact;

/**
 * Maven remote GAV level metadata.
 */
final class RemoteSnapshotMetadata extends MavenSnapshotMetadata {
    public static final String DEFAULT_SNAPSHOT_TIMESTAMP_FORMAT = "yyyyMMdd.HHmmss";

    private final Map<String, SnapshotVersion> versions = new LinkedHashMap<>();

    private final Integer buildNumber;

    RemoteSnapshotMetadata(Artifact artifact, Instant timestamp, Integer buildNumber) {
        super(createRepositoryMetadata(artifact), null, timestamp);
        this.buildNumber = buildNumber;
    }

    private RemoteSnapshotMetadata(Metadata metadata, Path path, Instant timestamp, Integer buildNumber) {
        super(metadata, path, timestamp);
        this.buildNumber = buildNumber;
    }

    @Deprecated
    @Override
    public MavenMetadata setFile(File file) {
        return new RemoteSnapshotMetadata(metadata, file.toPath(), timestamp, buildNumber);
    }

    @Override
    public MavenMetadata setPath(Path path) {
        return new RemoteSnapshotMetadata(metadata, path, timestamp, buildNumber);
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
            DateTimeFormatter utcDateFormatter = DateTimeFormatter.ofPattern(DEFAULT_SNAPSHOT_TIMESTAMP_FORMAT);

            snapshot = Snapshot.newBuilder()
                    .buildNumber(buildNumber != null ? buildNumber : getBuildNumber(recessive) + 1)
                    .timestamp(utcDateFormatter.format(this.timestamp.atZone(ZoneOffset.UTC)))
                    .build();

            lastUpdated = fmt.format(timestamp);
            Versioning versioning = Versioning.newBuilder()
                    .snapshot(snapshot)
                    .lastUpdated(lastUpdated)
                    .build();

            metadata = metadata.withVersioning(versioning);
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

            SnapshotVersion sv = SnapshotVersion.newBuilder()
                    .classifier(artifact.getClassifier())
                    .extension(artifact.getExtension())
                    .version(version)
                    .updated(lastUpdated)
                    .build();

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

        metadata = metadata.withVersioning(metadata.getVersioning().withSnapshotVersions(versions.values()));

        // just carry-on as-is
        if (recessive.getPlugins() != null && !recessive.getPlugins().isEmpty()) {
            metadata = metadata.withPlugins(new ArrayList<>(recessive.getPlugins()));
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
