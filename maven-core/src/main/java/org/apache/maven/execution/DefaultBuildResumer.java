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

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.lifecycle.LifecycleExecutionException;
import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import javax.inject.Singleton;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;

import static java.util.Comparator.comparing;

/**
 * This implementation of {@link BuildResumer} persists information in a properties file. The file is stored
 * in the build output directory under the Maven execution root.
 */
@Named
@Singleton
public class DefaultBuildResumer implements BuildResumer
{
    private static final String RESUME_PROPERTIES_FILENAME = "resume.properties";
    private static final String RESUME_FROM_PROPERTY = "resumeFrom";
    private static final String EXCLUDED_PROJECTS_PROPERTY = "excludedProjects";
    private static final String PROPERTY_DELIMITER = ", ";
    private static final Logger LOGGER = LoggerFactory.getLogger( DefaultBuildResumer.class );

    @Override
    public boolean persistResumptionData( MavenExecutionResult result, MavenProject rootProject )
            throws BuildResumptionPersistenceException
    {
        Properties properties = determineResumptionProperties( result );

        if ( properties.isEmpty() )
        {
            LOGGER.debug( "Will not create {} file: nothing to resume from", RESUME_PROPERTIES_FILENAME );
            return false;
        }

        return writeResumptionFile( rootProject, properties );
    }

    @Override
    public void applyResumptionData( MavenExecutionRequest request, MavenProject rootProject )
    {
        Properties properties = loadResumptionFile( Paths.get( rootProject.getBuild().getDirectory() ) );
        applyResumptionProperties( request, properties );
    }

    @Override
    public void removeResumptionData( MavenProject rootProject )
    {
        Path resumeProperties = Paths.get( rootProject.getBuild().getDirectory(), RESUME_PROPERTIES_FILENAME );
        try
        {
            Files.deleteIfExists( resumeProperties );
        }
        catch ( IOException e )
        {
            LOGGER.warn( "Could not delete {} file. ", RESUME_PROPERTIES_FILENAME, e );
        }
    }

    @Override
    public String getResumeFromSelector( List<MavenProject> mavenProjects, MavenProject failedProject )
    {
        boolean hasOverlappingArtifactId = mavenProjects.stream()
                .filter( project -> failedProject.getArtifactId().equals( project.getArtifactId() ) )
                .count() > 1;

        if ( hasOverlappingArtifactId )
        {
            return failedProject.getGroupId() + ":" + failedProject.getArtifactId();
        }

        return ":" + failedProject.getArtifactId();
    }

    // This method is made package-private for testing purposes
    Properties determineResumptionProperties( MavenExecutionResult result )
    {
        Properties properties = new Properties();

        List<MavenProject> failedProjects = getFailedProjectsInOrder( result );
        if ( !failedProjects.isEmpty() )
        {
            MavenProject resumeFromProject = failedProjects.get( 0 );
            Optional<String> resumeFrom = getResumeFrom( result, resumeFromProject );
            Optional<String> projectsToSkip = determineProjectsToSkip( result, failedProjects, resumeFromProject );

            resumeFrom.ifPresent( value -> properties.setProperty( RESUME_FROM_PROPERTY, value ) );
            projectsToSkip.ifPresent( value -> properties.setProperty( EXCLUDED_PROJECTS_PROPERTY, value ) );
        }
        else
        {
            LOGGER.warn( "Could not create {} file: no failed projects found", RESUME_PROPERTIES_FILENAME );
        }

        return properties;
    }

    private List<MavenProject> getFailedProjectsInOrder( MavenExecutionResult result )
    {
        List<MavenProject> sortedProjects = result.getTopologicallySortedProjects();

        return result.getExceptions().stream()
                .filter( LifecycleExecutionException.class::isInstance )
                .map( LifecycleExecutionException.class::cast )
                .map( LifecycleExecutionException::getProject )
                .sorted( comparing( sortedProjects::indexOf ) )
                .collect( Collectors.toList() );
    }

    /**
     * Determine the project where the next build can be resumed from.
     * If the failed project is the first project of the build,
     * it does not make sense to use --resume-from, so the result will be empty.
     * @param result The result of the Maven build.
     * @param failedProject The first failed project of the build.
     * @return An optional containing the resume-from suggestion.
     */
    private Optional<String> getResumeFrom( MavenExecutionResult result, MavenProject failedProject )
    {
        List<MavenProject> allSortedProjects = result.getTopologicallySortedProjects();
        if ( !allSortedProjects.get( 0 ).equals( failedProject ) )
        {
            return Optional.of( failedProject.getGroupId() + ":" + failedProject.getArtifactId() );
        }

        return Optional.empty();
    }

