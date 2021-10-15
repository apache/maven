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
import org.apache.maven.model.Plugin;
import org.apache.maven.model.building.ModelProblem;
import org.apache.maven.model.locator.ModelLocator;
import org.apache.maven.plugin.PluginManagerException;
import org.apache.maven.plugin.PluginResolutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.ProjectBuildingResult;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.transfer.ArtifactNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final Logger LOGGER = LoggerFactory.getLogger( MultiModuleCollectionStrategy.class );
    private final ModelLocator modelLocator;
    private final ProjectsSelector projectsSelector;

    @Inject
    public MultiModuleCollectionStrategy( ModelLocator modelLocator, ProjectsSelector projectsSelector )
    {
        this.modelLocator = modelLocator;
        this.projectsSelector = projectsSelector;
    }

    @Override
    public List<MavenProject> collectProjects( MavenExecutionRequest request ) throws ProjectBuildingException
    {
        File moduleProjectPomFile = getMultiModuleProjectPomFile( request );
        List<File> files = Collections.singletonList( moduleProjectPomFile.getAbsoluteFile() );
        try
        {
            List<MavenProject> projects = projectsSelector.selectProjects( files, request );
            boolean isRequestedProjectCollected = isRequestedProjectCollected( request, projects );
            if ( isRequestedProjectCollected )
            {
                return projects;
            }
            else
            {
                LOGGER.debug( "Multi module project collection failed:{}"
                        + "Detected a POM file next to a .mvn directory in a parent directory ({}). "
                        + "Maven assumed that POM file to be the parent of the requested project ({}), but it turned "
                        + "out that it was not. Another project collection strategy will be executed as result.",
                        System.lineSeparator(), moduleProjectPomFile.getAbsolutePath(),
                        request.getPom().getAbsolutePath() );
                return Collections.emptyList();
            }
        }
        catch ( ProjectBuildingException e )
        {
            boolean fallThrough = isModuleOutsideRequestScopeDependingOnPluginModule( request, e );

            if ( fallThrough )
            {
                LOGGER.debug( "Multi module project collection failed:{}"
                        + "Detected that one of the modules of this multi module project uses another module as "
                        + "plugin extension which still needed to be built. This is not possible within the same "
                        + "reactor build. Another project collection strategy will be executed as result.",
                        System.lineSeparator() );
                return Collections.emptyList();
            }

            throw e;
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
            File multiModuleProjectPom = modelLocator.locatePom( request.getMultiModuleProjectDirectory() );
            if ( !multiModuleProjectPom.exists() )
            {
                LOGGER.info( "Maven detected that the requested POM file is part of a multi module project, "
                        + "but could not find a pom.xml file in the multi module root directory '{}'.",
                        request.getMultiModuleProjectDirectory() );
                LOGGER.info( "The reactor is limited to all projects under: " + request.getPom().getParent() );
                return request.getPom();
            }

            return multiModuleProjectPom;
        }
    }

    /**
     * multiModuleProjectDirectory in MavenExecutionRequest is not always the parent of the request pom.
     * We should always check whether the request pom project is collected.
     * The integration tests for MNG-6223 are examples for this scenario.
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
    private boolean isModuleOutsideRequestScopeDependingOnPluginModule( MavenExecutionRequest request,
                                                                        ProjectBuildingException exception )
    {
        return exception.getResults().stream()
                .map( ProjectBuildingResult::getProject )
                .filter( Objects::nonNull )
                .filter( project -> request.getPom().equals( project.getFile() ) )
                .findFirst()
                .map( requestPomProject ->
                {
                    List<MavenProject> modules = requestPomProject.getCollectedProjects() != null
                            ? requestPomProject.getCollectedProjects() : Collections.emptyList();
                    List<MavenProject> projectsInRequestScope = new ArrayList<>( modules );
                    projectsInRequestScope.add( requestPomProject );

                    Predicate<ProjectBuildingResult> projectsOutsideOfRequestScope =
                            pr -> !projectsInRequestScope.contains( pr.getProject() );

                    Predicate<Exception> pluginArtifactNotFoundException =
                            exc -> exc instanceof PluginManagerException
                                    && exc.getCause() instanceof PluginResolutionException
                                    && exc.getCause().getCause() instanceof ArtifactResolutionException
                                    && exc.getCause().getCause().getCause() instanceof ArtifactNotFoundException;

                    Predicate<Plugin> isPluginPartOfRequestScope = plugin -> projectsInRequestScope.stream()
                            .anyMatch( project -> project.getGroupId().equals( plugin.getGroupId() )
                                    && project.getArtifactId().equals( plugin.getArtifactId() )
                                    && project.getVersion().equals( plugin.getVersion() ) );

                    return exception.getResults().stream()
                            .filter( projectsOutsideOfRequestScope )
                            .flatMap( projectBuildingResult -> projectBuildingResult.getProblems().stream() )
                            .map( ModelProblem::getException )
                            .filter( pluginArtifactNotFoundException )
                            .map( exc -> ( ( PluginResolutionException ) exc.getCause() ).getPlugin() )
                            .anyMatch( isPluginPartOfRequestScope );
                } ).orElse( false );
    }
}
