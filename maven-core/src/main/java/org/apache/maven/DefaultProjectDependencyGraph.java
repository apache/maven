package org.apache.maven;

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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.apache.maven.execution.ProjectDependencyGraph;
import org.apache.maven.project.DuplicateProjectException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectSorter;
import org.codehaus.plexus.util.dag.CycleDetectedException;

/**
 * Describes the inter-dependencies between projects in the reactor.
 * 
 * @author Benjamin Bentmann
 */
class DefaultProjectDependencyGraph
    implements ProjectDependencyGraph
{

    private ProjectSorter sorter;

    /**
     * Creates a new project dependency graph based on the specified projects.
     * 
     * @param projects The projects to create the dependency graph with
     * @throws DuplicateProjectException 
     * @throws CycleDetectedException 
     */
    public DefaultProjectDependencyGraph( Collection<MavenProject> projects ) throws CycleDetectedException, DuplicateProjectException
    {
        this.sorter = new ProjectSorter( projects );
    }

    public List<MavenProject> getSortedProjects()
    {
        return new ArrayList<MavenProject>( sorter.getSortedProjects() );
    }

    public List<MavenProject> getDownstreamProjects( MavenProject project, boolean transitive )
    {
        if ( project == null )
        {
            throw new IllegalArgumentException( "project missing" );
        }

        Collection<String> projectIds = new HashSet<String>();

        getDownstreamProjects( ProjectSorter.getId( project ), projectIds, transitive );

        return getProjects( projectIds );
    }

    private void getDownstreamProjects( String projectId, Collection<String> projectIds, boolean transitive )
    {
        for ( String id : sorter.getDependents( projectId ) )
        {
            if ( projectIds.add( id ) && transitive )
            {
                getDownstreamProjects( id, projectIds, transitive );
            }
        }
    }

    public List<MavenProject> getUpstreamProjects( MavenProject project, boolean transitive )
    {
        if ( project == null )
        {
            throw new IllegalArgumentException( "project missing" );
        }

        Collection<String> projectIds = new HashSet<String>();

        getUpstreamProjects( ProjectSorter.getId( project ), projectIds, transitive );

        return getProjects( projectIds );
    }

    private void getUpstreamProjects( String projectId, Collection<String> projectIds, boolean transitive )
    {
        for ( String id : sorter.getDependencies( projectId ) )
        {
            if ( projectIds.add( id ) && transitive )
            {
                getUpstreamProjects( id, projectIds, transitive );
            }
        }
    }

    private List<MavenProject> getProjects( Collection<String> projectIds )
    {
        List<MavenProject> projects = new ArrayList<MavenProject>( projectIds.size() );

        for ( String projectId : projectIds )
        {
            MavenProject project = sorter.getProjectMap().get( projectId );

            if ( project != null )
            {
                projects.add( project );
            }
        }

        return projects;
    }

    @Override
    public String toString()
    {
        return sorter.getSortedProjects().toString();
    }

}
