package org.apache.maven.lifecycle.internal;

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

import java.util.Collection;
import java.util.List;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.execution.ProjectExecutionListener;
import org.apache.maven.lifecycle.LifecycleExecutionException;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;

class CompoundProjectExecutionListener
    implements ProjectExecutionListener
{
    private final Collection<ProjectExecutionListener> listeners;

    public CompoundProjectExecutionListener( Collection<ProjectExecutionListener> listeners )
    {
        this.listeners = listeners; // NB this is live injected collection
    }

    public void beforeProjectExecution( MavenSession session, MavenProject project )
        throws LifecycleExecutionException
    {
        for ( ProjectExecutionListener listener : listeners )
        {
            listener.beforeProjectExecution( session, project );
        }
    }

    public void beforeProjectLifecycleExecution( MavenSession session, MavenProject project,
                                                 List<MojoExecution> executionPlan )
        throws LifecycleExecutionException
    {
        for ( ProjectExecutionListener listener : listeners )
        {
            listener.beforeProjectLifecycleExecution( session, project, executionPlan );
        }
    }

    public void afterProjectExecutionSuccess( MavenSession session, MavenProject project )
        throws LifecycleExecutionException
    {
        for ( ProjectExecutionListener listener : listeners )
        {
            listener.afterProjectExecutionSuccess( session, project );
        }
    }

    public void afterProjectExecutionFailure( MavenSession session, MavenProject project, Throwable cause )
    {
        for ( ProjectExecutionListener listener : listeners )
        {
            listener.afterProjectExecutionFailure( session, project, cause );
        }
    }
}
