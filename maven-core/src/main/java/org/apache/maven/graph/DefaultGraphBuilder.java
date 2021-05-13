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

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.MavenExecutionException;
import org.apache.maven.ProjectCycleException;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.execution.BuildResumptionDataRepository;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.execution.ProjectActivation;
import org.apache.maven.execution.ProjectDependencyGraph;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.building.DefaultModelProblem;
import org.apache.maven.model.building.Result;
import org.apache.maven.project.DuplicateProjectException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.collector.MultiModuleCollectionStrategy;
import org.apache.maven.project.collector.PomlessCollectionStrategy;
import org.apache.maven.project.collector.RequestPomCollectionStrategy;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.dag.CycleDetectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Comparator.comparing;

/**
 * Builds the {@link ProjectDependencyGraph inter-dependencies graph} between projects in the reactor.
 */
@Named( GraphBuilder.HINT )
@Singleton
public class DefaultGraphBuilder
    implements GraphBuilder
{
    private static final Logger LOGGER = LoggerFactory.getLogger( DefaultGraphBuilder.class );

    private final BuildResumptionDataRepository buildResumptionDataRepository;
    private final PomlessCollectionStrategy pomlessCollectionStrategy;
    private final MultiModuleCollectionStrategy multiModuleCollectionStrategy;
    private final RequestPomCollectionStrategy requestPomCollectionStrategy;

    @Inject
    public DefaultGraphBuilder( BuildResumptionDataRepository buildResumptionDataRepository,
                                PomlessCollectionStrategy pomlessCollectionStrategy,
                                MultiModuleCollectionStrategy multiModuleCollectionStrategy,
                                RequestPomCollectionStrategy requestPomCollectionStrategy )
    {
        this.buildResumptionDataRepository = buildResumptionDataRepository;
        this.pomlessCollectionStrategy = pomlessCollectionStrategy;
        this.multiModuleCollectionStrategy = multiModuleCollectionStrategy;
        this.requestPomCollectionStrategy = requestPomCollectionStrategy;
    }

    @Override
    public Result<ProjectDependencyGraph> build( MavenSession session )
    {
        try
        {
            Result<ProjectDependencyGraph> result = sessionDependencyGraph( session );

            if ( result == null )
            {
                final List<MavenProject> projects = getProjectsForMavenReactor( session );
                validateProjects( projects, session.getRequest() );
                enrichRequestFromResumptionData( projects, session.getRequest() );
                result = reactorDependencyGraph( session, projects );
            }

            return result;
        }
        catch ( final ProjectBuildingException | DuplicateProjectException | MavenExecutionException e )
        {
            return Result.error( Collections.singletonList
                    ( new DefaultModelProblem ( null, null, null, null, 0, 0, e ) ) );
        }
        catch ( final CycleDetectedException e )
        {
            String message = "The projects in the reactor contain a cyclic reference: " + e.getMessage();
            ProjectCycleException error = new ProjectCycleException( message, e );
            return Result.error( Collections.singletonList(
                    new DefaultModelProblem( null, null, null, null, 0, 0, error ) ) );
        }
    }

    private Result<ProjectDependencyGraph> sessionDependencyGraph( final MavenSession session )
        throws CycleDetectedException, DuplicateProjectException
    {
        Result<ProjectDependencyGraph> result = null;

        if ( session.getProjectDependencyGraph() != null || session.getProjects() != null )
        {
            final ProjectDependencyGraph graph =
                new DefaultProjectDependencyGraph( session.getAllProjects(), session.getProjects() );

            result = Result.success( graph );
        }

        return result;
    }

    private Result<ProjectDependencyGraph> reactorDependencyGraph( MavenSession session, List<MavenProject> projects )
        throws CycleDetectedException, DuplicateProjectException, MavenExecutionException
    {
        ProjectDependencyGraph projectDependencyGraph = new DefaultProjectDependencyGraph( projects );
        List<MavenProject> activeProjects = projectDependencyGraph.getSortedProjects();
        activeProjects = trimProjectsToRequest( activeProjects, projectDependencyGraph, session.getRequest() );
        activeProjects = trimSelectedProjects( activeProjects, projectDependencyGraph, session.getRequest() );
        activeProjects = trimResumedProjects( activeProjects, projectDependencyGraph, session.getRequest() );
        activeProjects = trimExcludedProjects( activeProjects, projectDependencyGraph, session.getRequest() );

        if ( activeProjects.size() != projectDependencyGraph.getSortedProjects().size() )
        {
            projectDependencyGraph = new FilteredProjectDependencyGraph( projectDependencyGraph, activeProjects );
        }

        return Result.success( projectDependencyGraph );
    }

    private List<MavenProject> trimProjectsToRequest( List<MavenProject> activeProjects,
                                                      ProjectDependencyGraph graph,
                                                      MavenExecutionRequest request )
            throws MavenExecutionException
    {
        List<MavenProject> result = activeProjects;

        if ( request.getPom() != null )
        {
            result = getProjectsInRequestScope( request, activeProjects );

            List<MavenProject> sortedProjects = graph.getSortedProjects();
            result.sort( comparing( sortedProjects::indexOf ) );

            result = includeAlsoMakeTransitively( result, request, graph );
        }

        return result;
    }

    private List<MavenProject> trimSelectedProjects( List<MavenProject> projects, ProjectDependencyGraph graph,
                                                     MavenExecutionRequest request )
        throws MavenExecutionException
    {
        List<MavenProject> result = projects;

        ProjectActivation projectActivation = request.getProjectActivation();
        Set<String> requiredSelectors = projectActivation.getRequiredActiveProjectSelectors();
        Set<String> optionalSelectors = projectActivation.getOptionalActiveProjectSelectors();
        if ( !requiredSelectors.isEmpty() || !optionalSelectors.isEmpty() )
        {
            Set<MavenProject> selectedProjects = new HashSet<>( requiredSelectors.size() + optionalSelectors.size() );
            selectedProjects.addAll( getProjectsBySelectors( request, projects, requiredSelectors, true ) );
            selectedProjects.addAll( getProjectsBySelectors( request, projects, optionalSelectors, false ) );

            // it can be empty when an optional project is missing from the reactor, fallback to returning all projects
            if ( !selectedProjects.isEmpty() )
            {
                result = new ArrayList<>( selectedProjects );

                result = includeAlsoMakeTransitively( result, request, graph );

                // Order the new list in the original order
                List<MavenProject> sortedProjects = graph.getSortedProjects();
                result.sort( comparing( sortedProjects::indexOf ) );
            }
        }

        return result;
    }

    private Set<MavenProject> getProjectsBySelectors( MavenExecutionRequest request, List<MavenProject> projects,
                                                      Set<String> projectSelectors, boolean required )
            throws MavenExecutionException
    {
        Set<MavenProject> selectedProjects = new LinkedHashSet<>();
        File reactorDirectory = getReactorDirectory( request );

        for ( String selector : projectSelectors )
        {
            Optional<MavenProject> optSelectedProject = projects.stream()
                    .filter( project -> isMatchingProject( project, selector, reactorDirectory ) )
                    .findFirst();
            if ( !optSelectedProject.isPresent() )
            {
                String message = "Could not find the selected project in the reactor: " + selector;
                if ( required )
                {
                    throw new MavenExecutionException( message, request.getPom() );
                }
                else
                {
                    LOGGER.info( message );
                    break;
                }
            }

            MavenProject selectedProject = optSelectedProject.get();

            selectedProjects.add( selectedProject );

            List<MavenProject> children = selectedProject.getCollectedProjects();
            if ( children != null && request.isRecursive() )
            {
                selectedProjects.addAll( children );
            }
        }

        return selectedProjects;
    }

    private List<MavenProject> trimResumedProjects( List<MavenProject> projects, ProjectDependencyGraph graph,
                                                    MavenExecutionRequest request )
            throws MavenExecutionException
    {
        List<MavenProject> result = projects;

        if ( StringUtils.isNotEmpty( request.getResumeFrom() ) )
        {
            File reactorDirectory = getReactorDirectory( request );

            String selector = request.getResumeFrom();

            MavenProject resumingFromProject = projects.stream()
                    .filter( project -> isMatchingProject( project, selector, reactorDirectory ) )
                    .findFirst()
                    .orElseThrow( () -> new MavenExecutionException(
                            "Could not find project to resume reactor build from: " + selector + " vs "
                            + formatProjects( projects ), request.getPom() ) );
            int resumeFromProjectIndex = projects.indexOf( resumingFromProject );
            List<MavenProject> retainingProjects = result.subList( resumeFromProjectIndex, projects.size() );

            result = includeAlsoMakeTransitively( retainingProjects, request, graph );
        }

        return result;
    }

    private List<MavenProject> trimExcludedProjects( List<MavenProject> projects, ProjectDependencyGraph graph,
                                                     MavenExecutionRequest request )
        throws MavenExecutionException
    {
        List<MavenProject> result = projects;

        ProjectActivation projectActivation = request.getProjectActivation();
        Set<String> requiredSelectors = projectActivation.getRequiredInactiveProjectSelectors();
        Set<String> optionalSelectors = projectActivation.getOptionalInactiveProjectSelectors();
        if ( !requiredSelectors.isEmpty() || !optionalSelectors.isEmpty() )
        {
            Set<MavenProject> excludedProjects = new HashSet<>( requiredSelectors.size() + optionalSelectors.size() );
            List<MavenProject> allProjects = graph.getAllProjects();
            excludedProjects.addAll( getProjectsBySelectors( request, allProjects, requiredSelectors, true ) );
            excludedProjects.addAll( getProjectsBySelectors( request, allProjects, optionalSelectors, false ) );

            result = new ArrayList<>( projects );
            result.removeAll( excludedProjects );

            if ( result.isEmpty() )
            {
                boolean isPlural = excludedProjects.size() > 1;
                String message = String.format( "The project exclusion%s in --projects/-pl resulted in an "
                        + "empty reactor, please correct %s.", isPlural ? "s" : "", isPlural ? "them" : "it" );
                throw new MavenExecutionException( message, request.getPom() );
            }
        }

        return result;
    }

    private List<MavenProject> includeAlsoMakeTransitively( List<MavenProject> projects, MavenExecutionRequest request,
                                                            ProjectDependencyGraph graph )
            throws MavenExecutionException
    {
        List<MavenProject> result = projects;

        String makeBehavior = request.getMakeBehavior();
        boolean makeBoth = MavenExecutionRequest.REACTOR_MAKE_BOTH.equals( makeBehavior );

        boolean makeUpstream = makeBoth || MavenExecutionRequest.REACTOR_MAKE_UPSTREAM.equals( makeBehavior );
        boolean makeDownstream = makeBoth || MavenExecutionRequest.REACTOR_MAKE_DOWNSTREAM.equals( makeBehavior );

        if ( StringUtils.isNotEmpty( makeBehavior ) && !makeUpstream && !makeDownstream )
        {
            throw new MavenExecutionException( "Invalid reactor make behavior: " + makeBehavior,
                    request.getPom() );
        }

        if ( makeUpstream || makeDownstream )
        {
            Set<MavenProject> projectsSet = new HashSet<>( projects );

            for ( MavenProject project : projects )
            {
                if ( makeUpstream )
                {
                    projectsSet.addAll( graph.getUpstreamProjects( project, true ) );
                }
                if ( makeDownstream )
                {
                    projectsSet.addAll( graph.getDownstreamProjects( project, true ) );
                }
            }

            result = new ArrayList<>( projectsSet );

            // Order the new list in the original order
            List<MavenProject> sortedProjects = graph.getSortedProjects();
            result.sort( comparing( sortedProjects::indexOf ) );
        }

        return result;
    }

    private void enrichRequestFromResumptionData( List<MavenProject> projects, MavenExecutionRequest request )
    {
        if ( request.isResume() )
        {
            projects.stream()
                    .filter( MavenProject::isExecutionRoot )
                    .findFirst()
                    .ifPresent( rootProject ->
                            buildResumptionDataRepository.applyResumptionData( request, rootProject ) );
        }
    }

    private List<MavenProject> getProjectsInRequestScope( MavenExecutionRequest request, List<MavenProject> projects )
            throws MavenExecutionException
    {
        if ( request.getPom() == null )
        {
            return projects;
        }

        MavenProject requestPomProject = projects.stream()
                .filter( project -> request.getPom().equals( project.getFile() ) )
                .findFirst()
                .orElseThrow( () -> new MavenExecutionException(
                        "Could not find a project in reactor matching the request POM", request.getPom() ) );

        List<MavenProject> modules = requestPomProject.getCollectedProjects() != null
                ? requestPomProject.getCollectedProjects() : Collections.emptyList();

        List<MavenProject> result = new ArrayList<>( modules );
        result.add( requestPomProject );
        return result;
    }

    private String formatProjects( List<MavenProject> projects )
    {
        StringBuilder projectNames = new StringBuilder();
        Iterator<MavenProject> iterator = projects.iterator();
        while ( iterator.hasNext() )
        {
            MavenProject project = iterator.next();
            projectNames.append( project.getGroupId() ).append( ":" ).append( project.getArtifactId() );
            if ( iterator.hasNext() )
            {
                projectNames.append( ", " );
            }
        }
        return projectNames.toString();
    }

    private boolean isMatchingProject( MavenProject project, String selector, File reactorDirectory )
    {
        // [groupId]:artifactId
        if ( selector.indexOf( ':' ) >= 0 )
        {
            String id = ':' + project.getArtifactId();

            if ( id.equals( selector ) )
            {
                return true;
            }

            id = project.getGroupId() + id;

            if ( id.equals( selector ) )
            {
                return true;
            }
        }

        // relative path, e.g. "sub", "../sub" or "."
        else if ( reactorDirectory != null )
        {
            File selectedProject = new File( new File( reactorDirectory, selector ).toURI().normalize() );

            if ( selectedProject.isFile() )
            {
                return selectedProject.equals( project.getFile() );
            }
            else if ( selectedProject.isDirectory() )
            {
                return selectedProject.equals( project.getBasedir() );
            }
        }

        return false;
    }

    private File getReactorDirectory( MavenExecutionRequest request )
    {
        if ( request.getBaseDirectory() != null )
        {
            return new File( request.getBaseDirectory() );
        }

        return null;
    }

    // ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // Project collection
    //
    // ////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private List<MavenProject> getProjectsForMavenReactor( MavenSession session )
        throws ProjectBuildingException
    {
        MavenExecutionRequest request = session.getRequest();
        request.getProjectBuildingRequest().setRepositorySession( session.getRepositorySession() );

        // 1. Collect project for invocation without a POM.
        if ( request.getPom() == null )
        {
            return pomlessCollectionStrategy.collectProjects( request );
        }

        // 2. Collect projects for all modules in the multi-module project.
        List<MavenProject> projects = multiModuleCollectionStrategy.collectProjects( request );
        if ( !projects.isEmpty() )
        {
            return projects;
        }

        // 3. Collect projects for explicitly requested POM.
        return requestPomCollectionStrategy.collectProjects( request );
    }

    private void validateProjects( List<MavenProject> projects, MavenExecutionRequest request )
            throws MavenExecutionException
    {
        Map<String, MavenProject> projectsMap = new HashMap<>();

        List<MavenProject> projectsInRequestScope = getProjectsInRequestScope( request, projects );
        for ( MavenProject p : projectsInRequestScope )
        {
            String projectKey = ArtifactUtils.key( p.getGroupId(), p.getArtifactId(), p.getVersion() );

            projectsMap.put( projectKey, p );
        }

        for ( MavenProject project : projects )
        {
            // MNG-1911 / MNG-5572: Building plugins with extensions cannot be part of reactor
            for ( Plugin plugin : project.getBuildPlugins() )
            {
                if ( plugin.isExtensions() )
                {
                    String pluginKey = ArtifactUtils.key( plugin.getGroupId(), plugin.getArtifactId(),
                                                          plugin.getVersion() );

                    if ( projectsMap.containsKey( pluginKey ) )
                    {
                        LOGGER.warn( "'{}' uses '{}' as extension which is not possible within the same reactor build. "
                            + "This plugin was pulled from the local repository!", project.getName(), plugin.getKey() );
                    }
                }
            }
        }
    }

}
