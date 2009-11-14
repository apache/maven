package org.apache.maven.lifecycle;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.maven.ProjectDependenciesResolver;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.artifact.repository.DefaultRepositoryRequest;
import org.apache.maven.artifact.repository.RepositoryRequest;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.MultipleArtifactsNotFoundException;
import org.apache.maven.execution.BuildFailure;
import org.apache.maven.execution.BuildSuccess;
import org.apache.maven.execution.DefaultLifecycleEvent;
import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.execution.ExecutionListener;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.mapping.LifecycleMapping;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.plugin.InvalidPluginDescriptorException;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.MojoNotFoundException;
import org.apache.maven.plugin.PluginConfigurationException;
import org.apache.maven.plugin.PluginDescriptorParsingException;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.PluginManagerException;
import org.apache.maven.plugin.PluginNotFoundException;
import org.apache.maven.plugin.PluginResolutionException;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.Parameter;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.lifecycle.Execution;
import org.apache.maven.plugin.lifecycle.Phase;
import org.apache.maven.plugin.prefix.DefaultPluginPrefixRequest;
import org.apache.maven.plugin.prefix.NoPluginFoundForPrefixException;
import org.apache.maven.plugin.prefix.PluginPrefixRequest;
import org.apache.maven.plugin.prefix.PluginPrefixResolver;
import org.apache.maven.plugin.prefix.PluginPrefixResult;
import org.apache.maven.plugin.version.DefaultPluginVersionRequest;
import org.apache.maven.plugin.version.PluginVersionRequest;
import org.apache.maven.plugin.version.PluginVersionResolutionException;
import org.apache.maven.plugin.version.PluginVersionResolver;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

//TODO: The configuration for the lifecycle needs to be externalized so that I can use the annotations properly for the wiring and reference and external source for the lifecycle configuration.
//TODO: check for online status in the build plan and die if necessary

/**
 * @author Jason van Zyl
 */
