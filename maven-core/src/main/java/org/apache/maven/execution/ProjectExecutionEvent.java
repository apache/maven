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

import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;

/**
 * <p>
 * Encapsulates parameters of ProjectExecutionListener callback methods and is meant to provide API evolution path
 * should it become necessary to introduce new parameters in the existing callbacks in the future.
 * </p>
 * <strong>Note:</strong> This class is part of work in progress and can be changed or removed without notice.
 *
 * @see ProjectExecutionListener
 * @since 3.1.2
 */
public class ProjectExecutionEvent
{

    private final MavenSession session;

    private final MavenProject project;

    private final List<MojoExecution> executionPlan;

    private final Throwable cause;

    public ProjectExecutionEvent( MavenSession session, MavenProject project )
    {
        this( session, project, null, null );
    }

    public ProjectExecutionEvent( MavenSession session, MavenProject project, List<MojoExecution> executionPlan )
    {
        this( session, project, executionPlan, null );
    }

    public ProjectExecutionEvent( MavenSession session, MavenProject project, Throwable cause )
    {
        this( session, project, null, cause );
    }

    public ProjectExecutionEvent( MavenSession session, MavenProject project, List<MojoExecution> executionPlan,
                                  Throwable cause )
    {
        this.session = session;
        this.project = project;
        this.executionPlan = executionPlan;
        this.cause = cause;
    }

    public MavenSession getSession()
    {
        return session;
    }

    public MavenProject getProject()
    {
        return project;
    }

    public List<MojoExecution> getExecutionPlan()
    {
        return executionPlan;
    }

    public Throwable getCause()
    {
        return cause;
    }

}
