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

import org.apache.maven.api.ArtifactCoordinates;
import org.apache.maven.api.VersionConstraint;
import org.apache.maven.api.annotations.Nonnull;
import org.eclipse.aether.artifact.Artifact;

import static org.apache.maven.impl.ImplUtils.nonNull;

/**
 * A wrapper class around a maven resolver artifact.
 */
public class DefaultArtifactCoordinates implements ArtifactCoordinates {
    private final @Nonnull InternalSession session;
    private final @Nonnull Artifact coordinates;

    public DefaultArtifactCoordinates(@Nonnull InternalSession session, @Nonnull Artifact coordinates) {
        this.session = nonNull(session, "session");
        this.coordinates = nonNull(coordinates, "coordinates");
    }

    public Artifact getCoordinates() {
        return coordinates;
    }

    @Nonnull
    @Override
    public String getGroupId() {
        return coordinates.getGroupId();
    }

    @Nonnull
    @Override
    public String getArtifactId() {
        return coordinates.getArtifactId();
    }

    @Nonnull
    @Override
    public VersionConstraint getVersionConstraint() {
        return session.parseVersionConstraint(coordinates.getVersion());
    }

    @Override
    public String getExtension() {
        return coordinates.getExtension();
    }

    @Nonnull
    @Override
    public String getClassifier() {
        return coordinates.getClassifier();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DefaultArtifactCoordinates that = (DefaultArtifactCoordinates) o;
        return Objects.equals(this.getGroupId(), that.getGroupId())
                && Objects.equals(this.getArtifactId(), that.getArtifactId())
                && Objects.equals(this.getVersionConstraint(), that.getVersionConstraint())
                && Objects.equals(this.getClassifier(), that.getClassifier());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getGroupId(), getArtifactId(), getVersionConstraint(), getClassifier());
    }

    @Override
    public String toString() {
        return coordinates.toString();
    }
}
