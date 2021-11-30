package org.apache.maven.caching;

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

import org.apache.maven.caching.xml.build.ProjectsInputInfo;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;

import static java.util.Objects.requireNonNull;

/**
 * CacheContext
 */
public class CacheContext
{
    private final MavenProject project;
    private final ProjectsInputInfo inputInfo;
    private final MavenSession session;

    public CacheContext( MavenProject project, ProjectsInputInfo inputInfo, MavenSession session )
    {
        this.project = requireNonNull( project );
        this.inputInfo = requireNonNull( inputInfo );
        this.session = requireNonNull( session );
    }

    public MavenProject getProject()
    {
        return project;
    }

    public ProjectsInputInfo getInputInfo()
    {
        return inputInfo;
    }

    public MavenSession getSession()
    {
        return session;
    }
}
