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

import java.util.Objects;
import java.util.function.Predicate;

import org.apache.maven.api.LifecyclePlan;
import org.apache.maven.api.MojoExecution;
import org.apache.maven.api.Session;
import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Immutable;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.annotations.NotThreadSafe;
import org.apache.maven.api.annotations.Nullable;

import static java.util.Objects.requireNonNull;

/**
 * A request to execute all or part of a previously calculated lifecycle plan.
 *
 * @since 4.1.0
 */
@Experimental
@Immutable
public interface LifecycleExecutionRequest extends Request<Session> {

    /** {@return the previously calculated lifecycle plan to execute} */
    @Nonnull
    LifecyclePlan plan();

    /** {@return the optional predicate used to select executions from the plan} */
    @Nullable
    Predicate<MojoExecution> filter();

    @Nonnull
    static Builder builder() {
        return new Builder();
    }

    @Nonnull
    static LifecycleExecutionRequest build(@Nonnull Session session, @Nonnull LifecyclePlan plan) {
        return builder().session(session).plan(plan).build();
    }

    @Nonnull
    static LifecycleExecutionRequest build(
            @Nonnull Session session, @Nonnull LifecyclePlan plan, @Nonnull Predicate<MojoExecution> filter) {
        return builder().session(session).plan(plan).filter(filter).build();
    }

    @NotThreadSafe
    class Builder {
        private Session session;
        private RequestTrace trace;
        private LifecyclePlan plan;
        private Predicate<MojoExecution> filter;

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
        public Builder plan(@Nonnull LifecyclePlan plan) {
            this.plan = plan;
            return this;
        }

        @Nonnull
        public Builder filter(@Nullable Predicate<MojoExecution> filter) {
            this.filter = filter;
            return this;
        }

        @Nonnull
        public LifecycleExecutionRequest build() {
            return new DefaultLifecycleExecutionRequest(session, trace, plan, filter);
        }

        private static final class DefaultLifecycleExecutionRequest extends BaseRequest<Session>
                implements LifecycleExecutionRequest {
            private final LifecyclePlan plan;
            private final Predicate<MojoExecution> filter;

            private DefaultLifecycleExecutionRequest(
                    Session session, RequestTrace trace, LifecyclePlan plan, Predicate<MojoExecution> filter) {
                super(requireNonNull(session, "session cannot be null"), trace);
                this.plan = requireNonNull(plan, "plan cannot be null");
                this.filter = filter;
            }

            @Override
            public LifecyclePlan plan() {
                return plan;
            }

            @Override
            public Predicate<MojoExecution> filter() {
                return filter;
            }

            @Override
            public boolean equals(Object obj) {
                return obj instanceof DefaultLifecycleExecutionRequest that && Objects.equals(plan, that.plan);
            }

            @Override
            public int hashCode() {
                return Objects.hash(plan);
            }

            @Override
            public String toString() {
                return "LifecycleExecutionRequest[plan=" + plan + ']';
            }
        }
    }
}
