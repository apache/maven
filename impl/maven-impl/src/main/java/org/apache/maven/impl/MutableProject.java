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

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.UnaryOperator;

import org.apache.maven.api.Project;
import org.apache.maven.api.annotations.Nonnull;

import static java.util.Objects.requireNonNull;

/**
 * Internal stable identity for a project whose state is represented by immutable snapshots.
 */
public final class MutableProject {
    private final AtomicReference<Project> project;

    public MutableProject(@Nonnull Project project) {
        this.project = new AtomicReference<>(requireNonNull(project, "project cannot be null"));
    }

    @Nonnull
    public Project project() {
        return project.get();
    }

    @Nonnull
    public Project update(@Nonnull UnaryOperator<Project> updater) {
        requireNonNull(updater, "updater cannot be null");
        return project.updateAndGet(
                current -> requireNonNull(updater.apply(current), "updated project cannot be null"));
    }
}
