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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;

import org.apache.maven.ProjectDependenciesResolver;
import org.apache.maven.artifact.repository.DefaultRepositoryRequest;
import org.apache.maven.artifact.repository.RepositoryRequest;
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
        List<ExecutionListener> listeners = session.getRequest().getExecutionListeners();

        if ( !listeners.isEmpty() )
        {
            ExecutionEvent event = new DefaultLifecycleEvent( session, mojoExecution );

            for ( ExecutionListener listener : listeners )
            {
                catapult.fire( listener, event );
            }
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

        RepositoryRequest repositoryRequest = getRepositoryRequest( session, null );

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

                repositoryRequest.setRemoteRepositories( currentProject.getPluginArtifactRepositories() );
                populateDefaultConfigurationForPlugins( currentProject.getBuild().getPlugins(), repositoryRequest );

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
                    repositoryRequest.setRemoteRepositories( project.getRemoteArtifactRepositories() );
                    projectDependenciesResolver.resolve( project, executionPlan.getRequiredResolutionScopes(),
                                                         repositoryRequest );
                }

                for ( MojoExecution mojoExecution : executionPlan.getExecutions() )
                {
                    execute( session, mojoExecution, projectIndex );
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

    private void execute( MavenSession session, MojoExecution mojoExecution, ProjectIndex projectIndex )
        throws MojoFailureException, MojoExecutionException, PluginConfigurationException, PluginManagerException
    {
        MojoDescriptor mojoDescriptor = mojoExecution.getMojoDescriptor();

        if ( mojoDescriptor.isOnlineRequired() && session.isOffline() )
        {
            if ( MojoExecution.Source.CLI.equals( mojoExecution.getSource() ) )
            {
                throw new MojoExecutionException( "Goal " + mojoDescriptor.getGoal()
                    + " requires online mode for execution but Maven is currently offline." );
            }
            else
            {
                fireEvent( session, mojoExecution, LifecycleEventCatapult.MOJO_SKIPPED );

                return;
            }
        }

        List<MavenProject> forkedProjects = Collections.emptyList();

        Map<String, List<MojoExecution>> forkedExecutions = mojoExecution.getForkedExecutions();

        if ( !forkedExecutions.isEmpty() )
        {
            fireEvent( session, mojoExecution, LifecycleEventCatapult.FORK_STARTED );

            MavenProject project = session.getCurrentProject();

            forkedProjects = new ArrayList<MavenProject>( forkedExecutions.size() );

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
                            execute( session, forkedExecution, projectIndex );
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
            catch ( MojoFailureException e )
            {
                fireEvent( session, mojoExecution, LifecycleEventCatapult.FORK_FAILED );

                throw e;
            }
            catch ( MojoExecutionException e )
            {
                fireEvent( session, mojoExecution, LifecycleEventCatapult.FORK_FAILED );

                throw e;
            }
            catch ( PluginConfigurationException e )
            {
                fireEvent( session, mojoExecution, LifecycleEventCatapult.FORK_FAILED );

                throw e;
            }
            catch ( PluginManagerException e )
            {
                fireEvent( session, mojoExecution, LifecycleEventCatapult.FORK_FAILED );

                throw e;
            }
        }

        fireEvent( session, mojoExecution, LifecycleEventCatapult.MOJO_STARTED );

        try
        {
            pluginManager.executeMojo( session, mojoExecution );

            fireEvent( session, mojoExecution, LifecycleEventCatapult.MOJO_SUCCEEDED );
        }
        catch ( MojoFailureException e )
        {
            fireEvent( session, mojoExecution, LifecycleEventCatapult.MOJO_FAILED );

            throw e;
        }
        catch ( MojoExecutionException e )
        {
            fireEvent( session, mojoExecution, LifecycleEventCatapult.MOJO_FAILED );

            throw e;
        }
        catch ( PluginConfigurationException e )
        {
            fireEvent( session, mojoExecution, LifecycleEventCatapult.MOJO_FAILED );

            throw e;
        }
        catch ( PluginManagerException e )
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
        List<MojoExecution> mojoExecutions = new ArrayList<MojoExecution>();

        Set<String> requiredDependencyResolutionScopes = new TreeSet<String>();

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

            extractMojoConfiguration( mojoExecution );

            calculateForkedExecutions( mojoExecution, session, project, new HashSet<MojoDescriptor>() );

            collectDependencyResolutionScopes( requiredDependencyResolutionScopes, mojoExecution );
        }

        return new MavenExecutionPlan( mojoExecutions, requiredDependencyResolutionScopes );
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

        return request;
    }

    private void collectDependencyResolutionScopes( Collection<String> requiredDependencyResolutionScopes,
                                                    MojoExecution mojoExecution )
    {
        String requiredDependencyResolutionScope = mojoExecution.getMojoDescriptor().isDependencyResolutionRequired();

        if ( StringUtils.isNotEmpty( requiredDependencyResolutionScope ) )
        {
            requiredDependencyResolutionScopes.add( requiredDependencyResolutionScope );
        }

        for ( List<MojoExecution> forkedExecutions : mojoExecution.getForkedExecutions().values() )
        {
            for ( MojoExecution forkedExecution : forkedExecutions )
            {
                collectDependencyResolutionScopes( requiredDependencyResolutionScopes, forkedExecution );
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

        Map<String, List<MojoExecution>> lifecycleMappings = new LinkedHashMap<String, List<MojoExecution>>();

        for ( String phase : lifecycle.getPhases() )
        {
            List<MojoExecution> mojoExecutions = new ArrayList<MojoExecution>();

            lifecycleMappings.put( phase, mojoExecutions );

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
                    List<MojoExecution> mojoExecutions = lifecycleMappings.get( execution.getPhase() );
                    if ( mojoExecutions != null )
                    {
                        for ( String goal : execution.getGoals() )
                        {
                            MojoExecution mojoExecution = new MojoExecution( plugin, goal, execution.getId() );
                            mojoExecutions.add( mojoExecution );
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

                        List<MojoExecution> mojoExecutions = lifecycleMappings.get( mojoDescriptor.getPhase() );
                        if ( mojoExecutions != null )
                        {
                            MojoExecution mojoExecution = new MojoExecution( mojoDescriptor, execution.getId() );
                            mojoExecutions.add( mojoExecution );
                        }
                    }
                }
            }
        }

        return lifecycleMappings;
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

            mojoExecution.addForkedExecutions( getKey( forkedProject ), forkedExecutions );
        }
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

        MojoExecution forkedExecution = new MojoExecution( forkedMojoDescriptor, forkedGoal );

        populateMojoExecutionConfiguration( project, forkedExecution, true );

        extractMojoConfiguration( forkedExecution );

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
                extractMojoConfiguration( forkedExecution );

                calculateForkedExecutions( forkedExecution, session, project, alreadyForkedExecutions );

                mojoExecutions.add( forkedExecution );
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
            throw new PluginDescriptorParsingException( pluginDescriptor.getPlugin(), e );
        }
        catch ( XmlPullParserException e )
        {
            throw new PluginDescriptorParsingException( pluginDescriptor.getPlugin(), e );
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

        boolean managedPlugin = false;

        if ( plugin == null && project.getPluginManagement() != null )
        {
            plugin = findPlugin( g, a, project.getPluginManagement().getPlugins() );

            managedPlugin = true;
        }

        MojoDescriptor mojoDescriptor = mojoExecution.getMojoDescriptor();

        if ( plugin != null && StringUtils.isNotEmpty( mojoExecution.getExecutionId() ) )
        {
            for ( PluginExecution e : plugin.getExecutions() )
            {
                if ( mojoExecution.getExecutionId().equals( e.getId() ) )
                {
                    Xpp3Dom executionConfiguration = (Xpp3Dom) e.getConfiguration();

                    Xpp3Dom mojoConfiguration =
                        ( executionConfiguration != null ) ? new Xpp3Dom( executionConfiguration ) : null;

                    mojoConfiguration = Xpp3Dom.mergeXpp3Dom( mojoExecution.getConfiguration(), mojoConfiguration );

                    /*
                     * The model only contains the default configuration for those goals that are present in the plugin
                     * execution. For goals invoked from the CLI or a forked execution, we need to grab the default
                     * parameter values explicitly.
                     */
                    if ( managedPlugin || !e.getGoals().contains( mojoExecution.getGoal() ) )
                    {
                        Xpp3Dom defaultConfiguration = getMojoConfiguration( mojoDescriptor );

                        mojoConfiguration = Xpp3Dom.mergeXpp3Dom( mojoConfiguration, defaultConfiguration );
                    }

                    mojoExecution.setConfiguration( mojoConfiguration );

                    return;
                }
            }
        }

        if ( allowPluginLevelConfig )
        {
            Xpp3Dom defaultConfiguration = getMojoConfiguration( mojoDescriptor );

            Xpp3Dom mojoConfiguration = defaultConfiguration;

            if ( plugin != null && plugin.getConfiguration() != null )
            {
                Xpp3Dom pluginConfiguration = (Xpp3Dom) plugin.getConfiguration();
                pluginConfiguration = new Xpp3Dom( pluginConfiguration );
                mojoConfiguration = Xpp3Dom.mergeXpp3Dom( pluginConfiguration, defaultConfiguration, Boolean.TRUE );
            }

            mojoConfiguration = Xpp3Dom.mergeXpp3Dom( mojoExecution.getConfiguration(), mojoConfiguration );

            mojoExecution.setConfiguration( mojoConfiguration );
        }
    }

    private void extractMojoConfiguration( MojoExecution mojoExecution )
    {
        Xpp3Dom configuration = mojoExecution.getConfiguration();

        configuration = extractMojoConfiguration( configuration, mojoExecution.getMojoDescriptor() );

        mojoExecution.setConfiguration( configuration );
    }

    /**
     * Extracts the configuration for a single mojo from the specified execution configuration by discarding any
     * non-applicable parameters. This is necessary because a plugin execution can have multiple goals with different
     * parametes whose default configurations are all aggregated into the execution configuration. However, the
     * underlying configurator will error out when trying to configure a mojo parameter that is specified in the
     * configuration but not present in the mojo instance.
     * 
     * @param executionConfiguration The configuration from the plugin execution, may be {@code null}.
     * @param mojoDescriptor The descriptor for the mojo being configured, must not be {@code null}.
     * @return The configuration for the mojo, never {@code null}.
     */
    private Xpp3Dom extractMojoConfiguration( Xpp3Dom executionConfiguration, MojoDescriptor mojoDescriptor )
    {
        Xpp3Dom mojoConfiguration = null;

        if ( executionConfiguration != null )
        {
            mojoConfiguration = new Xpp3Dom( executionConfiguration.getName() );

            if ( mojoDescriptor.getParameters() != null )
            {
                for ( Parameter parameter : mojoDescriptor.getParameters() )
                {
                    Xpp3Dom parameterConfiguration = executionConfiguration.getChild( parameter.getName() );

                    if ( parameterConfiguration == null )
                    {
                        parameterConfiguration = executionConfiguration.getChild( parameter.getAlias() );
                    }

                    if ( parameterConfiguration != null )
                    {
                        parameterConfiguration = new Xpp3Dom( parameterConfiguration, parameter.getName() );

                        if ( StringUtils.isNotEmpty( parameter.getImplementation() ) )
                        {
                            parameterConfiguration.setAttribute( "implementation", parameter.getImplementation() );
                        }

                        mojoConfiguration.addChild( parameterConfiguration );
                    }
                }
            }
        }

        return mojoConfiguration;
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
        for ( StringTokenizer tok = new StringTokenizer( goals, "," ); tok.hasMoreTokens(); )
        {
            // either <groupId>:<artifactId>:<goal> or <groupId>:<artifactId>:<version>:<goal>
            String goal = tok.nextToken().trim();
            String[] p = StringUtils.split( goal, ":" );

            PluginExecution execution = new PluginExecution();
            execution.setId( "default-" + p[p.length - 1] );
            execution.setPhase( phase );
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
    
    public void populateDefaultConfigurationForPlugin( Plugin plugin, RepositoryRequest repositoryRequest ) 
        throws LifecycleExecutionException
    {
        if ( plugin.getVersion() == null )
        {
            try
            {
                resolvePluginVersion( plugin, repositoryRequest );
            }
            catch ( PluginVersionResolutionException e )
            {
                throw new LifecycleExecutionException( "Error resolving version for plugin " + plugin.getKey(), e );
            }
        }

        try
        {
            // NOTE: Retrieve the plugin descriptor regardless whether there are any executions to verify the plugin
            PluginDescriptor pluginDescriptor = pluginManager.loadPlugin( plugin, repositoryRequest );

            for ( PluginExecution pluginExecution : plugin.getExecutions() )
            {
                for ( String goal : pluginExecution.getGoals() )
                {
                    MojoDescriptor mojoDescriptor = pluginDescriptor.getMojo( goal );

                    if ( mojoDescriptor == null )
                    {
                        throw new MojoNotFoundException( goal, pluginDescriptor );
                    }

                    Xpp3Dom defaultConfiguration = getMojoConfiguration( mojoDescriptor );

                    Xpp3Dom executionConfiguration =
                        Xpp3Dom.mergeXpp3Dom( (Xpp3Dom) pluginExecution.getConfiguration(), defaultConfiguration,
                                              Boolean.TRUE );

                    pluginExecution.setConfiguration( executionConfiguration );
                }
            }
        }
        catch ( PluginNotFoundException e )
        {
            throw new LifecycleExecutionException( "Error getting plugin information for " + plugin.getId() + ": "
                + e.getMessage(), e );
        }
        catch ( PluginResolutionException e )
        {
            throw new LifecycleExecutionException( "Error getting plugin information for " + plugin.getId() + ": "
                + e.getMessage(), e );
        }
        catch ( PluginDescriptorParsingException e )
        {
            throw new LifecycleExecutionException( "Error getting plugin information for " + plugin.getId() + ": "
                + e.getMessage(), e );
        }
        catch ( MojoNotFoundException e )
        {
            throw new LifecycleExecutionException( "Error getting plugin information for " + plugin.getId() + ": "
                + e.getMessage(), e );
        }
        catch ( InvalidPluginDescriptorException e )
        {
            throw new LifecycleExecutionException( "Error getting plugin information for " + plugin.getId() + ": "
                + e.getMessage(), e );
        }
    }

    public void populateDefaultConfigurationForPlugins( Collection<Plugin> plugins, RepositoryRequest repositoryRequest ) 
        throws LifecycleExecutionException
    {
        for( Plugin plugin : plugins )
        {            
            populateDefaultConfigurationForPlugin( plugin, repositoryRequest );
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
        
        PluginPrefixRequest prefixRequest = new DefaultPluginPrefixRequest( session ).setPrefix( prefix );
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
}
