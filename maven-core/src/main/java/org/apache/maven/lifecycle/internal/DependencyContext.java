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

import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.MavenExecutionPlan;
import org.apache.maven.project.MavenProject;

import java.util.Collection;

/**
 * Context of dependency artifacts for the entire build.
 *
 * @author Benjamin Bentmann
 * @author Kristian Rosenvold (class extract only)
 *         <p/>
 *         NOTE: This class is not part of any public api and can be changed or deleted without prior notice.
 */
// TODO: From a concurrency perspective, this class is not good. The combination of mutable/immutable state is not nice
public class DependencyContext
{

    private final Collection<String> scopesToCollect;

    private final Collection<String> scopesToResolve;

    private final boolean aggregating;

    private volatile MavenProject lastProject;

    private volatile Collection<?> lastDependencyArtifacts;

    private volatile int lastDependencyArtifactCount;

    public DependencyContext( Collection<String> scopesToCollect, Collection<String> scopesToResolve,
                              boolean aggregating )
    {
        this.scopesToCollect = scopesToCollect;
        this.scopesToResolve = scopesToResolve;
        this.aggregating = aggregating;
    }

    public DependencyContext( MavenExecutionPlan executionPlan, boolean aggregating )
    {
        this( executionPlan.getRequiredCollectionScopes(), executionPlan.getRequiredResolutionScopes(), aggregating );
    }

    public void setLastDependencyArtifacts( Collection<?> lastDependencyArtifacts )
    {
        this.lastDependencyArtifacts = lastDependencyArtifacts;
        lastDependencyArtifactCount = ( lastDependencyArtifacts != null ) ? lastDependencyArtifacts.size() : 0;
    }

    public MavenProject getLastProject()
    {
        return lastProject;
    }

    public void setLastProject( MavenProject lastProject )
    {
        this.lastProject = lastProject;
    }

    public Collection<String> getScopesToCollect()
    {
        return scopesToCollect;
    }

    public Collection<String> getScopesToResolve()
    {
        return scopesToResolve;
    }

    public boolean isAggregating()
    {
        return aggregating;
    }

    public DependencyContext clone()
    {
        return new DependencyContext( scopesToCollect, scopesToResolve, aggregating );
    }

    public boolean isSameProject( MavenSession session )
    {
        return ( lastProject == session.getCurrentProject() );
    }

    public boolean isSameButUpdatedProject( MavenSession session )
    {
        if ( isSameProject( session ) )
        {
            if ( lastDependencyArtifacts != lastProject.getDependencyArtifacts()
                || ( lastDependencyArtifacts != null && lastDependencyArtifactCount != lastDependencyArtifacts.size() ) )
            {
                return true;

            }
        }
        return false;
    }

}
