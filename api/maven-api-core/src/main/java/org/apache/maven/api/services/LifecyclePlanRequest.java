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
package org.apache.maven.api.services;

import java.util.List;
import java.util.Objects;

import org.apache.maven.api.Project;
import org.apache.maven.api.Session;
import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Immutable;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.annotations.NotThreadSafe;
import org.apache.maven.api.annotations.Nullable;

import static java.util.Objects.requireNonNull;

/**
 * A request to calculate a lifecycle plan for one project.
 *
 * @since 4.1.0
 */
@Experimental
@Immutable
public interface LifecyclePlanRequest extends Request<Session> {

    /** {@return the project for which the lifecycle plan is calculated} */
    @Nonnull
    Project project();

    /** {@return the lifecycle phases and plugin goals used to calculate the plan} */
    @Nonnull
    List<String> tasks();

    @Nonnull
    static Builder builder() {
        return new Builder();
    }

    @Nonnull
    static LifecyclePlanRequest build(@Nonnull Session session, @Nonnull Project project, @Nonnull List<String> tasks) {
        return builder().session(session).project(project).tasks(tasks).build();
    }

    @NotThreadSafe
    class Builder {
        private Session session;
        private RequestTrace trace;
        private Project project;
        private List<String> tasks;

        Builder() {}

        @Nonnull
        public Builder session(@Nonnull Session session) {
            this.session = session;
            return this;
        }

        @Nonnull
        public Builder trace(@Nullable RequestTrace trace) {
            this.trace = trace;
            return this;
        }

        @Nonnull
        public Builder project(@Nonnull Project project) {
            this.project = project;
            return this;
        }

        @Nonnull
        public Builder tasks(@Nonnull List<String> tasks) {
            this.tasks = tasks;
            return this;
        }

        @Nonnull
        public LifecyclePlanRequest build() {
            return new DefaultLifecyclePlanRequest(session, trace, project, tasks);
        }

        private static final class DefaultLifecyclePlanRequest extends BaseRequest<Session>
                implements LifecyclePlanRequest {
            private final Project project;
            private final List<String> tasks;

            private DefaultLifecyclePlanRequest(
                    Session session, RequestTrace trace, Project project, List<String> tasks) {
                super(requireNonNull(session, "session cannot be null"), trace);
                this.project = requireNonNull(project, "project cannot be null");
                this.tasks = List.copyOf(requireNonNull(tasks, "tasks cannot be null"));
                this.tasks.forEach(task -> requireNonNull(task, "task cannot be null"));
            }

            @Override
            public Project project() {
                return project;
            }

            @Override
            public List<String> tasks() {
                return tasks;
            }

            @Override
            public boolean equals(Object obj) {
                return obj instanceof DefaultLifecyclePlanRequest that
                        && Objects.equals(project, that.project)
                        && Objects.equals(tasks, that.tasks);
            }

            @Override
            public int hashCode() {
                return Objects.hash(project, tasks);
            }

            @Override
            public String toString() {
                return "LifecyclePlanRequest[project=" + project + ", tasks=" + tasks + ']';
            }
        }
    }
}
