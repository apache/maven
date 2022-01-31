package org.apache.maven.project.collector;

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

import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.model.building.ModelProblem;
import org.apache.maven.model.building.ModelProblemUtils;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.project.ProjectBuildingResult;
import org.codehaus.plexus.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility to select projects for a given set of pom.xml files.
 */
@Named
@Singleton
public class DefaultProjectsSelector implements ProjectsSelector
{
    private static final Logger LOGGER = LoggerFactory.getLogger( DefaultProjectsSelector.class );
    private final ProjectBuilder projectBuilder;

    @Inject
    public DefaultProjectsSelector( ProjectBuilder projectBuilder )
    {
        this.projectBuilder = projectBuilder;
    }

    @Override
    public List<MavenProject> selectProjects( List<File> files, MavenExecutionRequest request )
            throws ProjectBuildingException
    {
        ProjectBuildingRequest projectBuildingRequest = request.getProjectBuildingRequest();

        boolean hasProjectSelection = !request.getProjectActivation().isEmpty();
        boolean isRecursive = hasProjectSelection || request.isRecursive();
        List<ProjectBuildingResult> results = projectBuilder.build( files, isRecursive, projectBuildingRequest );

        List<MavenProject> projects = new ArrayList<>( results.size() );

        boolean problems = false;

        for ( ProjectBuildingResult result : results )
        {
            projects.add( result.getProject() );

            if ( !result.getProblems().isEmpty() && LOGGER.isWarnEnabled() )
            {
                LOGGER.warn( "" );
                LOGGER.warn( "Some problems were encountered while building the effective model for '{}'",
                        result.getProject().getId() );

                for ( ModelProblem problem : result.getProblems() )
                {
                    String loc = ModelProblemUtils.formatLocation( problem, result.getProjectId() );
                    LOGGER.warn( "{}{}", problem.getMessage(), ( StringUtils.isNotEmpty( loc ) ? " @ " + loc : "" ) );
                }

                problems = true;
            }
        }

        if ( problems )
        {
            LOGGER.warn( "" );
            LOGGER.warn( "It is highly recommended to fix these problems"
                    + " because they threaten the stability of your build." );
            LOGGER.warn( "" );
            LOGGER.warn( "For this reason, future Maven versions might no"
                    + " longer support building such malformed projects." );
            LOGGER.warn( "" );
        }

        return projects;
    }
}
