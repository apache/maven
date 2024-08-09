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

import java.util.Collection;

import org.apache.maven.api.DependencyCoordinate;
import org.apache.maven.api.Exclusion;
import org.apache.maven.api.VersionConstraint;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.annotations.Nullable;

public class DefaultDependencyCoordinate extends AetherDependencyWrapper implements DependencyCoordinate {
    public DefaultDependencyCoordinate(
            @Nonnull InternalSession session, @Nonnull org.eclipse.aether.graph.Dependency dependency) {
        super(session, dependency);
    }

    @Override
    public VersionConstraint getVersion() {
        return session.parseVersionConstraint(dependency.getArtifact().getVersion());
    }

    @Nullable
    @Override
    public Boolean getOptional() {
        return dependency.getOptional();
    }

    @Nonnull
    @Override
    public Collection<Exclusion> getExclusions() {
        return new MappedCollection<>(dependency.getExclusions(), this::toExclusion);
    }

    private Exclusion toExclusion(org.eclipse.aether.graph.Exclusion exclusion) {
        return new Exclusion() {
            @Nullable
            @Override
            public String getGroupId() {
                return exclusion.getGroupId();
            }

            @Nullable
            @Override
            public String getArtifactId() {
                return exclusion.getArtifactId();
            }
        };
    }
}
