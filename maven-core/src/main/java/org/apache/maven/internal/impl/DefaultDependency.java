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
package org.apache.maven.internal.impl;

import java.util.Objects;

import org.apache.maven.api.Artifact;
import org.apache.maven.api.Dependency;
import org.apache.maven.api.DependencyCoordinate;
import org.apache.maven.api.DependencyProperties;
import org.apache.maven.api.Scope;
import org.apache.maven.api.Type;
import org.apache.maven.api.Version;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.annotations.Nullable;
import org.apache.maven.api.services.TypeRegistry;
import org.apache.maven.repository.internal.DefaultModelVersionParser;
import org.eclipse.aether.artifact.ArtifactProperties;

import static org.apache.maven.internal.impl.Utils.nonNull;

public class DefaultDependency implements Dependency {
    private final InternalSession session;
    private final org.eclipse.aether.graph.Dependency dependency;
    private final DependencyProperties dependencyProperties;
    private final String key;

    public DefaultDependency(
            @Nonnull InternalSession session, @Nonnull org.eclipse.aether.graph.Dependency dependency) {
        this.session = nonNull(session, "session");
        this.dependency = nonNull(dependency, "dependency");
        this.dependencyProperties =
                new DefaultDependencyProperties(dependency.getArtifact().getProperties());
        this.key = getGroupId()
                + ':'
                + getArtifactId()
                + ':'
                + getExtension()
                + (!getClassifier().isEmpty() ? ":" + getClassifier() : "")
                + ':'
                + getVersion();
    }

    @Override
    public String key() {
        return key;
    }

    @Nonnull
    public org.eclipse.aether.graph.Dependency getDependency() {
        return dependency;
    }

    @Override
    public String getGroupId() {
        return dependency.getArtifact().getGroupId();
    }

    @Override
    public String getArtifactId() {
        return dependency.getArtifact().getArtifactId();
    }

    @Override
    public String getClassifier() {
        return dependency.getArtifact().getClassifier();
    }

    @Override
    public Version getVersion() {
        return session.parseVersion(dependency.getArtifact().getVersion());
    }

    @Override
    public Version getBaseVersion() {
        return session.parseVersion(dependency.getArtifact().getBaseVersion());
    }

    @Override
    public String getExtension() {
        return dependency.getArtifact().getExtension();
    }

    @Override
    public Type getType() {
        String type = dependency
                .getArtifact()
                .getProperty(ArtifactProperties.TYPE, dependency.getArtifact().getExtension());
        return session.requireType(type);
    }

    @Override
    public DependencyProperties getDependencyProperties() {
        return dependencyProperties;
    }

    @Override
    public boolean isSnapshot() {
        return DefaultModelVersionParser.checkSnapshot(dependency.getArtifact().getVersion());
    }

    @Nonnull
    @Override
    public Scope getScope() {
        return Scope.get(dependency.getScope());
    }

    @Nullable
    @Override
    public boolean isOptional() {
        return dependency.isOptional();
    }

    @Nonnull
    @Override
    public DependencyCoordinate toCoordinate() {
        return session.createDependencyCoordinate(this);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Artifact && Objects.equals(key(), ((Artifact) o).key());
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }

    @Override
    public String toString() {
        return dependency.toString();
    }
}
