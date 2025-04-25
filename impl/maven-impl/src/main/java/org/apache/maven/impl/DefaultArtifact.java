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
package org.apache.maven.impl;

import java.util.Objects;

import org.apache.maven.api.Artifact;
import org.apache.maven.api.ArtifactCoordinates;
import org.apache.maven.api.Version;
import org.apache.maven.api.annotations.Nonnull;

/**
 * Default implementation of the Artifact interface.
 * Wraps an Aether artifact and provides access to its properties.
 */
public class DefaultArtifact implements Artifact {
    /**
     * The session used for artifact operations.
     */
    @Nonnull
    protected final InternalSession session;

    /**
     * The wrapped Aether artifact.
     */
    @Nonnull
    protected final org.eclipse.aether.artifact.Artifact artifact;

    /**
     * The unique key for this artifact.
     */
    @Nonnull
    protected final String key;

    public DefaultArtifact(@Nonnull InternalSession session, @Nonnull org.eclipse.aether.artifact.Artifact artifact) {
        this.session = Objects.requireNonNull(session, "session cannot be null");
        this.artifact = Objects.requireNonNull(artifact, "artifact cannot be null");
        this.key = getGroupId()
                + ':'
                + getArtifactId()
                + ':'
                + getExtension()
                + (getClassifier().isEmpty() ? "" : ":" + getClassifier())
                + ':'
                + getVersion();
    }

    public org.eclipse.aether.artifact.Artifact getArtifact() {
        return artifact;
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
        return session.parseVersion(artifact.getVersion());
    }

    @Override
    public Version getBaseVersion() {
        return session.parseVersion(artifact.getBaseVersion());
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
        return DefaultModelVersionParser.checkSnapshot(artifact.getVersion());
    }

    @Nonnull
    @Override
    public ArtifactCoordinates toCoordinates() {
        return session.createArtifactCoordinates(this);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Artifact artifact && Objects.equals(key(), artifact.key());
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }

    @Override
    public String toString() {
        return artifact.toString();
    }
}
