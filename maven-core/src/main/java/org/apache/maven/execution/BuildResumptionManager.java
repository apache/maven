package org.apache.maven.execution;

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

import org.apache.maven.project.MavenProject;

import java.util.List;

/**
 * This class describes most of the logic needed for the --resume / -r feature. Its goal is to ensure newer
 * builds of the same project that have the -r command-line flag skip successfully built projects during earlier
 * invocations of Maven.
 */
public interface BuildResumptionManager
{
    /**
     * Persists any data needed to resume the build at a later point in time, using a new Maven invocation. This method
     * may also decide it is not needed or meaningful to persist such data, and return <code>false</code> to indicate
     * so.
     *
     * @param result The result of the current Maven invocation.
     * @param rootProject The root project that is being built.
     * @return Whether any data was persisted.
     */
    boolean persistResumptionData( final MavenExecutionResult result, final MavenProject rootProject );

    /**
     * Uses previously stored resumption data to enrich an existing execution request.
     * @param request The execution request that will be enriched.
     * @param rootProject The root project that is being built.
     */
    void applyResumptionData( final MavenExecutionRequest request, final MavenProject rootProject );

    /**
     * Removes previously stored resumption data.
     * @param rootProject The root project that is being built.
     */
    void removeResumptionData( final MavenProject rootProject );

    /**
     * A helper method to determine the value to resume the build with {@code -rf} taking into account the edge case
     *   where multiple modules in the reactor have the same artifactId.
     * <p>
     * {@code -rf :artifactId} will pick up the first module which matches, but when multiple modules in the reactor
     *   have the same artifactId, effective failed module might be later in build reactor.
     * This means that developer will either have to type groupId or wait for build execution of all modules which
     *   were fine, but they are still before one which reported errors.
     * <p>Then the returned value is {@code groupId:artifactId} when there is a name clash and
     * {@code :artifactId} if there is no conflict.
     *
     * @param mavenProjects Maven projects which are part of build execution.
     * @param failedProject Project which has failed.
     * @return Value for -rf flag to resume build exactly from place where it failed ({@code :artifactId} in general
     * and {@code groupId:artifactId} when there is a name clash).
     */
    String getResumeFromSelector( final List<MavenProject> mavenProjects, final MavenProject failedProject );
}
