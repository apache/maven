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

        boolean hasNoSuccess = sortedProjects.stream()
                .noneMatch( project -> result.getBuildSummary( project ) instanceof BuildSuccess );

        if ( hasNoSuccess )
        {
            return Optional.empty();
        }

        List<String> remainingProjects = sortedProjects.stream()
                .filter( project -> result.getBuildSummary( project ) == null
                        || result.getBuildSummary( project ) instanceof BuildFailure )
                .map( project -> project.getGroupId() + ":" + project.getArtifactId() )
                .collect( Collectors.toList() );

        if ( remainingProjects.isEmpty() )
        {
            LOGGER.info( "No remaining projects found, resuming the build would not make sense." );
            return Optional.empty();
        }

        return Optional.of( new BuildResumptionData( remainingProjects ) );
    }

}
