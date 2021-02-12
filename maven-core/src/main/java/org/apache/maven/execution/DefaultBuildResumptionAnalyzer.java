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

import org.apache.maven.project.MavenProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import javax.inject.Singleton;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Default implementation of {@link BuildResumptionAnalyzer}.
 */
@Named
@Singleton
public class DefaultBuildResumptionAnalyzer implements BuildResumptionAnalyzer
{
    private static final Logger LOGGER = LoggerFactory.getLogger( DefaultBuildResumptionAnalyzer.class );

    @Override
    public Optional<BuildResumptionData> determineBuildResumptionData( final MavenExecutionResult result )
    {
        if ( !result.hasExceptions() )
        {
            return Optional.empty();
        }

        List<MavenProject> sortedProjects = result.getTopologicallySortedProjects();

        long succeeded = sortedProjects.stream()
                .filter( project -> result.getBuildSummary( project ) instanceof BuildSuccess )
                .count();

        if ( succeeded == 0 )
        {
            return Optional.empty();
        }

        List<MavenProject> notSucceededProjects = sortedProjects.stream()
                .filter( project -> result.getBuildSummary( project ) == null
                        || result.getBuildSummary( project ) instanceof BuildFailure )
                .collect( Collectors.toList() );
        // When a project is selected to be built, all children projects will be automatically
        // added {@see org.apache.maven.graph.DefaultGraphBuilder#trimSelectedProjects}
        // This loop will remove all children from projects in this list
        while ( true )
        {
            List<MavenProject> children = notSucceededProjects.stream()
                    .filter( project -> project.getCollectedProjects() != null )
                    .flatMap( project -> project.getCollectedProjects().stream() )
                    .collect( Collectors.toList() );
            if ( !notSucceededProjects.removeAll( children ) )
            {
                // if we haven't removed any child project, we're good to go so exit this loop
                break;
            }
        }

        if ( notSucceededProjects.isEmpty() )
        {
            LOGGER.info( "No failed projects found, resuming the build would not make sense." );
            return Optional.empty();
        }

        List<String> projectList = notSucceededProjects.stream()
                .map( project -> project.getGroupId() + ":" + project.getArtifactId() )
                .collect( Collectors.toList() );

        return Optional.of( new BuildResumptionData( projectList ) );
    }

}
