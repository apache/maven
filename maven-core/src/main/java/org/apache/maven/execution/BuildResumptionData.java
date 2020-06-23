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
     * List of projects to skip if the build would be resumed from {@link #resumeFrom}.
     */
    private final List<String> projectsToSkip;

    public BuildResumptionData ( final String resumeFrom, final List<String> projectsToSkip )
    {
        this.resumeFrom = resumeFrom;
        this.projectsToSkip = projectsToSkip;
    }

    public String getResumeFrom()
    {
        return this.resumeFrom;
    }

    public List<String> getProjectsToSkip()
    {
        return this.projectsToSkip;
    }
}