    /**
     * Projects after the first failed project could have succeeded by using -T or --fail-at-end.
     * These projects can be skipped from later builds.
     * This is not the case these projects are dependent on one of the failed projects.
     * @param result The result of the Maven build.
     * @param failedProjects The list of failed projects in the build.
     * @param resumeFromProject The project where the build will be resumed with in the next run.
     * @return An optional containing a comma separated list of projects which can be skipped,
     *   or an empty optional if no projects can be skipped.
     */
    private Optional<String> determineProjectsToSkip( MavenExecutionResult result, List<MavenProject> failedProjects,
                                                      MavenProject resumeFromProject )
    {
        List<MavenProject> allProjects = result.getTopologicallySortedProjects();
        int resumeFromProjectIndex = allProjects.indexOf( resumeFromProject );
        List<MavenProject> remainingProjects = allProjects.subList( resumeFromProjectIndex + 1, allProjects.size() );

        List<GroupArtifactPair> failedProjectsGAList = failedProjects.stream()
                .map( GroupArtifactPair::new )
                .collect( Collectors.toList() );

        String projectsToSkip = remainingProjects.stream()
                .filter( project -> result.getBuildSummary( project ) instanceof BuildSuccess )
                .filter( project -> hasNoDependencyOnProjects( project, failedProjectsGAList ) )
                .map( project -> project.getGroupId() + ":" + project.getArtifactId() )
                .collect( Collectors.joining( PROPERTY_DELIMITER ) );

        if ( !StringUtils.isEmpty( projectsToSkip ) )
        {
            return Optional.of( projectsToSkip );
        }

        return Optional.empty();
    }

    private boolean hasNoDependencyOnProjects( MavenProject project, List<GroupArtifactPair> projectsGAs )
    {
        return project.getDependencies().stream()
                .map( GroupArtifactPair::new )
                .noneMatch( projectsGAs::contains );
    }

    private boolean writeResumptionFile( MavenProject rootProject, Properties properties )
            throws BuildResumptionPersistenceException
    {
        Path resumeProperties = Paths.get( rootProject.getBuild().getDirectory(), RESUME_PROPERTIES_FILENAME );
        try
        {
            Files.createDirectories( resumeProperties.getParent() );
            try ( Writer writer = Files.newBufferedWriter( resumeProperties ) )
            {
                properties.store( writer, null );
            }
        }
        catch ( IOException e )
        {
            String message = "Could not create " + RESUME_PROPERTIES_FILENAME + " file.";
            throw new BuildResumptionPersistenceException( message, e );
        }

        return true;
    }

    private Properties loadResumptionFile( Path rootBuildDirectory )
    {
        Properties properties = new Properties();
        Path path = Paths.get( RESUME_PROPERTIES_FILENAME ).resolve( rootBuildDirectory );
        if ( !Files.exists( path ) )
        {
            LOGGER.warn( "The {} file does not exist. The --resume / -r feature will not work.", path );
            return properties;
        }

        try ( Reader reader = Files.newBufferedReader( path ) )
        {
            properties.load( reader );
        }
        catch ( IOException e )
        {
            LOGGER.warn( "Unable to read {}. The --resume / -r feature will not work.", path );
        }

        return properties;
    }

    // This method is made package-private for testing purposes
    void applyResumptionProperties( MavenExecutionRequest request, Properties properties )
    {
        if ( properties.containsKey( RESUME_FROM_PROPERTY ) && StringUtils.isEmpty( request.getResumeFrom() ) )
        {
            String propertyValue = properties.getProperty( RESUME_FROM_PROPERTY );
            request.setResumeFrom( propertyValue );
            LOGGER.info( "Resuming from {} due to the --resume / -r feature.", propertyValue );
        }

        if ( properties.containsKey( EXCLUDED_PROJECTS_PROPERTY ) )
        {
            String propertyValue = properties.getProperty( EXCLUDED_PROJECTS_PROPERTY );
            String[] excludedProjects = propertyValue.split( PROPERTY_DELIMITER );
            request.getExcludedProjects().addAll( Arrays.asList( excludedProjects ) );
            LOGGER.info( "Additionally excluding projects '{}' due to the --resume / -r feature.", propertyValue );
        }
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
