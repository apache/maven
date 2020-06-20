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
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.util.List;

import org.apache.maven.project.DependencyResolutionResult;
import org.apache.maven.project.MavenProject;

/**
 * @author Jason van Zyl
 */
public interface MavenExecutionResult
{
    MavenExecutionResult setProject( MavenProject project );
    MavenProject getProject();

    MavenExecutionResult setTopologicallySortedProjects( List<MavenProject> projects );

    /**
     * @return the sorted list, or an empty list if there are no projects.
     */
    List<MavenProject> getTopologicallySortedProjects();

    MavenExecutionResult setDependencyResolutionResult( DependencyResolutionResult result );
    DependencyResolutionResult getDependencyResolutionResult();

    // for each exception
    // - knowing what artifacts are missing
    // - project building exception
    // - invalid project model exception: list of markers
    // - xmlpull parser exception
    List<Throwable> getExceptions();

    MavenExecutionResult addException( Throwable e );

    boolean hasExceptions();

    /**
     * Gets the build summary for the specified project.
     *
     * @param project The project to get the build summary for, must not be {@code null}.
     * @return The build summary for the project or {@code null} if the project has not been built (yet).
     */
    BuildSummary getBuildSummary( MavenProject project );

    /**
     * Add the specified build summary.
     *
     * @param summary The build summary to add, must not be {@code null}.
     */
    void addBuildSummary( BuildSummary summary );

    /**
     * Indicates whether or not the build could be resumed by a second invocation of Maven.
     * @see BuildResumptionDataRepository
     * @return <code>true</code> when it is possible to resume the build, <code>false</code> otherwise.
     */
    boolean canResume();

    /**
     * Indicate that the build can or cannot be resumed by a second invocation of Maven.
     * @param canResume <code>true</code> when it is possible to resume the build, <code>false</code> otherwise.
     * @see BuildResumptionDataRepository
     * @see #canResume()
     */
    void setCanResume( boolean canResume );
}
