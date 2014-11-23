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

/**
 * Summarizes the result of a failed project build in the reactor.
 *
 * @author Benjamin Bentmann
 */
public class BuildFailure
    extends BuildSummary
{

    /**
     * The cause of the build failure.
     */
    private final Throwable cause;

    /**
     * Creates a new build summary for the specified project.
     *
     * @param project The project being summarized, must not be {@code null}.
     * @param time The build time of the project in milliseconds.
     * @param cause The cause of the build failure, may be {@code null}.
     */
    public BuildFailure( MavenProject project, long time, Throwable cause )
    {
        super( project, time );
        this.cause = cause;
    }

    /**
     * Gets the cause of the build failure.
     *
     * @return The cause of the build failure or {@code null} if unknown.
     */
    public Throwable getCause()
    {
        return cause;
    }

}
