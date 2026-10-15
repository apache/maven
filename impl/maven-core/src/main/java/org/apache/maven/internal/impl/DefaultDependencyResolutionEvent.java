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

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.apache.maven.api.Dependency;
import org.apache.maven.api.DependencyResolutionEvent;
import org.apache.maven.api.DependencyResolutionEventType;
import org.apache.maven.api.Project;
import org.apache.maven.api.Session;
import org.apache.maven.project.DependencyResolutionResult;

/**
 * Maven 4 view of a dependency resolution, created from the legacy request and result.
 */
class DefaultDependencyResolutionEvent implements DependencyResolutionEvent {
    private final InternalMavenSession session;
    private final Project project;
    private final DependencyResolutionEventType type;
    private final List<Dependency> resolved;
    private final List<Dependency> unresolved;
    private final Exception exception;

    /** Creates the event fired before the dependencies of the project are resolved. */
    DefaultDependencyResolutionEvent(InternalMavenSession session, Project project) {
        this.session = Objects.requireNonNull(session, "session");
        this.project = Objects.requireNonNull(project, "project");
        this.type = DependencyResolutionEventType.STARTED;
        this.resolved = List.of();
        this.unresolved = List.of();
        this.exception = null;
    }

    /** Creates the event fired once the dependencies of the project have been resolved or have failed to. */
    DefaultDependencyResolutionEvent(InternalMavenSession session, Project project, DependencyResolutionResult result) {
        this.session = Objects.requireNonNull(session, "session");
        this.project = Objects.requireNonNull(project, "project");
        this.resolved = result.getResolvedDependencies().stream()
                .map(session::getDependency)
                .toList();
        this.unresolved = result.getUnresolvedDependencies().stream()
                .map(session::getDependency)
                .toList();
        Exception failure = null;
        if (!result.getCollectionErrors().isEmpty()) {
            failure = result.getCollectionErrors().get(0);
        } else {
            for (org.eclipse.aether.graph.Dependency dependency : result.getUnresolvedDependencies()) {
                List<Exception> errors = result.getResolutionErrors(dependency);
                if (errors != null && !errors.isEmpty()) {
                    failure = errors.get(0);
                    break;
                }
            }
        }
        this.exception = failure;
        this.type = failure != null || !unresolved.isEmpty()
                ? DependencyResolutionEventType.FAILED
                : DependencyResolutionEventType.SUCCEEDED;
    }

    @Override
    public DependencyResolutionEventType type() {
        return type;
    }

    @Override
    public Session session() {
        return session;
    }

    @Override
    public Project project() {
        return project;
    }

    @Override
    public List<Dependency> resolvedDependencies() {
        return resolved;
    }

    @Override
    public List<Dependency> unresolvedDependencies() {
        return unresolved;
    }

    @Override
    public Optional<Exception> exception() {
        return Optional.ofNullable(exception);
    }
}
