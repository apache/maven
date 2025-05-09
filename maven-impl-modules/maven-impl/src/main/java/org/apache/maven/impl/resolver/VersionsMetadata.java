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

import java.io.File;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

import org.apache.maven.api.metadata.Metadata;
import org.apache.maven.api.metadata.Versioning;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.ArtifactProperties;

/**
 * Maven GA level metadata.
 */
final class VersionsMetadata extends MavenMetadata {

    private final Artifact artifact;

    VersionsMetadata(Artifact artifact, Instant timestamp) {
        super(createRepositoryMetadata(artifact), (Path) null, timestamp);
        this.artifact = artifact;
    }

    VersionsMetadata(Artifact artifact, Path path, Instant timestamp) {
        super(createRepositoryMetadata(artifact), path, timestamp);
        this.artifact = artifact;
    }

    private static Metadata createRepositoryMetadata(Artifact artifact) {

        Metadata.Builder metadata = Metadata.newBuilder();
        metadata.groupId(artifact.getGroupId());
        metadata.artifactId(artifact.getArtifactId());

        Versioning.Builder versioning = Versioning.newBuilder();
        versioning.versions(List.of(artifact.getBaseVersion()));
        if (!artifact.isSnapshot()) {
            versioning.release(artifact.getBaseVersion());
        }
        if ("maven-plugin".equals(artifact.getProperty(ArtifactProperties.TYPE, ""))) {
            versioning.latest(artifact.getBaseVersion());
        }

        metadata.versioning(versioning.build());

        return metadata.build();
    }

    @Override
    protected void merge(Metadata recessive) {
        Versioning original = metadata.getVersioning();

        Versioning.Builder versioning = Versioning.newBuilder(original);
        versioning.lastUpdated(fmt.format(timestamp));

        if (recessive.getVersioning() != null) {
            if (original.getLatest() == null) {
                versioning.latest(recessive.getVersioning().getLatest());
            }
            if (original.getRelease() == null) {
                versioning.release(recessive.getVersioning().getRelease());
            }

            Collection<String> versions =
                    new LinkedHashSet<>(recessive.getVersioning().getVersions());
            versions.addAll(original.getVersions());
            versioning.versions(new ArrayList<>(versions));
        }

        metadata = metadata.withVersioning(versioning.build());

        // just carry-on as-is
        if (recessive.getPlugins() != null && !recessive.getPlugins().isEmpty()) {
            metadata = metadata.withPlugins(new ArrayList<>(recessive.getPlugins()));
        }
    }

    public Object getKey() {
        return getGroupId() + ':' + getArtifactId();
    }

    public static Object getKey(Artifact artifact) {
        return artifact.getGroupId() + ':' + artifact.getArtifactId();
    }

    @Deprecated
    @Override
    public MavenMetadata setFile(File file) {
        return new VersionsMetadata(artifact, file.toPath(), timestamp);
    }

    @Override
    public MavenMetadata setPath(Path path) {
        return new VersionsMetadata(artifact, path, timestamp);
    }

    @Override
    public String getGroupId() {
        return artifact.getGroupId();
    }

    @Override
    public String getArtifactId() {
        return artifact.getArtifactId();
    }

    @Override
    public String getVersion() {
        return "";
    }

    @Override
    public Nature getNature() {
        return artifact.isSnapshot() ? Nature.RELEASE_OR_SNAPSHOT : Nature.RELEASE;
    }
}
