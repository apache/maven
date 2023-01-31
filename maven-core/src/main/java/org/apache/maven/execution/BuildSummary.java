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
package org.apache.maven.execution;

import java.util.Objects;

import org.apache.maven.project.MavenProject;

/**
 * Summarizes the result of a project build in the reactor.
 *
 * @author Benjamin Bentmann
 */
public abstract class BuildSummary {

    /**
     * The project being summarized.
     */
    private final MavenProject project;

    /**
     * The build time of the project in milliseconds.
     */
    private final long time;

    /**
     * Creates a new build summary for the specified project.
     *
     * @param project The project being summarized, must not be {@code null}.
     * @param time The build time of the project in milliseconds.
     */
    protected BuildSummary(MavenProject project, long time) {
        this.project = Objects.requireNonNull(project, "project cannot be null");
        // TODO Validate for < 0?
        this.time = time;
    }

    /**
     * Gets the project being summarized.
     *
     * @return The project being summarized, never {@code null}.
     */
    public MavenProject getProject() {
        return project;
    }

    /**
     * Gets the build time of the project in milliseconds.
     *
     * @return The build time of the project in milliseconds.
     */
    public long getTime() {
        return time;
    }
}