public class DefaultLifecycleExecutor
    implements LifecycleExecutor, Initializable
{
    @Requirement
    private Logger logger;

    @Requirement
    private BuildPluginManager pluginManager;

    @Requirement
    protected RepositorySystem repositorySystem;

    @Requirement
    private ProjectDependenciesResolver projectDependenciesResolver;

    @Requirement
    private PluginVersionResolver pluginVersionResolver;

    @Requirement
    private PluginPrefixResolver pluginPrefixResolver;
            
    // @Configuration(source="org/apache/maven/lifecycle/lifecycles.xml")    
    private List<Lifecycle> lifecycles;

    /**
     * We use this to display all the lifecycles available and their phases to users. Currently this is primarily
     * used in the IDE integrations where a UI is presented to the user and they can select the lifecycle phase
     * they would like to execute.
     */
    private Map<String,Lifecycle> lifecycleMap;
    
    /**
     * We use this to map all phases to the lifecycle that contains it. This is used so that a user can specify the 
     * phase they want to execute and we can easily determine what lifecycle we need to run.
     */
    private Map<String, Lifecycle> phaseToLifecycleMap;

    /**
     * These mappings correspond to packaging types, like WAR packaging, which configure a particular mojos
     * to run in a given phase.
     */
    @Requirement
    private Map<String, LifecycleMapping> lifecycleMappings;

    private void fireEvent( MavenSession session, MojoExecution mojoExecution, LifecycleEventCatapult catapult )
    {
        ExecutionListener listener = session.getRequest().getExecutionListener();

        if ( listener != null )
        {
            ExecutionEvent event = new DefaultLifecycleEvent( session, mojoExecution );

            catapult.fire( listener, event );
        }
    }

    private static String getKey( MavenProject project )
    {
        return project.getGroupId() + ':' + project.getArtifactId() + ':' + project.getVersion();
    }

    private void debugReactorPlan( List<ProjectBuild> projectBuilds )
    {
        logger.debug( "=== REACTOR BUILD PLAN ================================================" );

        for ( Iterator<ProjectBuild> it = projectBuilds.iterator(); it.hasNext(); )
        {
            ProjectBuild projectBuild = it.next();

            logger.debug( "Project: " + projectBuild.project.getId() );
            logger.debug( "Tasks:   " + projectBuild.taskSegment.tasks );
            logger.debug( "Style:   " + ( projectBuild.taskSegment.aggregating ? "Aggregating" : "Regular" ) );

            if ( it.hasNext() )
            {
                logger.debug( "-----------------------------------------------------------------------" );
            }
        }

        logger.debug( "=======================================================================" );
    }

    private void debugProjectPlan( MavenProject currentProject, MavenExecutionPlan executionPlan )
    {
        logger.debug( "=== PROJECT BUILD PLAN ================================================" );
        logger.debug( "Project:       " + getKey( currentProject ) );
        logger.debug( "Dependencies (collect): " + executionPlan.getRequiredCollectionScopes() );
        logger.debug( "Dependencies (resolve): " + executionPlan.getRequiredResolutionScopes() );

        for ( MojoExecution mojoExecution : executionPlan.getExecutions() )
        {
            debugMojoExecution( mojoExecution );
        }

        logger.debug( "=======================================================================" );
    }

    private void debugMojoExecution( MojoExecution mojoExecution )
    {
        String mojoExecId =
            mojoExecution.getGroupId() + ':' + mojoExecution.getArtifactId() + ':' + mojoExecution.getVersion() + ':'
                + mojoExecution.getGoal() + " (" + mojoExecution.getExecutionId() + ')';

        Map<String, List<MojoExecution>> forkedExecutions = mojoExecution.getForkedExecutions();
        if ( !forkedExecutions.isEmpty() )
        {
            for ( Map.Entry<String, List<MojoExecution>> fork : forkedExecutions.entrySet() )
            {
                logger.debug( "--- init fork of " + fork.getKey() + " for " + mojoExecId + " ---" );

                for ( MojoExecution forkedExecution : fork.getValue() )
                {
                    debugMojoExecution( forkedExecution );
                }

                logger.debug( "--- exit fork of " + fork.getKey() + " for " + mojoExecId + " ---" );
            }
        }

        logger.debug( "-----------------------------------------------------------------------" );
        logger.debug( "Goal:          " + mojoExecId );
        logger.debug( "Style:         "
            + ( mojoExecution.getMojoDescriptor().isAggregating() ? "Aggregating" : "Regular" ) );
        logger.debug( "Configuration: " + mojoExecution.getConfiguration() );
    }

    public void execute( MavenSession session )
    {
        fireEvent( session, null, LifecycleEventCatapult.SESSION_STARTED );

        MavenExecutionResult result = session.getResult();

        List<ProjectBuild> projectBuilds;

        ProjectIndex projectIndex;

        try
        {
            projectBuilds = calculateProjectBuilds( session );

            projectIndex = new ProjectIndex( session.getProjects() );
        }
        catch ( Exception e )
        {
            result.addException( e );

            fireEvent( session, null, LifecycleEventCatapult.SESSION_ENDED );

            return;
        }

        if ( logger.isDebugEnabled() )
        {
            debugReactorPlan( projectBuilds );
        }

        ClassLoader oldContextClassLoader = Thread.currentThread().getContextClassLoader();

        for ( ProjectBuild projectBuild : projectBuilds )
        {
            MavenProject currentProject = projectBuild.project;

            long buildStartTime = System.currentTimeMillis();

            try
            {
                session.setCurrentProject( currentProject );

                if ( session.isBlackListed( currentProject ) )
                {
                    fireEvent( session, null, LifecycleEventCatapult.PROJECT_SKIPPED );

                    continue;
                }

                fireEvent( session, null, LifecycleEventCatapult.PROJECT_STARTED );

                ClassRealm projectRealm = currentProject.getClassRealm();
                if ( projectRealm != null )
                {
                    Thread.currentThread().setContextClassLoader( projectRealm );
                }

                MavenExecutionPlan executionPlan =
                    calculateExecutionPlan( session, currentProject, projectBuild.taskSegment );

                if ( logger.isDebugEnabled() )
                {
                    debugProjectPlan( currentProject, executionPlan );
                }

                // TODO: once we have calculated the build plan then we should accurately be able to download
                // the project dependencies. Having it happen in the plugin manager is a tangled mess. We can optimize
                // this later by looking at the build plan. Would be better to just batch download everything required
                // by the reactor.

                List<MavenProject> projectsToResolve;

                if ( projectBuild.taskSegment.aggregating )
                {
                    projectsToResolve = session.getProjects();
                }
                else
                {
                    projectsToResolve = Collections.singletonList( currentProject );
                }

                for ( MavenProject project : projectsToResolve )
                {
                    resolveProjectDependencies( project, executionPlan.getRequiredCollectionScopes(),
                                                executionPlan.getRequiredResolutionScopes(), session,
                                                projectBuild.taskSegment.aggregating );
                }

                DependencyContext dependencyContext =
                    new DependencyContext( executionPlan, projectBuild.taskSegment.aggregating );

                for ( MojoExecution mojoExecution : executionPlan.getExecutions() )
                {
                    execute( session, mojoExecution, projectIndex, dependencyContext );
                }

                long buildEndTime = System.currentTimeMillis();

                result.addBuildSummary( new BuildSuccess( currentProject, buildEndTime - buildStartTime ) );

                fireEvent( session, null, LifecycleEventCatapult.PROJECT_SUCCEEDED );
            }
            catch ( Exception e )
            {
                result.addException( e );

                long buildEndTime = System.currentTimeMillis();

                result.addBuildSummary( new BuildFailure( currentProject, buildEndTime - buildStartTime, e ) );

                fireEvent( session, null, LifecycleEventCatapult.PROJECT_FAILED );

                if ( MavenExecutionRequest.REACTOR_FAIL_NEVER.equals( session.getReactorFailureBehavior() ) )
                {
                    // continue the build
                }
                else if ( MavenExecutionRequest.REACTOR_FAIL_AT_END.equals( session.getReactorFailureBehavior() ) )
                {
                    // continue the build but ban all projects that depend on the failed one
                    session.blackList( currentProject );
                }
                else if ( MavenExecutionRequest.REACTOR_FAIL_FAST.equals( session.getReactorFailureBehavior() ) )
                {
                    // abort the build
                    break;
                }
                else
                {
                    throw new IllegalArgumentException( "invalid reactor failure behavior "
                        + session.getReactorFailureBehavior() );
                }
            }
            finally
            {
                session.setCurrentProject( null );

                Thread.currentThread().setContextClassLoader( oldContextClassLoader );
            }
        }

        fireEvent( session, null, LifecycleEventCatapult.SESSION_ENDED );
    }

    private void resolveProjectDependencies( MavenProject project, Collection<String> scopesToCollect,
                                             Collection<String> scopesToResolve, MavenSession session,
                                             boolean aggregating )
        throws LifecycleExecutionException
    {
        Set<Artifact> artifacts;

        try
        {
            try
            {
                artifacts = projectDependenciesResolver.resolve( project, scopesToCollect, scopesToResolve, session );
            }
            catch ( MultipleArtifactsNotFoundException e )
            {
                /*
                 * MNG-2277, the check below compensates for our bad plugin support where we ended up with aggregator
                 * plugins that require dependency resolution although they usually run in phases of the build where project
                 * artifacts haven't been assembled yet. The prime example of this is "mvn release:prepare".
                 */
                if ( aggregating && areAllArtifactsInReactor( session.getProjects(), e.getMissingArtifacts() ) )
                {
                    logger.warn( "The following artifacts could not be resolved at this point of the build"
                        + " but seem to be part of the reactor:" );

                    for ( Artifact artifact : e.getMissingArtifacts() )
                    {
                        logger.warn( "o " + artifact.getId() );
                    }

                    logger.warn( "Try running the build up to the lifecycle phase \"package\"" );

                    artifacts = new LinkedHashSet<Artifact>( e.getResolvedArtifacts() );
                }
                else
                {
                    throw e;
                }
            }
        }
        catch ( ArtifactResolutionException e )
        {
            throw new LifecycleExecutionException( null, project, e );
        }
        catch ( ArtifactNotFoundException e )
        {
            throw new LifecycleExecutionException( null, project, e );
        }

        project.setArtifacts( artifacts );

        if ( project.getDependencyArtifacts() == null )
        {
            Set<String> directDependencies = new HashSet<String>( project.getDependencies().size() * 2 );
            for ( Dependency dependency : project.getDependencies() )
            {
                directDependencies.add( dependency.getManagementKey() );
            }

            Set<Artifact> dependencyArtifacts = new LinkedHashSet<Artifact>( project.getDependencies().size() * 2 );
            for ( Artifact artifact : artifacts )
            {
                if ( directDependencies.contains( artifact.getDependencyConflictId() ) )
                {
                    dependencyArtifacts.add( artifact );
                }
            }
            project.setDependencyArtifacts( dependencyArtifacts );
        }
    }

    private boolean areAllArtifactsInReactor( Collection<MavenProject> projects, Collection<Artifact> artifacts )
    {
        Set<String> projectKeys = new HashSet<String>( projects.size() * 2 );
        for ( MavenProject project : projects )
        {
            String key = ArtifactUtils.key( project.getGroupId(), project.getArtifactId(), project.getVersion() );
            projectKeys.add( key );
        }

        for ( Artifact artifact : artifacts )
        {
            String key = ArtifactUtils.key( artifact );
            if ( !projectKeys.contains( key ) )
            {
                return false;
            }
        }

        return true;
    }

    private class DependencyContext
    {

        private final Collection<String> scopesToCollect;

        private final Collection<String> scopesToResolve;

        private final boolean aggregating;

        private MavenProject lastProject;

        private Collection<?> lastDependencyArtifacts;

        private int lastDependencyArtifactCount;

        DependencyContext( Collection<String> scopesToCollect, Collection<String> scopesToResolve, boolean aggregating )
        {
            this.scopesToCollect = scopesToCollect;
            this.scopesToResolve = scopesToResolve;
            this.aggregating = aggregating;
        }

        DependencyContext( MavenExecutionPlan executionPlan, boolean aggregating )
        {
            this.scopesToCollect = executionPlan.getRequiredCollectionScopes();
            this.scopesToResolve = executionPlan.getRequiredResolutionScopes();
            this.aggregating = aggregating;
        }

        DependencyContext( MojoExecution mojoExecution )
        {
            this.scopesToCollect = new TreeSet<String>();
            this.scopesToResolve = new TreeSet<String>();
            collectDependencyRequirements( scopesToResolve, scopesToCollect, mojoExecution );
            this.aggregating = mojoExecution.getMojoDescriptor().isAggregating();
        }

        public DependencyContext clone()
        {
            return new DependencyContext( scopesToCollect, scopesToResolve, aggregating );
        }

        void checkForUpdate( MavenSession session )
            throws LifecycleExecutionException
        {
            if ( lastProject == session.getCurrentProject() )
            {
                if ( lastDependencyArtifacts != lastProject.getDependencyArtifacts()
                    || ( lastDependencyArtifacts != null && lastDependencyArtifactCount != lastDependencyArtifacts.size() ) )
                {
                    logger.debug( "Re-resolving dependencies for project " + lastProject.getId()
                        + " to account for updates by previous goal execution" );
                    resolveProjectDependencies( lastProject, scopesToCollect, scopesToResolve, session, aggregating );
                }
            }

            lastProject = session.getCurrentProject();
            lastDependencyArtifacts = lastProject.getDependencyArtifacts();
            lastDependencyArtifactCount = ( lastDependencyArtifacts != null ) ? lastDependencyArtifacts.size() : 0;
        }
    }

    private void execute( MavenSession session, MojoExecution mojoExecution, ProjectIndex projectIndex,
                          DependencyContext dependencyContext )
        throws LifecycleExecutionException
    {
        MojoDescriptor mojoDescriptor = mojoExecution.getMojoDescriptor();

        if ( mojoDescriptor.isProjectRequired() && !session.isUsingPOMsFromFilesystem() )
        {
            Throwable cause =
                new IllegalStateException( "Goal requires a project to execute but there is no POM in this build." );
            throw new LifecycleExecutionException( mojoExecution, null, cause );
        }

        if ( mojoDescriptor.isOnlineRequired() && session.isOffline() )
        {
            if ( MojoExecution.Source.CLI.equals( mojoExecution.getSource() ) )
            {
                Throwable cause =
                    new IllegalStateException( "Goal requires online mode for execution"
                        + " but Maven is currently offline." );
                throw new LifecycleExecutionException( mojoExecution, session.getCurrentProject(), cause );
            }
            else
            {
                fireEvent( session, mojoExecution, LifecycleEventCatapult.MOJO_SKIPPED );

                return;
            }
        }

        dependencyContext.checkForUpdate( session );

        List<MavenProject> forkedProjects =
            executeForkedExecutions( mojoExecution, session, projectIndex, dependencyContext );

        fireEvent( session, mojoExecution, LifecycleEventCatapult.MOJO_STARTED );

        try
        {
            try
            {
                pluginManager.executeMojo( session, mojoExecution );
            }
            catch ( MojoFailureException e )
            {
                throw new LifecycleExecutionException( mojoExecution, session.getCurrentProject(), e );
            }
            catch ( MojoExecutionException e )
            {
                throw new LifecycleExecutionException( mojoExecution, session.getCurrentProject(), e );
            }
            catch ( PluginConfigurationException e )
            {
                throw new LifecycleExecutionException( mojoExecution, session.getCurrentProject(), e );
            }
            catch ( PluginManagerException e )
            {
                throw new LifecycleExecutionException( mojoExecution, session.getCurrentProject(), e );
            }

            fireEvent( session, mojoExecution, LifecycleEventCatapult.MOJO_SUCCEEDED );
        }
        catch ( LifecycleExecutionException e )
        {
            fireEvent( session, mojoExecution, LifecycleEventCatapult.MOJO_FAILED );

            throw e;
        }
        finally
        {
            for ( MavenProject forkedProject : forkedProjects )
            {
                forkedProject.setExecutionProject( null );
            }
        }
    }

    public List<MavenProject> executeForkedExecutions( MojoExecution mojoExecution, MavenSession session )
        throws LifecycleExecutionException
    {
        return executeForkedExecutions( mojoExecution, session, new ProjectIndex( session.getProjects() ),
                                        new DependencyContext( mojoExecution ) );
    }

    private List<MavenProject> executeForkedExecutions( MojoExecution mojoExecution, MavenSession session,
                                                        ProjectIndex projectIndex, DependencyContext dependencyContext )
        throws LifecycleExecutionException
    {
        List<MavenProject> forkedProjects = Collections.emptyList();

        Map<String, List<MojoExecution>> forkedExecutions = mojoExecution.getForkedExecutions();

        if ( !forkedExecutions.isEmpty() )
        {
            fireEvent( session, mojoExecution, LifecycleEventCatapult.FORK_STARTED );

            MavenProject project = session.getCurrentProject();

            forkedProjects = new ArrayList<MavenProject>( forkedExecutions.size() );

            dependencyContext = dependencyContext.clone();

            try
            {
                for ( Map.Entry<String, List<MojoExecution>> fork : forkedExecutions.entrySet() )
                {
                    int index = projectIndex.indices.get( fork.getKey() );

                    MavenProject forkedProject = projectIndex.projects.get( fork.getKey() );

                    forkedProjects.add( forkedProject );

                    MavenProject executedProject = forkedProject.clone();

                    forkedProject.setExecutionProject( executedProject );

                    try
                    {
                        session.setCurrentProject( executedProject );
                        session.getProjects().set( index, executedProject );
                        projectIndex.projects.put( fork.getKey(), executedProject );

                        for ( MojoExecution forkedExecution : fork.getValue() )
                        {
                            execute( session, forkedExecution, projectIndex, dependencyContext );
                        }
                    }
                    finally
                    {
                        projectIndex.projects.put( fork.getKey(), forkedProject );
                        session.getProjects().set( index, forkedProject );
                        session.setCurrentProject( project );
                    }
                }

                fireEvent( session, mojoExecution, LifecycleEventCatapult.FORK_SUCCEEDED );
            }
            catch ( LifecycleExecutionException e )
            {
                fireEvent( session, mojoExecution, LifecycleEventCatapult.FORK_FAILED );

                throw e;
            }
        }

        return forkedProjects;
    }

    private static final class ProjectIndex
    {

        Map<String, MavenProject> projects;

        Map<String, Integer> indices;

        ProjectIndex( List<MavenProject> projects )
        {
            this.projects = new HashMap<String, MavenProject>( projects.size() * 2 );
            this.indices = new HashMap<String, Integer>( projects.size() * 2 );

            for ( int i = 0; i < projects.size(); i++ )
            {
                MavenProject project = projects.get( i );
                String key = getKey( project );

                this.projects.put( key, project );
                this.indices.put( key, Integer.valueOf( i ) );
            }
        }

    }

    private List<ProjectBuild> calculateProjectBuilds( MavenSession session )
        throws PluginNotFoundException, PluginResolutionException, PluginDescriptorParsingException,
        MojoNotFoundException, NoPluginFoundForPrefixException, InvalidPluginDescriptorException,
        PluginVersionResolutionException
    {
        List<ProjectBuild> projectBuilds = new ArrayList<ProjectBuild>();

        MavenProject rootProject = session.getTopLevelProject();

        List<String> tasks = session.getGoals();

        if ( tasks == null || tasks.isEmpty() )
        {
            if ( !StringUtils.isEmpty( rootProject.getDefaultGoal() ) )
            {
                tasks = Collections.singletonList( rootProject.getDefaultGoal() );
            }
        }

        List<TaskSegment> taskSegments = calculateTaskSegments( session, tasks );

        for ( TaskSegment taskSegment : taskSegments )
        {
            List<MavenProject> projects;

            if ( taskSegment.aggregating )
            {
                projects = Collections.singletonList( rootProject );
            }
            else
            {
                projects = session.getProjects();
            }

            for ( MavenProject project : projects )
            {
                projectBuilds.add( new ProjectBuild( project, taskSegment ) );
            }
        }

        return projectBuilds;
    }

    private MavenExecutionPlan calculateExecutionPlan( MavenSession session, MavenProject project,
                                                       TaskSegment taskSegment )
        throws PluginNotFoundException, PluginResolutionException, LifecyclePhaseNotFoundException,
        PluginDescriptorParsingException, MojoNotFoundException, InvalidPluginDescriptorException,
        NoPluginFoundForPrefixException, LifecycleNotFoundException, PluginVersionResolutionException
    {
        resolveMissingPluginVersions( project, session );

        List<MojoExecution> mojoExecutions = new ArrayList<MojoExecution>();

        Set<String> requiredDependencyResolutionScopes = new TreeSet<String>();

        Set<String> requiredDependencyCollectionScopes = new TreeSet<String>();

        for ( Object task : taskSegment.tasks )
        {
            if ( task instanceof GoalTask )
            {
                MojoDescriptor mojoDescriptor = ( (GoalTask) task ).mojoDescriptor;

                MojoExecution mojoExecution =
                    new MojoExecution( mojoDescriptor, "default-cli", MojoExecution.Source.CLI );

                mojoExecutions.add( mojoExecution );
            }
            else if ( task instanceof LifecycleTask )
            {
                String lifecyclePhase = ( (LifecycleTask) task ).lifecyclePhase;

                Map<String, List<MojoExecution>> phaseToMojoMapping =
                    calculateLifecycleMappings( session, project, lifecyclePhase );

                for ( List<MojoExecution> mojoExecutionsFromLifecycle : phaseToMojoMapping.values() )
                {
                    mojoExecutions.addAll( mojoExecutionsFromLifecycle );
                }
            }
            else
            {
                throw new IllegalStateException( "unexpected task " + task );
            }
        }

        for ( MojoExecution mojoExecution : mojoExecutions )
        {
            MojoDescriptor mojoDescriptor = mojoExecution.getMojoDescriptor();

            if ( mojoDescriptor == null )
            {
                mojoDescriptor =
                    pluginManager.getMojoDescriptor( mojoExecution.getPlugin(), mojoExecution.getGoal(),
                                                     getRepositoryRequest( session, project ) );

                mojoExecution.setMojoDescriptor( mojoDescriptor );
            }

            populateMojoExecutionConfiguration( project, mojoExecution,
                                                MojoExecution.Source.CLI.equals( mojoExecution.getSource() ) );

            finalizeMojoConfiguration( mojoExecution );

            calculateForkedExecutions( mojoExecution, session, project, new HashSet<MojoDescriptor>() );

            collectDependencyRequirements( requiredDependencyResolutionScopes, requiredDependencyCollectionScopes,
                                           mojoExecution );
        }

        return new MavenExecutionPlan( mojoExecutions, requiredDependencyResolutionScopes,
                                       requiredDependencyCollectionScopes );
    }

    private List<TaskSegment> calculateTaskSegments( MavenSession session, List<String> tasks )
        throws PluginNotFoundException, PluginResolutionException, PluginDescriptorParsingException,
        MojoNotFoundException, NoPluginFoundForPrefixException, InvalidPluginDescriptorException,
        PluginVersionResolutionException
    {
        List<TaskSegment> taskSegments = new ArrayList<TaskSegment>( tasks.size() );

        TaskSegment currentSegment = null;

        for ( String task : tasks )
        {
            if ( isGoalSpecification( task ) )
            {
                // "pluginPrefix:goal" or "groupId:artifactId[:version]:goal"

                MojoDescriptor mojoDescriptor = getMojoDescriptor( task, session, session.getTopLevelProject() );

                boolean aggregating = mojoDescriptor.isAggregating();

                if ( currentSegment == null || currentSegment.aggregating != aggregating )
                {
                    currentSegment = new TaskSegment( aggregating );
                    taskSegments.add( currentSegment );
                }

                currentSegment.tasks.add( new GoalTask( mojoDescriptor ) );
            }
            else
            {
                // lifecycle phase

                if ( currentSegment == null || currentSegment.aggregating )
                {
                    currentSegment = new TaskSegment( false );
                    taskSegments.add( currentSegment );
                }

                currentSegment.tasks.add( new LifecycleTask( task ) );
            }
        }

        return taskSegments;
    }

    private boolean isGoalSpecification( String task )
    {
        return task.indexOf( ':' ) >= 0;
    }

    private static final class ProjectBuild
    {

        final MavenProject project;

        final TaskSegment taskSegment;

        ProjectBuild( MavenProject project, TaskSegment taskSegment )
        {
            this.project = project;
            this.taskSegment = taskSegment;
        }

        @Override
        public String toString()
        {
            return project.getId() + " -> " + taskSegment;
        }

    }

    private static final class TaskSegment
    {

        final List<Object> tasks;

        final boolean aggregating;

        TaskSegment( boolean aggregating )
        {
            this.aggregating = aggregating;
            tasks = new ArrayList<Object>();
        }

        @Override
        public String toString()
        {
            return tasks.toString();
        }

    }

    private static final class GoalTask
    {

        final MojoDescriptor mojoDescriptor;

        GoalTask( MojoDescriptor mojoDescriptor )
        {
            this.mojoDescriptor = mojoDescriptor;
        }

        @Override
        public String toString()
        {
            return mojoDescriptor.getId();
        }

    }

    private static final class LifecycleTask
    {

        final String lifecyclePhase;

        LifecycleTask( String lifecyclePhase )
        {
            this.lifecyclePhase = lifecyclePhase;
        }

        @Override
        public String toString()
        {
            return lifecyclePhase;
        }

    }

    public MavenExecutionPlan calculateExecutionPlan( MavenSession session, String... tasks )
        throws PluginNotFoundException, PluginResolutionException, PluginDescriptorParsingException,
        MojoNotFoundException, NoPluginFoundForPrefixException, InvalidPluginDescriptorException,
        PluginManagerException, LifecyclePhaseNotFoundException, LifecycleNotFoundException,
        PluginVersionResolutionException
    {
        List<TaskSegment> taskSegments = calculateTaskSegments( session, Arrays.asList( tasks ) );

        TaskSegment mergedSegment = new TaskSegment( false );

        for ( TaskSegment taskSegment : taskSegments )
        {
            mergedSegment.tasks.addAll( taskSegment.tasks );
        }

        return calculateExecutionPlan( session, session.getCurrentProject(), mergedSegment );
    }

    private RepositoryRequest getRepositoryRequest( MavenSession session, MavenProject project )
    {
        RepositoryRequest request = new DefaultRepositoryRequest();

        request.setCache( session.getRepositoryCache() );
        request.setLocalRepository( session.getLocalRepository() );
        if ( project != null )
        {
            request.setRemoteRepositories( project.getPluginArtifactRepositories() );
        }
        request.setOffline( session.isOffline() );
        request.setTransferListener( session.getRequest().getTransferListener() );

        return request;
    }

    private void collectDependencyRequirements( Collection<String> requiredDependencyResolutionScopes,
                                                    Collection<String> requiredDependencyCollectionScopes,
                                                    MojoExecution mojoExecution )
    {
        MojoDescriptor mojoDescriptor = mojoExecution.getMojoDescriptor();

        String requiredDependencyResolutionScope = mojoDescriptor.getDependencyResolutionRequired();

        if ( StringUtils.isNotEmpty( requiredDependencyResolutionScope ) )
        {
            requiredDependencyResolutionScopes.add( requiredDependencyResolutionScope );
        }

        String requiredDependencyCollectionScope = mojoDescriptor.getDependencyCollectionRequired();

        if ( StringUtils.isNotEmpty( requiredDependencyCollectionScope ) )
        {
            requiredDependencyCollectionScopes.add( requiredDependencyCollectionScope );
        }

        for ( List<MojoExecution> forkedExecutions : mojoExecution.getForkedExecutions().values() )
        {
            for ( MojoExecution forkedExecution : forkedExecutions )
            {
                collectDependencyRequirements( requiredDependencyResolutionScopes,
                                                   requiredDependencyCollectionScopes, forkedExecution );
            }
        }
    }

    private Map<String, List<MojoExecution>> calculateLifecycleMappings( MavenSession session, MavenProject project,
                                                                         String lifecyclePhase )
        throws LifecyclePhaseNotFoundException, PluginNotFoundException, PluginResolutionException,
        PluginDescriptorParsingException, MojoNotFoundException, InvalidPluginDescriptorException
    {
        /*
         * Determine the lifecycle that corresponds to the given phase.
         */

        Lifecycle lifecycle = phaseToLifecycleMap.get( lifecyclePhase );

        if ( lifecycle == null )
        {
            throw new LifecyclePhaseNotFoundException( lifecyclePhase );
        }

        /*
         * Initialize mapping from lifecycle phase to bound mojos. The key set of this map denotes the phases the caller
         * is interested in, i.e. all phases up to and including the specified phase.
         */

        Map<String, Map<Integer, List<MojoExecution>>> mappings =
            new LinkedHashMap<String, Map<Integer, List<MojoExecution>>>();

        for ( String phase : lifecycle.getPhases() )
        {
            Map<Integer, List<MojoExecution>> phaseBindings = new TreeMap<Integer, List<MojoExecution>>();

            mappings.put( phase, phaseBindings );

            if ( phase.equals( lifecyclePhase ) )
            {
                break;
            }
        }

        /*
         * Grab plugin executions that are bound to the selected lifecycle phases from project. The effective model of
         * the project already contains the plugin executions induced by the project's packaging type. Remember, all
         * phases of interest and only those are in the lifecyle mapping, if a phase has no value in the map, we are not
         * interested in any of the executions bound to it.
         */

        for ( Plugin plugin : project.getBuild().getPlugins() )
        {
            for ( PluginExecution execution : plugin.getExecutions() )
            {
                // if the phase is specified then I don't have to go fetch the plugin yet and pull it down
                // to examine the phase it is associated to.
                if ( execution.getPhase() != null )
                {
                    Map<Integer, List<MojoExecution>> phaseBindings = mappings.get( execution.getPhase() );
                    if ( phaseBindings != null )
                    {
                        for ( String goal : execution.getGoals() )
                        {
                            MojoExecution mojoExecution = new MojoExecution( plugin, goal, execution.getId() );
                            addMojoExecution( phaseBindings, mojoExecution, execution.getPriority() );
                        }
                    }
                }
                // if not then i need to grab the mojo descriptor and look at the phase that is specified
                else
                {
                    for ( String goal : execution.getGoals() )
                    {
                        MojoDescriptor mojoDescriptor =
                            pluginManager.getMojoDescriptor( plugin, goal, getRepositoryRequest( session, project ) );

                        Map<Integer, List<MojoExecution>> phaseBindings = mappings.get( mojoDescriptor.getPhase() );
                        if ( phaseBindings != null )
                        {
                            MojoExecution mojoExecution = new MojoExecution( mojoDescriptor, execution.getId() );
                            addMojoExecution( phaseBindings, mojoExecution, execution.getPriority() );
                        }
                    }
                }
            }
        }

        Map<String, List<MojoExecution>> lifecycleMappings = new LinkedHashMap<String, List<MojoExecution>>();

        for ( Map.Entry<String, Map<Integer, List<MojoExecution>>> entry : mappings.entrySet() )
        {
            List<MojoExecution> mojoExecutions = new ArrayList<MojoExecution>();

            for ( List<MojoExecution> executions : entry.getValue().values() )
            {
                mojoExecutions.addAll( executions );
            }

            lifecycleMappings.put( entry.getKey(), mojoExecutions );
        }

        return lifecycleMappings;
    }

    private void addMojoExecution( Map<Integer, List<MojoExecution>> phaseBindings, MojoExecution mojoExecution,
                                   int priority )
    {
        List<MojoExecution> mojoExecutions = phaseBindings.get( priority );

        if ( mojoExecutions == null )
        {
            mojoExecutions = new ArrayList<MojoExecution>();
            phaseBindings.put( priority, mojoExecutions );
        }

        mojoExecutions.add( mojoExecution );
    }

    public void calculateForkedExecutions( MojoExecution mojoExecution, MavenSession session )
        throws MojoNotFoundException, PluginNotFoundException, PluginResolutionException,
        PluginDescriptorParsingException, NoPluginFoundForPrefixException, InvalidPluginDescriptorException,
        LifecyclePhaseNotFoundException, LifecycleNotFoundException, PluginVersionResolutionException
    {
        calculateForkedExecutions( mojoExecution, session, session.getCurrentProject(), new HashSet<MojoDescriptor>() );
    }

    private void calculateForkedExecutions( MojoExecution mojoExecution, MavenSession session, MavenProject project,
                                            Collection<MojoDescriptor> alreadyForkedExecutions )
        throws MojoNotFoundException, PluginNotFoundException, PluginResolutionException,
        PluginDescriptorParsingException, NoPluginFoundForPrefixException, InvalidPluginDescriptorException,
        LifecyclePhaseNotFoundException, LifecycleNotFoundException, PluginVersionResolutionException
    {
        MojoDescriptor mojoDescriptor = mojoExecution.getMojoDescriptor();

        if ( !mojoDescriptor.isForking() )
        {
            return;
        }

        if ( !alreadyForkedExecutions.add( mojoDescriptor ) )
        {
            return;
        }

        List<MavenProject> forkedProjects;

        if ( mojoDescriptor.isAggregating() )
        {
            forkedProjects = session.getProjects();
        }
        else
        {
            forkedProjects = Collections.singletonList( project );
        }

        for ( MavenProject forkedProject : forkedProjects )
        {
            List<MojoExecution> forkedExecutions;

            if ( StringUtils.isNotEmpty( mojoDescriptor.getExecutePhase() ) )
            {
                forkedExecutions =
                    calculateForkedLifecycle( mojoExecution, session, forkedProject, alreadyForkedExecutions );
            }
            else
            {
                forkedExecutions = calculateForkedGoal( mojoExecution, session, forkedProject, alreadyForkedExecutions );
            }

            mojoExecution.setForkedExecutions( getKey( forkedProject ), forkedExecutions );
        }

        alreadyForkedExecutions.remove( mojoDescriptor );
    }

    private List<MojoExecution> calculateForkedGoal( MojoExecution mojoExecution, MavenSession session,
                                                     MavenProject project,
                                                     Collection<MojoDescriptor> alreadyForkedExecutions )
        throws MojoNotFoundException, PluginNotFoundException, PluginResolutionException,
        PluginDescriptorParsingException, NoPluginFoundForPrefixException, InvalidPluginDescriptorException,
        LifecyclePhaseNotFoundException, LifecycleNotFoundException, PluginVersionResolutionException
    {
        MojoDescriptor mojoDescriptor = mojoExecution.getMojoDescriptor();

        PluginDescriptor pluginDescriptor = mojoDescriptor.getPluginDescriptor();

        String forkedGoal = mojoDescriptor.getExecuteGoal();

        MojoDescriptor forkedMojoDescriptor = pluginDescriptor.getMojo( forkedGoal );
        if ( forkedMojoDescriptor == null )
        {
            throw new MojoNotFoundException( forkedGoal, pluginDescriptor );
        }

        if ( alreadyForkedExecutions.contains( forkedMojoDescriptor ) )
        {
            return Collections.emptyList();
        }

        MojoExecution forkedExecution = new MojoExecution( forkedMojoDescriptor, forkedGoal );

        populateMojoExecutionConfiguration( project, forkedExecution, true );

        finalizeMojoConfiguration( forkedExecution );

        calculateForkedExecutions( forkedExecution, session, project, alreadyForkedExecutions );

        return Collections.singletonList( forkedExecution );
    }

    private List<MojoExecution> calculateForkedLifecycle( MojoExecution mojoExecution, MavenSession session,
                                                          MavenProject project,
                                                          Collection<MojoDescriptor> alreadyForkedExecutions )
        throws MojoNotFoundException, PluginNotFoundException, PluginResolutionException,
        PluginDescriptorParsingException, NoPluginFoundForPrefixException, InvalidPluginDescriptorException,
        LifecyclePhaseNotFoundException, LifecycleNotFoundException, PluginVersionResolutionException
    {
        MojoDescriptor mojoDescriptor = mojoExecution.getMojoDescriptor();

        String forkedPhase = mojoDescriptor.getExecutePhase();

        Map<String, List<MojoExecution>> lifecycleMappings = calculateLifecycleMappings( session, project, forkedPhase );

        for ( List<MojoExecution> forkedExecutions : lifecycleMappings.values() )
        {
            for ( MojoExecution forkedExecution : forkedExecutions )
            {
                if ( forkedExecution.getMojoDescriptor() == null )
                {
                    MojoDescriptor forkedMojoDescriptor =
                        pluginManager.getMojoDescriptor( forkedExecution.getPlugin(), forkedExecution.getGoal(),
                                                         getRepositoryRequest( session, project ) );

                    forkedExecution.setMojoDescriptor( forkedMojoDescriptor );
                }

                populateMojoExecutionConfiguration( project, forkedExecution, false );
            }
        }

        injectLifecycleOverlay( lifecycleMappings, mojoExecution, session, project );

        List<MojoExecution> mojoExecutions = new ArrayList<MojoExecution>();

        for ( List<MojoExecution> forkedExecutions : lifecycleMappings.values() )
        {
            for ( MojoExecution forkedExecution : forkedExecutions )
            {
                if ( !alreadyForkedExecutions.contains( forkedExecution.getMojoDescriptor() ) )
                {
                    finalizeMojoConfiguration( forkedExecution );

                    calculateForkedExecutions( forkedExecution, session, project, alreadyForkedExecutions );

                    mojoExecutions.add( forkedExecution );
                }
            }
        }

        return mojoExecutions;
    }

    private void injectLifecycleOverlay( Map<String, List<MojoExecution>> lifecycleMappings,
                                         MojoExecution mojoExecution, MavenSession session, MavenProject project )
        throws PluginDescriptorParsingException, LifecycleNotFoundException, MojoNotFoundException,
        PluginNotFoundException, PluginResolutionException, NoPluginFoundForPrefixException,
        InvalidPluginDescriptorException, PluginVersionResolutionException
    {
        MojoDescriptor mojoDescriptor = mojoExecution.getMojoDescriptor();

        PluginDescriptor pluginDescriptor = mojoDescriptor.getPluginDescriptor();

        String forkedLifecycle = mojoDescriptor.getExecuteLifecycle();

        if ( StringUtils.isEmpty( forkedLifecycle ) )
        {
            return;
        }

        org.apache.maven.plugin.lifecycle.Lifecycle lifecycleOverlay;

        try
        {
            lifecycleOverlay = pluginDescriptor.getLifecycleMapping( forkedLifecycle );
        }
        catch ( IOException e )
        {
            throw new PluginDescriptorParsingException( pluginDescriptor.getPlugin(), pluginDescriptor.getSource(), e );
        }
        catch ( XmlPullParserException e )
        {
            throw new PluginDescriptorParsingException( pluginDescriptor.getPlugin(), pluginDescriptor.getSource(), e );
        }

        if ( lifecycleOverlay == null )
        {
            throw new LifecycleNotFoundException( forkedLifecycle );
        }

        for ( Phase phase : lifecycleOverlay.getPhases() )
        {
            List<MojoExecution> forkedExecutions = lifecycleMappings.get( phase.getId() );

            if ( forkedExecutions != null )
            {
                for ( Execution execution : phase.getExecutions() )
                {
                    for ( String goal : execution.getGoals() )
                    {
                        MojoDescriptor forkedMojoDescriptor;

                        if ( goal.indexOf( ':' ) < 0 )
                        {
                            forkedMojoDescriptor = pluginDescriptor.getMojo( goal );
                            if ( forkedMojoDescriptor == null )
                            {
                                throw new MojoNotFoundException( goal, pluginDescriptor );
                            }
                        }
                        else
                        {
                            forkedMojoDescriptor = getMojoDescriptor( goal, session, project );
                        }

                        MojoExecution forkedExecution =
                            new MojoExecution( forkedMojoDescriptor, mojoExecution.getExecutionId() );

                        Xpp3Dom forkedConfiguration = (Xpp3Dom) execution.getConfiguration();

                        forkedExecution.setConfiguration( forkedConfiguration );

                        populateMojoExecutionConfiguration( project, forkedExecution, true );

                        forkedExecutions.add( forkedExecution );
                    }
                }

                Xpp3Dom phaseConfiguration = (Xpp3Dom) phase.getConfiguration();

                if ( phaseConfiguration != null )
                {
                    for ( MojoExecution forkedExecution : forkedExecutions )
                    {
                        Xpp3Dom forkedConfiguration = forkedExecution.getConfiguration();

                        forkedConfiguration = Xpp3Dom.mergeXpp3Dom( phaseConfiguration, forkedConfiguration );

                        forkedExecution.setConfiguration( forkedConfiguration );
                    }
                }
            }
        }
    }

    private void populateMojoExecutionConfiguration( MavenProject project, MojoExecution mojoExecution,
                                                     boolean allowPluginLevelConfig )
    {
        String g = mojoExecution.getGroupId();

        String a = mojoExecution.getArtifactId();

        Plugin plugin = findPlugin( g, a, project.getBuildPlugins() );

        if ( plugin == null && project.getPluginManagement() != null )
        {
            plugin = findPlugin( g, a, project.getPluginManagement().getPlugins() );
        }

        if ( plugin != null )
        {
            PluginExecution pluginExecution =
                findPluginExecution( mojoExecution.getExecutionId(), plugin.getExecutions() );

            Xpp3Dom pomConfiguration = null;

            if ( pluginExecution != null )
            {
                pomConfiguration = (Xpp3Dom) pluginExecution.getConfiguration();
            }
            else if ( allowPluginLevelConfig )
            {
                pomConfiguration = (Xpp3Dom) plugin.getConfiguration();
            }

            Xpp3Dom mojoConfiguration = ( pomConfiguration != null ) ? new Xpp3Dom( pomConfiguration ) : null;

            mojoConfiguration = Xpp3Dom.mergeXpp3Dom( mojoExecution.getConfiguration(), mojoConfiguration );

            mojoExecution.setConfiguration( mojoConfiguration );
        }
    }

    /**
     * Post-processes the effective configuration for the specified mojo execution. This step discards all parameters
     * from the configuration that are not applicable to the mojo and injects the default values for any missing
     * parameters.
     * 
     * @param mojoExecution The mojo execution whose configuration should be finalized, must not be {@code null}.
     */
    private void finalizeMojoConfiguration( MojoExecution mojoExecution )
    {
        MojoDescriptor mojoDescriptor = mojoExecution.getMojoDescriptor();

        Xpp3Dom executionConfiguration = mojoExecution.getConfiguration();
        if ( executionConfiguration == null )
        {
            executionConfiguration = new Xpp3Dom( "configuration" );
        }

        Xpp3Dom defaultConfiguration = getMojoConfiguration( mojoDescriptor );

        Xpp3Dom finalConfiguration = new Xpp3Dom( "configuration" );

        if ( mojoDescriptor.getParameters() != null )
        {
            for ( Parameter parameter : mojoDescriptor.getParameters() )
            {
                Xpp3Dom parameterConfiguration = executionConfiguration.getChild( parameter.getName() );

                if ( parameterConfiguration == null )
                {
                    parameterConfiguration = executionConfiguration.getChild( parameter.getAlias() );
                }

                Xpp3Dom parameterDefaults = defaultConfiguration.getChild( parameter.getName() );

                parameterConfiguration = Xpp3Dom.mergeXpp3Dom( parameterConfiguration, parameterDefaults, Boolean.TRUE );

                if ( parameterConfiguration != null )
                {
                    parameterConfiguration = new Xpp3Dom( parameterConfiguration, parameter.getName() );

                    if ( StringUtils.isEmpty( parameterConfiguration.getAttribute( "implementation" ) )
                        && StringUtils.isNotEmpty( parameter.getImplementation() ) )
                    {
                        parameterConfiguration.setAttribute( "implementation", parameter.getImplementation() );
                    }

                    finalConfiguration.addChild( parameterConfiguration );
                }
            }
        }

        mojoExecution.setConfiguration( finalConfiguration );
    }

    // org.apache.maven.plugins:maven-remote-resources-plugin:1.0:process
    MojoDescriptor getMojoDescriptor( String task, MavenSession session, MavenProject project ) 
        throws PluginNotFoundException, PluginResolutionException, PluginDescriptorParsingException, MojoNotFoundException, NoPluginFoundForPrefixException, InvalidPluginDescriptorException, PluginVersionResolutionException
    {        
        String goal = null;
        
        Plugin plugin = null;

        StringTokenizer tok = new StringTokenizer( task, ":" );
        
        int numTokens = tok.countTokens();
        
        if ( numTokens == 4 )
        {
            // We have everything that we need
            //
            // org.apache.maven.plugins:maven-remote-resources-plugin:1.0:process
            //
            // groupId
            // artifactId
            // version
            // goal
            //
            plugin = new Plugin();
            plugin.setGroupId( tok.nextToken() );
            plugin.setArtifactId( tok.nextToken() );
            plugin.setVersion( tok.nextToken() );
            goal = tok.nextToken();
            
        }
        else if ( numTokens == 3 )
        {
            // We have everything that we need except the version
            //
            // org.apache.maven.plugins:maven-remote-resources-plugin:???:process
            //
            // groupId
            // artifactId
            // ???
            // goal
            //
            plugin = new Plugin();
            plugin.setGroupId( tok.nextToken() );
            plugin.setArtifactId( tok.nextToken() );
            goal = tok.nextToken();
        }
        else if ( numTokens == 2 )
        {
            // We have a prefix and goal
            //
            // idea:idea
            //
            String prefix = tok.nextToken();
            goal = tok.nextToken();

            // This is the case where someone has executed a single goal from the command line
            // of the form:
            //
            // mvn remote-resources:process
            //
            // From the metadata stored on the server which has been created as part of a standard
            // Maven plugin deployment we will find the right PluginDescriptor from the remote
            // repository.
            
            plugin = findPluginForPrefix( prefix, session );
        }

        injectPluginDeclarationFromProject( plugin, project );

        RepositoryRequest repositoryRequest = getRepositoryRequest( session, project );

        // If there is no version to be found then we need to look in the repository metadata for
        // this plugin and see what's specified as the latest release.
        //
        if ( plugin.getVersion() == null )
        {
            resolvePluginVersion( plugin, repositoryRequest );
        }

        return pluginManager.getMojoDescriptor( plugin, goal, repositoryRequest );
    }

    private void resolvePluginVersion( Plugin plugin, RepositoryRequest repositoryRequest )
        throws PluginVersionResolutionException
    {
        PluginVersionRequest versionRequest = new DefaultPluginVersionRequest( plugin, repositoryRequest );
        plugin.setVersion( pluginVersionResolver.resolve( versionRequest ).getVersion() );
    }

    private void injectPluginDeclarationFromProject( Plugin plugin, MavenProject project )
    {
        Plugin pluginInPom = findPlugin( plugin, project.getBuildPlugins() );

        if ( pluginInPom == null && project.getPluginManagement() != null )
        {
            pluginInPom = findPlugin( plugin, project.getPluginManagement().getPlugins() );
        }

        if ( pluginInPom != null )
        {
            if ( plugin.getVersion() == null )
            {
                plugin.setVersion( pluginInPom.getVersion() );
            }

            plugin.setDependencies( new ArrayList<Dependency>( pluginInPom.getDependencies() ) );
        }
    }

    private Plugin findPlugin( Plugin plugin, Collection<Plugin> plugins )
    {
        return findPlugin( plugin.getGroupId(), plugin.getArtifactId(), plugins );
    }

    private Plugin findPlugin( String groupId, String artifactId, Collection<Plugin> plugins )
    {
        for ( Plugin plugin : plugins )
        {
            if ( artifactId.equals( plugin.getArtifactId() ) && groupId.equals( plugin.getGroupId() ) )
            {
                return plugin;
            }
        }

        return null;
    }

    private PluginExecution findPluginExecution( String executionId, Collection<PluginExecution> executions )
    {
        if ( StringUtils.isNotEmpty( executionId ) )
        {
            for ( PluginExecution execution : executions )
            {
                if ( executionId.equals( execution.getId() ) )
                {
                    return execution;
                }
            }
        }

        return null;
    }

    public void initialize()
        throws InitializationException
    {
        lifecycleMap = new HashMap<String,Lifecycle>();
        
        // If people are going to make their own lifecycles then we need to tell people how to namespace them correctly so
        // that they don't interfere with internally defined lifecycles.

        phaseToLifecycleMap = new HashMap<String,Lifecycle>();

        for ( Lifecycle lifecycle : lifecycles )
        {                        
            for ( String phase : lifecycle.getPhases() )
            {                
                // The first definition wins.
                if ( !phaseToLifecycleMap.containsKey( phase ) )
                {
                    phaseToLifecycleMap.put( phase, lifecycle );
                }
            }
            
            lifecycleMap.put( lifecycle.getId(), lifecycle );
        }
    }
        
    // These methods deal with construction intact Plugin object that look like they come from a standard
    // <plugin/> block in a Maven POM. We have to do some wiggling to pull the sources of information
    // together and this really shows the problem of constructing a sensible default configuration but
    // it's all encapsulated here so it appears normalized to the POM builder.
    
    // We are going to take the project packaging and find all plugin in the default lifecycle and create
    // fully populated Plugin objects, including executions with goals and default configuration taken
    // from the plugin.xml inside a plugin.
    //
    public Set<Plugin> getPluginsBoundByDefaultToAllLifecycles( String packaging )
    {
        LifecycleMapping lifecycleMappingForPackaging = lifecycleMappings.get( packaging );

        if ( lifecycleMappingForPackaging == null )
        {
            return null;
        }

        Map<Plugin, Plugin> plugins = new LinkedHashMap<Plugin, Plugin>();

        for ( Lifecycle lifecycle : lifecycles )
        {
            org.apache.maven.lifecycle.mapping.Lifecycle lifecycleConfiguration =
                lifecycleMappingForPackaging.getLifecycles().get( lifecycle.getId() );

            Map<String, String> phaseToGoalMapping = null;

            if ( lifecycleConfiguration != null )
            {
                phaseToGoalMapping = lifecycleConfiguration.getPhases();
            }
            else if ( lifecycle.getDefaultPhases() != null )
            {
                phaseToGoalMapping = lifecycle.getDefaultPhases();
            }
            
            if ( phaseToGoalMapping != null )
            {
                // These are of the form:
                //
                // compile -> org.apache.maven.plugins:maven-compiler-plugin:compile[,gid:aid:goal,...]
                //
                for ( Map.Entry<String, String> goalsForLifecyclePhase : phaseToGoalMapping.entrySet() )
                {
                    String phase = goalsForLifecyclePhase.getKey();
                    String goals = goalsForLifecyclePhase.getValue();
                    if ( goals != null )
                    {
                        parseLifecyclePhaseDefinitions( plugins, phase, goals );
                    }
                }
            }
        }

        return plugins.keySet();
    }

    private void parseLifecyclePhaseDefinitions( Map<Plugin, Plugin> plugins, String phase, String goals )
    {
        String[] mojos = StringUtils.split( goals, "," );

        for ( int i = 0; i < mojos.length; i++ )
        {
            // either <groupId>:<artifactId>:<goal> or <groupId>:<artifactId>:<version>:<goal>
            String goal = mojos[i].trim();
            String[] p = StringUtils.split( goal, ":" );

            PluginExecution execution = new PluginExecution();
            execution.setId( "default-" + p[p.length - 1] );
            execution.setPhase( phase );
            execution.setPriority( i - mojos.length );
            execution.getGoals().add( p[p.length - 1] );

            Plugin plugin = new Plugin();
            plugin.setGroupId( p[0] );
            plugin.setArtifactId( p[1] );
            if ( p.length >= 4 )
            {
                plugin.setVersion( p[2] );
            }

            Plugin existing = plugins.get( plugin );
            if ( existing != null )
            {
                plugin = existing;
            }
            else
            {
                plugins.put( plugin, plugin );
            }

            plugin.getExecutions().add( execution );
        }
    }

    private void resolveMissingPluginVersions( MavenProject project, MavenSession session )
        throws PluginVersionResolutionException
    {
        for ( Plugin plugin : project.getBuildPlugins() )
        {
            if ( plugin.getVersion() == null )
            {
                PluginVersionRequest request = new DefaultPluginVersionRequest( plugin, session );
                plugin.setVersion( pluginVersionResolver.resolve( request ).getVersion() );
            }
        }
    }

    public Xpp3Dom getMojoConfiguration( MojoDescriptor mojoDescriptor )
    {
        return convert( mojoDescriptor );
    }
        
    Xpp3Dom convert( MojoDescriptor mojoDescriptor  )
    {
        Xpp3Dom dom = new Xpp3Dom( "configuration" );

        PlexusConfiguration c = mojoDescriptor.getMojoConfiguration();

        PlexusConfiguration[] ces = c.getChildren();

        if ( ces != null )
        {
            for ( PlexusConfiguration ce : ces )
            {
                String value = ce.getValue( null );
                String defaultValue = ce.getAttribute( "default-value", null );
                if ( value != null || defaultValue != null )
                {
                    Xpp3Dom e = new Xpp3Dom( ce.getName() );
                    e.setValue( value );
                    if ( defaultValue != null )
                    {
                        e.setAttribute( "default-value", defaultValue );
                    }
                    dom.addChild( e );
                }
            }
        }

        return dom;
    }

    //TODO: take repo mans into account as one may be aggregating prefixes of many
    //TODO: collect at the root of the repository, read the one at the root, and fetch remote if something is missing
    //      or the user forces the issue
    Plugin findPluginForPrefix( String prefix, MavenSession session )
        throws NoPluginFoundForPrefixException
    {        
        // [prefix]:[goal]
        
        PluginPrefixRequest prefixRequest = new DefaultPluginPrefixRequest( prefix, session );
        PluginPrefixResult prefixResult = pluginPrefixResolver.resolve( prefixRequest );
        
        Plugin plugin = new Plugin();
        plugin.setGroupId( prefixResult.getGroupId() );
        plugin.setArtifactId( prefixResult.getArtifactId() );

        return plugin;
    }

    // These are checks that should be available in real time to IDEs

    /*
    checkRequiredMavenVersion( plugin, localRepository, project.getRemoteArtifactRepositories() );
        // Validate against non-editable (@readonly) parameters, to make sure users aren't trying to override in the POM.
        //validatePomConfiguration( mojoDescriptor, pomConfiguration );            
        //checkDeprecatedParameters( mojoDescriptor, pomConfiguration );
        //checkRequiredParameters( mojoDescriptor, pomConfiguration, expressionEvaluator );        
    
    public void checkRequiredMavenVersion( Plugin plugin, ArtifactRepository localRepository, List<ArtifactRepository> remoteRepositories )
        throws PluginVersionResolutionException, InvalidPluginException
    {
        // if we don't have the required Maven version, then ignore an update
        if ( ( pluginProject.getPrerequisites() != null ) && ( pluginProject.getPrerequisites().getMaven() != null ) )
        {
            DefaultArtifactVersion requiredVersion = new DefaultArtifactVersion( pluginProject.getPrerequisites().getMaven() );

            if ( runtimeInformation.getApplicationInformation().getVersion().compareTo( requiredVersion ) < 0 )
            {
                throw new PluginVersionResolutionException( plugin.getGroupId(), plugin.getArtifactId(), "Plugin requires Maven version " + requiredVersion );
            }
        }
    }
    
   private void checkDeprecatedParameters( MojoDescriptor mojoDescriptor, PlexusConfiguration extractedMojoConfiguration )
        throws PlexusConfigurationException
    {
        if ( ( extractedMojoConfiguration == null ) || ( extractedMojoConfiguration.getChildCount() < 1 ) )
        {
            return;
        }

        List<Parameter> parameters = mojoDescriptor.getParameters();

        if ( ( parameters != null ) && !parameters.isEmpty() )
        {
            for ( Parameter param : parameters )
            {
                if ( param.getDeprecated() != null )
                {
                    boolean warnOfDeprecation = false;
                    PlexusConfiguration child = extractedMojoConfiguration.getChild( param.getName() );

                    if ( ( child != null ) && ( child.getValue() != null ) )
                    {
                        warnOfDeprecation = true;
                    }
                    else if ( param.getAlias() != null )
                    {
                        child = extractedMojoConfiguration.getChild( param.getAlias() );
                        if ( ( child != null ) && ( child.getValue() != null ) )
                        {
                            warnOfDeprecation = true;
                        }
                    }

                    if ( warnOfDeprecation )
                    {
                        StringBuilder buffer = new StringBuilder( 128 );
                        buffer.append( "In mojo: " ).append( mojoDescriptor.getGoal() ).append( ", parameter: " ).append( param.getName() );

                        if ( param.getAlias() != null )
                        {
                            buffer.append( " (alias: " ).append( param.getAlias() ).append( ")" );
                        }

                        buffer.append( " is deprecated:" ).append( "\n\n" ).append( param.getDeprecated() ).append( "\n" );

                        logger.warn( buffer.toString() );
                    }
                }
            }
        }
    }
    
   private void checkRequiredParameters( MojoDescriptor goal, PlexusConfiguration configuration, ExpressionEvaluator expressionEvaluator )
        throws PluginConfigurationException
    {
        // TODO: this should be built in to the configurator, as we presently double process the expressions

        List<Parameter> parameters = goal.getParameters();

        if ( parameters == null )
        {
            return;
        }

        List<Parameter> invalidParameters = new ArrayList<Parameter>();

        for ( int i = 0; i < parameters.size(); i++ )
        {
            Parameter parameter = parameters.get( i );

            if ( parameter.isRequired() )
            {
                // the key for the configuration map we're building.
                String key = parameter.getName();

                Object fieldValue = null;
                String expression = null;
                PlexusConfiguration value = configuration.getChild( key, false );
                try
                {
                    if ( value != null )
                    {
                        expression = value.getValue( null );

                        fieldValue = expressionEvaluator.evaluate( expression );

                        if ( fieldValue == null )
                        {
                            fieldValue = value.getAttribute( "default-value", null );
                        }
                    }

                    if ( ( fieldValue == null ) && StringUtils.isNotEmpty( parameter.getAlias() ) )
                    {
                        value = configuration.getChild( parameter.getAlias(), false );
                        if ( value != null )
                        {
                            expression = value.getValue( null );
                            fieldValue = expressionEvaluator.evaluate( expression );
                            if ( fieldValue == null )
                            {
                                fieldValue = value.getAttribute( "default-value", null );
                            }
                        }
                    }
                }
                catch ( ExpressionEvaluationException e )
                {
                    throw new PluginConfigurationException( goal.getPluginDescriptor(), e.getMessage(), e );
                }

                // only mark as invalid if there are no child nodes
                if ( ( fieldValue == null ) && ( ( value == null ) || ( value.getChildCount() == 0 ) ) )
                {
                    parameter.setExpression( expression );
                    invalidParameters.add( parameter );
                }
            }
        }

        if ( !invalidParameters.isEmpty() )
        {
            throw new PluginParameterException( goal, invalidParameters );
        }
    }

    private void validatePomConfiguration( MojoDescriptor goal, PlexusConfiguration pomConfiguration )
        throws PluginConfigurationException
    {
        List<Parameter> parameters = goal.getParameters();

        if ( parameters == null )
        {
            return;
        }

        for ( int i = 0; i < parameters.size(); i++ )
        {
            Parameter parameter = parameters.get( i );

            // the key for the configuration map we're building.
            String key = parameter.getName();

            PlexusConfiguration value = pomConfiguration.getChild( key, false );

            if ( ( value == null ) && StringUtils.isNotEmpty( parameter.getAlias() ) )
            {
                key = parameter.getAlias();
                value = pomConfiguration.getChild( key, false );
            }

            if ( value != null )
            {
                // Make sure the parameter is either editable/configurable, or else is NOT specified in the POM
                if ( !parameter.isEditable() )
                {
                    StringBuilder errorMessage = new StringBuilder( 128 ).append( "ERROR: Cannot override read-only parameter: " );
                    errorMessage.append( key );
                    errorMessage.append( " in goal: " ).append( goal.getFullGoalName() );

                    throw new PluginConfigurationException( goal.getPluginDescriptor(), errorMessage.toString() );
                }

                String deprecated = parameter.getDeprecated();
                if ( StringUtils.isNotEmpty( deprecated ) )
                {
                    logger.warn( "DEPRECATED [" + parameter.getName() + "]: " + deprecated );
                }
            }
        }
    }
    
    */    

    // USED BY MAVEN HELP PLUGIN
    @Deprecated
    public Map<String, Lifecycle> getPhaseToLifecycleMap()
    {
        return phaseToLifecycleMap;
    }

}
