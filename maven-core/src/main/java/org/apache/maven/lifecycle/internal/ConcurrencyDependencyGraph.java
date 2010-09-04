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

import org.apache.maven.execution.ProjectDependencyGraph;
import org.apache.maven.project.MavenProject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Presents a view of the Dependency Graph that is suited for concurrent building.
 * 
 * @since 3.0
 * @author Kristian Rosenvold
 *         <p/>
 *         NOTE: This class is not part of any public api and can be changed or deleted without prior notice.
 */
public class ConcurrencyDependencyGraph
{

    private final ProjectBuildList projectBuilds;

    private final ProjectDependencyGraph projectDependencyGraph;

    private final HashSet<MavenProject> finishedProjects = new HashSet<MavenProject>();


    public ConcurrencyDependencyGraph( ProjectBuildList projectBuilds, ProjectDependencyGraph projectDependencyGraph )
    {
        this.projectDependencyGraph = projectDependencyGraph;
        this.projectBuilds = projectBuilds;
    }


    public int getNumberOfBuilds()
    {
        return projectBuilds.size();
    }

    /**
     * Gets all the builds that have no reactor-dependencies
     *
     * @return A list of all the initial builds
     */

    public List<MavenProject> getRootSchedulableBuilds()
    {
        List<MavenProject> result = new ArrayList<MavenProject>();
        for ( ProjectSegment projectBuild : projectBuilds )
        {
            if ( projectDependencyGraph.getUpstreamProjects( projectBuild.getProject(), false ).size() == 0 )
            {
                result.add( projectBuild.getProject() );
            }
        }
        return result;
    }

    /**
     * Marks the provided project as finished. Returns a list of
     *
     * @param mavenProject The project
     * @return The list of builds that are eligible for starting now that the provided project is done
     */
    public List<MavenProject> markAsFinished( MavenProject mavenProject )
    {
        finishedProjects.add( mavenProject );
        return getSchedulableNewProcesses( mavenProject );
    }

    private List<MavenProject> getSchedulableNewProcesses( MavenProject finishedProject )
    {
        List<MavenProject> result = new ArrayList<MavenProject>();
        // schedule dependent projects, if all of their requirements are met
        for ( MavenProject dependentProject : projectDependencyGraph.getDownstreamProjects( finishedProject, false ) )
        {
            final List<MavenProject> upstreamProjects =
                projectDependencyGraph.getUpstreamProjects( dependentProject, false );
            if ( finishedProjects.containsAll( upstreamProjects ) )
            {
                result.add( dependentProject );
            }
        }
        return result;
    }

    public ProjectBuildList getProjectBuilds()
    {
        return projectBuilds;
    }
}