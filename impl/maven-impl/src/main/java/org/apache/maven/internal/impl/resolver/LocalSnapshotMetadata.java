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
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.maven.api.metadata.Metadata;
import org.apache.maven.api.metadata.Snapshot;
import org.apache.maven.api.metadata.SnapshotVersion;
import org.apache.maven.api.metadata.Versioning;
import org.eclipse.aether.artifact.Artifact;

/**
 * Maven local GAV level metadata.
 */
final class LocalSnapshotMetadata extends MavenMetadata {

    private final Collection<Artifact> artifacts = new ArrayList<>();

    LocalSnapshotMetadata(Artifact artifact, Instant timestamp) {
        super(createMetadata(artifact), (Path) null, timestamp);
    }

    LocalSnapshotMetadata(Metadata metadata, Path path, Instant timestamp) {
        super(metadata, path, timestamp);
    }

    private static Metadata createMetadata(Artifact artifact) {
        Snapshot snapshot = Snapshot.newBuilder().localCopy(true).build();
        Versioning versioning = Versioning.newBuilder().snapshot(snapshot).build();
        Metadata metadata = Metadata.newBuilder()
                .versioning(versioning)
                .groupId(artifact.getGroupId())
                .artifactId(artifact.getArtifactId())
                .version(artifact.getBaseVersion())
                .modelVersion("1.1.0")
                .build();
        return metadata;
    }

    public void bind(Artifact artifact) {
        artifacts.add(artifact);
    }

    @Deprecated
    @Override
    public MavenMetadata setFile(File file) {
        return new LocalSnapshotMetadata(metadata, file.toPath(), timestamp);
    }

    @Override
    public MavenMetadata setPath(Path path) {
        return new LocalSnapshotMetadata(metadata, path, timestamp);
    }

    public Object getKey() {
        return getGroupId() + ':' + getArtifactId() + ':' + getVersion();
    }

    public static Object getKey(Artifact artifact) {
        return artifact.getGroupId() + ':' + artifact.getArtifactId() + ':' + artifact.getBaseVersion();
    }

    @Override
    protected void merge(Metadata recessive) {
        metadata = metadata.withVersioning(metadata.getVersioning().withLastUpdated(fmt.format(timestamp)));

        String lastUpdated = metadata.getVersioning().getLastUpdated();

        Map<String, SnapshotVersion> versions = new LinkedHashMap<>();

        for (Artifact artifact : artifacts) {
            SnapshotVersion sv = SnapshotVersion.newBuilder()
                    .classifier(artifact.getClassifier())
                    .extension(artifact.getExtension())
                    .version(getVersion())
                    .updated(lastUpdated)
                    .build();
            versions.put(getKey(sv.getClassifier(), sv.getExtension()), sv);
        }

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

        artifacts.clear();
    }

    private String getKey(String classifier, String extension) {
        return classifier + ':' + extension;
    }

    @Override
    public String getGroupId() {
        return metadata.getGroupId();
    }

    @Override
    public String getArtifactId() {
        return metadata.getArtifactId();
    }

    @Override
    public String getVersion() {
        return metadata.getVersion();
    }

    @Override
    public Nature getNature() {
        return Nature.SNAPSHOT;
    }
}
