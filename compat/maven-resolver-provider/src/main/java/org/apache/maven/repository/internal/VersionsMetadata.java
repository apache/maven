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
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashSet;

import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.Versioning;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.ArtifactProperties;

/**
 * Maven GA level metadata.
 *
 * @deprecated since 4.0.0, use {@code maven-api-impl} jar instead
 */
@Deprecated(since = "4.0.0")
final class VersionsMetadata extends MavenMetadata {

    private final Artifact artifact;

    VersionsMetadata(Artifact artifact, Date timestamp) {
        super(createRepositoryMetadata(artifact), (Path) null, timestamp);
        this.artifact = artifact;
    }

    VersionsMetadata(Artifact artifact, Path path, Date timestamp) {
        super(createRepositoryMetadata(artifact), path, timestamp);
        this.artifact = artifact;
    }

    private static Metadata createRepositoryMetadata(Artifact artifact) {
        Metadata metadata = new Metadata();
        metadata.setGroupId(artifact.getGroupId());
        metadata.setArtifactId(artifact.getArtifactId());

        Versioning versioning = new Versioning();
        versioning.addVersion(artifact.getBaseVersion());
        if (!artifact.isSnapshot()) {
            versioning.setRelease(artifact.getBaseVersion());
        }
        if ("maven-plugin".equals(artifact.getProperty(ArtifactProperties.TYPE, ""))) {
            versioning.setLatest(artifact.getBaseVersion());
        }

        metadata.setVersioning(versioning);

        return metadata;
    }

    @Override
    protected void merge(Metadata recessive) {
        Versioning versioning = metadata.getVersioning();
        versioning.setLastUpdatedTimestamp(timestamp);

        if (recessive.getVersioning() != null) {
            if (versioning.getLatest() == null) {
                versioning.setLatest(recessive.getVersioning().getLatest());
            }
            if (versioning.getRelease() == null) {
                versioning.setRelease(recessive.getVersioning().getRelease());
            }

            Collection<String> versions =
                    new LinkedHashSet<>(recessive.getVersioning().getVersions());
            versions.addAll(versioning.getVersions());
            versioning.setVersions(new ArrayList<>(versions));
        }

        // just carry-on as-is
        if (!recessive.getPlugins().isEmpty()) {
            metadata.setPlugins(new ArrayList<>(recessive.getPlugins()));
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
