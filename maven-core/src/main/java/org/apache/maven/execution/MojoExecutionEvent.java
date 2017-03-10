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

import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;

/**
 * <p>
 * Encapsulates parameters of MojoExecutionListener callback methods and is meant to provide API evolution path should
 * it become necessary to introduce new parameters in the existing callbacks in the future.
 * </p>
 * <strong>Note:</strong> This class is part of work in progress and can be changed or removed without notice.
 *
 * @see MojoExecutionListener
 * @see org.apache.maven.execution.scope.WeakMojoExecutionListener
 * @since 3.1.2
 */
public class MojoExecutionEvent
{
    private final MavenSession session;

    private final MavenProject project;

    private final MojoExecution mojoExecution;

    private final Mojo mojo;

    private final Throwable cause;

    public MojoExecutionEvent( MavenSession session, MavenProject project, MojoExecution mojoExecution, Mojo mojo )
    {
        this( session, project, mojoExecution, mojo, null );
    }

    public MojoExecutionEvent( MavenSession session, MavenProject project, MojoExecution mojoExecution, Mojo mojo,
                               Throwable cause )
    {
        this.session = session;
        this.project = project;
        this.mojoExecution = mojoExecution;
        this.mojo = mojo;
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

    public MojoExecution getExecution()
    {
        return mojoExecution;
    }

    public Mojo getMojo()
    {
        return mojo;
    }

    public Throwable getCause()
    {
        return cause;
    }
}
