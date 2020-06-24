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

import org.apache.maven.lifecycle.LifecycleExecutionException;
import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import javax.inject.Singleton;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Comparator.comparing;

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
        final List<MavenProject> failedProjects = getFailedProjectsInOrder( result );

        if ( failedProjects.isEmpty() )
        {
            LOGGER.info( "No failed projects found, resuming the build would not make sense." );
            return Optional.empty();
        }

        final MavenProject resumeFromProject = failedProjects.get( 0 );

        final String resumeFromSelector;
        final List<String> projectsToSkip;
        if ( isFailedProjectFirstInBuild( result, resumeFromProject ) )
        {
            // As the first module in the build failed, there is no need to specify this as the resumeFrom project.
            resumeFromSelector = null;
            projectsToSkip = determineProjectsToSkip( result, failedProjects, 0 );
        }
        else
        {
            resumeFromSelector = resumeFromProject.getGroupId() + ":" + resumeFromProject.getArtifactId();
            List<MavenProject> allProjects = result.getTopologicallySortedProjects();
            int resumeFromProjectIndex = allProjects.indexOf( resumeFromProject );
            projectsToSkip = determineProjectsToSkip( result, failedProjects, resumeFromProjectIndex + 1 );
        }

        boolean canBuildBeResumed = StringUtils.isNotEmpty( resumeFromSelector ) || !projectsToSkip.isEmpty();
        if ( canBuildBeResumed )
        {
            return Optional.of( new BuildResumptionData( resumeFromSelector, projectsToSkip ) );
        }
        else
        {
            return Optional.empty();
        }
    }

    private boolean isFailedProjectFirstInBuild( final MavenExecutionResult result, final MavenProject failedProject )
    {
        final List<MavenProject> sortedProjects = result.getTopologicallySortedProjects();
        return sortedProjects.indexOf( failedProject ) == 0;
    }

    private List<MavenProject> getFailedProjectsInOrder( MavenExecutionResult result )
    {
        List<MavenProject> sortedProjects = result.getTopologicallySortedProjects();

        return result.getExceptions().stream()
                .filter( LifecycleExecutionException.class::isInstance )
                .map( LifecycleExecutionException.class::cast )
                .map( LifecycleExecutionException::getProject )
                .filter( Objects::nonNull )
                .sorted( comparing( sortedProjects::indexOf ) )
                .collect( Collectors.toList() );
    }

    /**
     * Projects after the first failed project could have succeeded by using -T or --fail-at-end.
     * These projects can be skipped from later builds.
     * This is not the case these projects are dependent on one of the failed projects.
     * @param result The result of the Maven build.
     * @param failedProjects The list of failed projects in the build.
     * @param startFromProjectIndex Start looking for projects which can be skipped from a certain index.
     * @return A list of projects which can be skipped in a later build.
     */
    private List<String> determineProjectsToSkip( MavenExecutionResult result,
                                                  List<MavenProject> failedProjects,
                                                  int startFromProjectIndex )
    {
        List<MavenProject> allProjects = result.getTopologicallySortedProjects();
        List<MavenProject> remainingProjects = allProjects.subList( startFromProjectIndex, allProjects.size() );

        List<GroupArtifactPair> failedProjectsGAList = failedProjects.stream()
                .map( GroupArtifactPair::new )
                .collect( Collectors.toList() );

        return remainingProjects.stream()
                .filter( project -> result.getBuildSummary( project ) instanceof BuildSuccess )
                .filter( project -> hasNoDependencyOnProjects( project, failedProjectsGAList ) )
                .map( project -> project.getGroupId() + ":" + project.getArtifactId() )
                .collect( Collectors.toList() );
    }

    private boolean hasNoDependencyOnProjects( MavenProject project, List<GroupArtifactPair> projectsGAs )
    {
        return project.getDependencies().stream()
                .map( GroupArtifactPair::new )
                .noneMatch( projectsGAs::contains );
    }

    private static class GroupArtifactPair
    {
        private final String groupId;
        private final String artifactId;

        GroupArtifactPair( MavenProject project )
        {
            this.groupId = project.getGroupId();
            this.artifactId = project.getArtifactId();
        }

        GroupArtifactPair( Dependency dependency )
        {
            this.groupId = dependency.getGroupId();
            this.artifactId = dependency.getArtifactId();
        }

        @Override
        public boolean equals( Object o )
        {
            if ( this == o )
            {
                return true;
            }
            if ( o == null || getClass() != o.getClass() )
            {
                return false;
            }
            GroupArtifactPair that = (GroupArtifactPair) o;
            return Objects.equals( groupId, that.groupId ) && Objects.equals( artifactId, that.artifactId );
        }

        @Override
        public int hashCode()
        {
            return Objects.hash( groupId, artifactId );
        }
    }
}
