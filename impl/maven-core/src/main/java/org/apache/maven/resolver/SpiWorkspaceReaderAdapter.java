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
package org.apache.maven.resolver;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.apache.maven.api.ArtifactCoordinates;
import org.apache.maven.api.Version;
import org.apache.maven.api.annotations.Nonnull;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.repository.WorkspaceReader;
import org.eclipse.aether.repository.WorkspaceRepository;

/**
 * Bridge adapter that wraps an {@link org.apache.maven.api.spi.WorkspaceReader SPI WorkspaceReader}
 * into a resolver {@link WorkspaceReader}.
 *
 * <p>This adapter translates between resolver {@link Artifact} and Maven API
 * {@link org.apache.maven.api.Artifact} types, allowing SPI implementations to work
 * with Maven 4 API types exclusively while being integrated into the resolver's
 * workspace reader chain.
 *
 * @since 4.1.0
 */
public class SpiWorkspaceReaderAdapter implements WorkspaceReader {

    private final org.apache.maven.api.spi.WorkspaceReader delegate;
    private final WorkspaceRepository repository;

    public SpiWorkspaceReaderAdapter(org.apache.maven.api.spi.WorkspaceReader delegate) {
        this.delegate = delegate;
        this.repository = new WorkspaceRepository("spi-" + delegate.getClass().getSimpleName());
    }

    @Override
    public WorkspaceRepository getRepository() {
        return repository;
    }

    @Override
    public File findArtifact(Artifact artifact) {
        Optional<Path> result = delegate.findArtifact(toApiArtifact(artifact));
        return result.map(Path::toFile).orElse(null);
    }

    @Override
    public List<String> findVersions(Artifact artifact) {
        return delegate.findVersions(toApiArtifact(artifact));
    }

    /**
     * Whether the underlying SPI reader should participate in plugin resolution.
     *
     * @return {@code true} if applicable for plugin resolution
     */
    public boolean isApplicableForPluginResolution() {
        return delegate.isApplicableForPluginResolution();
    }

    /**
     * Returns the underlying SPI workspace reader.
     */
    public org.apache.maven.api.spi.WorkspaceReader getDelegate() {
        return delegate;
    }

    /**
     * Creates a lightweight Maven API {@link org.apache.maven.api.Artifact} from a resolver artifact
     * without requiring an active session.
     */
    private static org.apache.maven.api.Artifact toApiArtifact(Artifact artifact) {
        return new LightweightApiArtifact(artifact);
    }

    /**
     * A lightweight implementation of {@link org.apache.maven.api.Artifact} that wraps a resolver artifact
     * for the purpose of passing artifact coordinates to SPI workspace readers.
     */
    private static class LightweightApiArtifact implements org.apache.maven.api.Artifact {
        private final Artifact artifact;
        private final String key;

        LightweightApiArtifact(Artifact artifact) {
            this.artifact = artifact;
            this.key = getGroupId()
                    + ':'
                    + getArtifactId()
                    + ':'
                    + getExtension()
                    + (getClassifier().isEmpty() ? "" : ":" + getClassifier())
                    + ':'
                    + artifact.getVersion();
        }

        @Override
        public String key() {
            return key;
        }

        @Nonnull
        @Override
        public String getGroupId() {
            return artifact.getGroupId();
        }

        @Nonnull
        @Override
        public String getArtifactId() {
            return artifact.getArtifactId();
        }

        @Nonnull
        @Override
        public Version getVersion() {
            return new StringVersion(artifact.getVersion());
        }

        @Nonnull
        @Override
        public Version getBaseVersion() {
            return new StringVersion(artifact.getBaseVersion());
        }

        @Nonnull
        @Override
        public String getExtension() {
            return artifact.getExtension();
        }

        @Nonnull
        @Override
        public String getClassifier() {
            return artifact.getClassifier();
        }

        @Override
        public boolean isSnapshot() {
            return artifact.isSnapshot();
        }

        @Nonnull
        @Override
        public ArtifactCoordinates toCoordinates() {
            throw new UnsupportedOperationException("Lightweight artifact wrapper does not support toCoordinates(); "
                    + "use Session.createArtifactCoordinates() instead");
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof org.apache.maven.api.Artifact a && key.equals(a.key());
        }

        @Override
        public int hashCode() {
            return key.hashCode();
        }

        @Override
        public String toString() {
            return key;
        }
    }

    /**
     * Simple {@link Version} implementation that wraps a version string.
     */
    private record StringVersion(String version) implements Version {
        @Override
        public int compareTo(Version o) {
            return version.compareTo(o.toString());
        }

        @Override
        public String toString() {
            return version;
        }
    }
}
