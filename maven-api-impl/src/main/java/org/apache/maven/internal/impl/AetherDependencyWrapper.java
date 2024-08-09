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

import org.apache.maven.api.DependencyScope;
import org.apache.maven.api.Type;
import org.apache.maven.api.annotations.Nonnull;
import org.eclipse.aether.artifact.ArtifactProperties;
import org.eclipse.aether.graph.Dependency;

/**
 * Base class of {@code Dependency} or {@code DependencyCoordinate} implementations as a wrapper around
 * an Eclipse Aether object. This class implements the methods that are common to {@code Dependency} and
 * {@code DependencyCoordinate}, even if this class does not implement directly any of those interfaces.
 * Having matching method signatures is sufficient, even if there is no {@code @Override} annotations.
 *
 * <p>The fact that this class is wrapping an Eclipse Aether object is an implementation details that may
 * change in any future Maven version. For now, one purpose of this class is to have a single type to check
 * for unwrapping the Eclipse Aether object.</p>
 */
abstract class AetherDependencyWrapper {
    /**
     * The session to install / deploy / resolve artifacts and dependencies.
     */
    final InternalSession session;

    /**
     * The wrapped Eclipse Aether dependency.
     */
    final Dependency dependency;

    /**
     * Creates a new wrapper for the given dependency.
     *
     * @param dependency the Eclipse Aether dependency to wrap
     */
    AetherDependencyWrapper(@Nonnull InternalSession session, @Nonnull Dependency dependency) {
        this.session = Objects.requireNonNull(session, "session");
        this.dependency = Objects.requireNonNull(dependency, "dependency");
    }

    /**
     * {@return the group identifier of the wrapped dependency}.
     * The default implementation delegates to the Eclipse Aether artifact.
     */
    public String getGroupId() {
        return dependency.getArtifact().getGroupId();
    }

    /**
     * {@return the artifact identifier of the wrapped dependency}.
     * The default implementation delegates to the Eclipse Aether artifact.
     */
    public String getArtifactId() {
        return dependency.getArtifact().getArtifactId();
    }

    /**
     * {@return the file extension of the wrapped dependency}.
     * The default implementation delegates to the Eclipse Aether artifact.
     */
    public String getExtension() {
        return dependency.getArtifact().getExtension();
    }

    /**
     * {@return the type of the wrapped dependency}.
     * The default implementation infers the type from the properties associated to the Eclipse Aether artifact.
     */
    public Type getType() {
        String type = dependency.getArtifact().getProperty(ArtifactProperties.TYPE, getExtension());
        return session.requireType(type);
    }

    /**
     * {@return the classifier ("jar", "test-jar", …) of the wrapped dependency}.
     * The default implementation first delegates to the Eclipse Aether artifact.
     * If the latter does not provide a non-empty classifier,
     * then the default value is determined by {@linkplain #getType() type}.
     */
    @Nonnull
    public String getClassifier() {
        String classifier = dependency.getArtifact().getClassifier();
        if (classifier.isEmpty()) {
            classifier = getType().getClassifier();
            if (classifier == null) {
                classifier = "";
            }
        }
        return classifier;
    }

    /**
     * {@return the scope (compile, test, …) of this dependency}.
     */
    @Nonnull
    public DependencyScope getScope() {
        return session.requireDependencyScope(dependency.getScope());
    }

    /**
     * {@return a string representation of this dependency}.
     * This is for debugging purposes only and may change in any future version.
     */
    @Override
    public String toString() {
        return dependency.toString();
    }
}
