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

import java.util.List;
import java.util.Optional;

import static java.util.Collections.emptyList;

/**
 * This class holds the information required to enable resuming a Maven build with {@code --resume}.
 */
public class BuildResumptionData
{
    /**
     * The project where the next build could resume from.
     */
    private final String resumeFrom;

    /**
     * List of projects to skip.
     */
    private final List<String> projectsToSkip;

    public BuildResumptionData ( final String resumeFrom, final List<String> projectsToSkip )
    {
        this.resumeFrom = resumeFrom;
        this.projectsToSkip = projectsToSkip;
    }

    /**
     * Returns the project where the next build can resume from.
     * This is usually the first failed project in the order of the reactor.
     * @return An optional containing the group and artifact id of the project. It does not make sense to resume
     *   the build when the first project of the reactor has failed, so then it will return an empty optional.
     */
    public Optional<String> getResumeFrom()
    {
        return Optional.ofNullable( this.resumeFrom );
    }

    /**
     * A list of projects which can be skipped in the next build.
     * @return A list of group and artifact ids. Can be empty when no projects can be skipped.
     */
    public List<String> getProjectsToSkip()
    {
        return ( projectsToSkip != null ) ? projectsToSkip : emptyList();
    }
}
