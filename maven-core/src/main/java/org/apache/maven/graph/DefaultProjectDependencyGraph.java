package org.apache.maven.graph;

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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

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
public class DefaultProjectDependencyGraph
    implements ProjectDependencyGraph
{

    private ProjectSorter sorter;

    private List<MavenProject> allProjects;

    /**
     * Creates a new project dependency graph based on the specified projects.
     *
     * @param projects The projects to create the dependency graph with
     * @throws DuplicateProjectException
     * @throws CycleDetectedException
     */
    public DefaultProjectDependencyGraph( Collection<MavenProject> projects )
        throws CycleDetectedException, DuplicateProjectException
    {
        super();
        this.allProjects = Collections.unmodifiableList( new ArrayList<>( projects ) );
        this.sorter = new ProjectSorter( projects );
    }

    /**
     * Creates a new project dependency graph based on the specified projects.
     *
     * @param allProjects All collected projects.
     * @param projects The projects to create the dependency graph with.
     *
     * @throws DuplicateProjectException
     * @throws CycleDetectedException
     * @since 3.5.0
     */
    public DefaultProjectDependencyGraph( final List<MavenProject> allProjects,
                                          final Collection<MavenProject> projects )
        throws CycleDetectedException, DuplicateProjectException
    {
        super();
        this.allProjects = Collections.unmodifiableList( new ArrayList<>( allProjects ) );
        this.sorter = new ProjectSorter( projects );
    }

    /**
     * @since 3.5.0
     */
    public List<MavenProject> getAllProjects()
    {
        return this.allProjects;
    }

    public List<MavenProject> getSortedProjects()
    {
        return new ArrayList<>( sorter.getSortedProjects() );
    }

    public List<MavenProject> getDownstreamProjects( MavenProject project, boolean transitive )
    {
        Objects.requireNonNull( project, "project cannot be null" );

        Set<String> projectIds = new HashSet<>();

        getDownstreamProjects( ProjectSorter.getId( project ), projectIds, transitive );

        return getSortedProjects( projectIds );
    }

    private void getDownstreamProjects( String projectId, Set<String> projectIds, boolean transitive )
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
        Objects.requireNonNull( project, "project cannot be null" );

        Set<String> projectIds = new HashSet<>();

        getUpstreamProjects( ProjectSorter.getId( project ), projectIds, transitive );

        return getSortedProjects( projectIds );
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

    private List<MavenProject> getSortedProjects( Set<String> projectIds )
    {
        List<MavenProject> result = new ArrayList<>( projectIds.size() );

        for ( MavenProject mavenProject : sorter.getSortedProjects() )
        {
            if ( projectIds.contains( ProjectSorter.getId( mavenProject ) ) )
            {
                result.add( mavenProject );
            }
        }

        return result;
    }

    @Override
    public String toString()
    {
        return sorter.getSortedProjects().toString();
    }

}
