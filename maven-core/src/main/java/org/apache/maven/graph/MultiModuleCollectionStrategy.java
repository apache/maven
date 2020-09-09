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

import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.building.ModelProblem;
import org.apache.maven.plugin.PluginManagerException;
import org.apache.maven.plugin.PluginResolutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.ProjectBuildingResult;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.transfer.ArtifactNotFoundException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Strategy for collecting Maven projects from the multi-module project root, even when executed in a submodule.
 */
@Named( "MultiModuleCollectionStrategy" )
@Singleton
public class MultiModuleCollectionStrategy implements ProjectCollectionStrategy
{
    private final Logger logger;
    private final ProjectCollector projectCollector;

    @Inject
    public MultiModuleCollectionStrategy( Logger logger, ProjectCollector projectCollector )
    {
        this.logger = logger;
        this.projectCollector = projectCollector;
    }

    @Override
    public List<MavenProject> collectProjects( MavenExecutionRequest request ) throws ProjectBuildingException
    {
        File moduleProjectPomFile = getMultiModuleProjectPomFile( request );
        List<File> files = Collections.singletonList( moduleProjectPomFile.getAbsoluteFile() );
        try
        {
            List<MavenProject> projects = new ArrayList<>();
            projectCollector.collectProjects( projects, files, request );
            boolean isRequestedProjectCollected = isRequestedProjectCollected( request, projects );
            if ( isRequestedProjectCollected )
            {
                return projects;
            }
            else
            {
                return Collections.emptyList();
            }
        }
        catch ( ProjectBuildingException e )
        {
            boolean shouldRetryWithOnlyProjectsInBuild = isModuleOutsideBuildDependingOnPluginModule( request, e );

            if ( !shouldRetryWithOnlyProjectsInBuild )
            {
                throw e;
            }

            return Collections.emptyList();
        }
    }

    private File getMultiModuleProjectPomFile( MavenExecutionRequest request )
    {
        if ( request.getPom().getParentFile().equals( request.getMultiModuleProjectDirectory() ) )
        {
            return request.getPom();
        }
        else
        {
            File multiModuleProjectPom = new File( request.getMultiModuleProjectDirectory(), "pom.xml" );
            if ( !multiModuleProjectPom.exists() )
            {
                logger.info( "Maven detected that the requested POM file is part of a multi module project, "
                        + "but could not find a pom.xml file in the multi module root directory: '"
                        + request.getMultiModuleProjectDirectory() + "'. " );
                logger.info( "The reactor is limited to all projects under: " + request.getPom().getParent() );
                return request.getPom();
            }

            return multiModuleProjectPom;
        }
    }

    /**
     * multiModuleProjectDirectory in MavenExecutionRequest is not always the parent of the requested pom.
     * We should always check whether the requested pom project is collected.
     * The integration tests for MNG-5889 are examples for this scenario.
     *
     * @return true if the collected projects contain the requested project (for example with -f)
     */
    private boolean isRequestedProjectCollected( MavenExecutionRequest request, List<MavenProject> projects )
    {
        return projects.stream()
                .map( MavenProject::getFile )
                .anyMatch( request.getPom()::equals );
    }

    /**
     * This method finds out whether collecting projects failed because of the following scenario:
     * - A multi module project containing a module which is a plugin and another module which depends on it.
     * - Just the plugin is being built with the -f <pom> flag.
     * - Because of inter-module dependency collection, all projects in the multi-module project are collected.
     * - The plugin is not yet installed in a repository.
     *
     * Therefore the build fails because the plugin is not found and plugins cannot be built in the same session.
     *
     * The integration test for <a href="https://issues.apache.org/jira/browse/MNG-5572">MNG-5572</a> is an
     *   example of this scenario.
     *
     * @return true if the module which fails to collect the inter-module plugin is not part of the build.
     */
    private boolean isModuleOutsideBuildDependingOnPluginModule( MavenExecutionRequest request,
                                                                 ProjectBuildingException exception )
    {
        if ( request.getPom() == null )
        {
            return false;
        }

        return exception.getResults().stream()
                .map( ProjectBuildingResult::getProject )
                .filter( Objects::nonNull )
                .filter( project -> request.getPom().equals( project.getFile() ) )
                .findFirst()
                .map( buildProjectRoot ->
                {
                    List<MavenProject> modules = buildProjectRoot.getCollectedProjects() != null
                            ? buildProjectRoot.getCollectedProjects() : Collections.emptyList();
                    List<MavenProject> projectsToBeBuilt = new ArrayList<>( modules );
                    projectsToBeBuilt.add( buildProjectRoot );

                    Predicate<ProjectBuildingResult> projectsOutsideOfBuild =
                            pr -> !projectsToBeBuilt.contains( pr.getProject() );

                    Predicate<Exception> pluginArtifactNotFoundException =
                            exc -> exc instanceof PluginManagerException
                                    && exc.getCause() instanceof PluginResolutionException
                                    && exc.getCause().getCause() instanceof ArtifactResolutionException
                                    && exc.getCause().getCause().getCause() instanceof ArtifactNotFoundException;

                    Predicate<Plugin> isPluginPartOfBuild = plugin -> projectsToBeBuilt.stream()
                            .anyMatch( project -> project.getGroupId().equals( plugin.getGroupId() )
                                    && project.getArtifactId().equals( plugin.getArtifactId() )
                                    && project.getVersion().equals( plugin.getVersion() ) );

                    return exception.getResults().stream()
                            .filter( projectsOutsideOfBuild )
                            .flatMap( projectBuildingResult -> projectBuildingResult.getProblems().stream() )
                            .map( ModelProblem::getException )
                            .filter( pluginArtifactNotFoundException )
                            .map( exc -> ( ( PluginResolutionException ) exc.getCause() ).getPlugin() )
                            .anyMatch( isPluginPartOfBuild );
                } ).orElse( false );
    }
}
