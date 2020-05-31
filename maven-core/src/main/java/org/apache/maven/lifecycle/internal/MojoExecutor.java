package org.apache.maven.lifecycle.internal;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.resolver.filter.CumulativeScopeArtifactFilter;
import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.LifecycleExecutionException;
import org.apache.maven.lifecycle.MissingProjectException;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MavenPluginManager;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.PluginConfigurationException;
import org.apache.maven.plugin.PluginIncompatibleException;
import org.apache.maven.plugin.PluginManagerException;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.StringUtils;

/**
 * <p>
 * Executes an individual mojo
 * </p>
 * <strong>NOTE:</strong> This class is not part of any public api and can be changed or deleted without prior notice.
 *
 * @author Jason van Zyl
 * @author Benjamin Bentmann
 * @author Kristian Rosenvold
 * @since 3.0
 */
@Named
@Singleton
public class MojoExecutor
{

    @Inject
    private BuildPluginManager pluginManager;

    @Inject
    private MavenPluginManager mavenPluginManager;

    @Inject
    private LifecycleDependencyResolver lifeCycleDependencyResolver;

    @Inject
    private ExecutionEventCatapult eventCatapult;

    public MojoExecutor()
    {
    }

    public DependencyContext newDependencyContext( MavenSession session, List<MojoExecution> mojoExecutions )
    {
        Set<String> scopesToCollect = new TreeSet<>();
        Set<String> scopesToResolve = new TreeSet<>();

        collectDependencyRequirements( scopesToResolve, scopesToCollect, mojoExecutions );

        return new DependencyContext( session.getCurrentProject(), scopesToCollect, scopesToResolve );
    }

    private void collectDependencyRequirements( Set<String> scopesToResolve, Set<String> scopesToCollect,
                                                Collection<MojoExecution> mojoExecutions )
    {
        for ( MojoExecution mojoExecution : mojoExecutions )
        {
            MojoDescriptor mojoDescriptor = mojoExecution.getMojoDescriptor();

            scopesToResolve.addAll( toScopes( mojoDescriptor.getDependencyResolutionRequired() ) );

            scopesToCollect.addAll( toScopes( mojoDescriptor.getDependencyCollectionRequired() ) );
        }
    }

    private Collection<String> toScopes( String classpath )
    {
        Collection<String> scopes = Collections.emptyList();

        if ( StringUtils.isNotEmpty( classpath ) )
        {
            if ( Artifact.SCOPE_COMPILE.equals( classpath ) )
            {
                scopes = Arrays.asList( Artifact.SCOPE_COMPILE, Artifact.SCOPE_SYSTEM, Artifact.SCOPE_PROVIDED );
            }
            else if ( Artifact.SCOPE_RUNTIME.equals( classpath ) )
            {
                scopes = Arrays.asList( Artifact.SCOPE_COMPILE, Artifact.SCOPE_RUNTIME );
            }
            else if ( Artifact.SCOPE_COMPILE_PLUS_RUNTIME.equals( classpath ) )
            {
                scopes = Arrays.asList( Artifact.SCOPE_COMPILE, Artifact.SCOPE_SYSTEM, Artifact.SCOPE_PROVIDED,
                                        Artifact.SCOPE_RUNTIME );
            }
            else if ( Artifact.SCOPE_RUNTIME_PLUS_SYSTEM.equals( classpath ) )
            {
                scopes = Arrays.asList( Artifact.SCOPE_COMPILE, Artifact.SCOPE_SYSTEM, Artifact.SCOPE_RUNTIME );
            }
            else if ( Artifact.SCOPE_TEST.equals( classpath ) )
            {
                scopes = Arrays.asList( Artifact.SCOPE_COMPILE, Artifact.SCOPE_SYSTEM, Artifact.SCOPE_PROVIDED,
                                        Artifact.SCOPE_RUNTIME, Artifact.SCOPE_TEST );
            }
        }
        return Collections.unmodifiableCollection( scopes );
    }

    public void execute( MavenSession session, List<MojoExecution> mojoExecutions, ProjectIndex projectIndex )
        throws LifecycleExecutionException

    {
        DependencyContext dependencyContext = newDependencyContext( session, mojoExecutions );

        PhaseRecorder phaseRecorder = new PhaseRecorder( session.getCurrentProject() );

        for ( MojoExecution mojoExecution : mojoExecutions )
        {
            execute( session, mojoExecution, projectIndex, dependencyContext, phaseRecorder );
        }
    }

    public void execute( MavenSession session, MojoExecution mojoExecution, ProjectIndex projectIndex,
                         DependencyContext dependencyContext, PhaseRecorder phaseRecorder )
        throws LifecycleExecutionException
    {
        execute( session, mojoExecution, projectIndex, dependencyContext );
        phaseRecorder.observeExecution( mojoExecution );
    }

    private void execute( MavenSession session, MojoExecution mojoExecution, ProjectIndex projectIndex,
                          DependencyContext dependencyContext )
        throws LifecycleExecutionException
    {
        MojoDescriptor mojoDescriptor = mojoExecution.getMojoDescriptor();

        try
        {
            mavenPluginManager.checkRequiredMavenVersion( mojoDescriptor.getPluginDescriptor() );
        }
        catch ( PluginIncompatibleException e )
        {
            throw new LifecycleExecutionException( mojoExecution, session.getCurrentProject(), e );
        }

        if ( mojoDescriptor.isProjectRequired() && !session.getRequest().isProjectPresent() )
        {
            Throwable cause = new MissingProjectException(
                "Goal requires a project to execute" + " but there is no POM in this directory ("
                    + session.getExecutionRootDirectory() + ")."
                    + " Please verify you invoked Maven from the correct directory." );
            throw new LifecycleExecutionException( mojoExecution, null, cause );
        }

        if ( mojoDescriptor.isOnlineRequired() && session.isOffline() )
        {
            if ( MojoExecution.Source.CLI.equals( mojoExecution.getSource() ) )
            {
                Throwable cause = new IllegalStateException(
                    "Goal requires online mode for execution" + " but Maven is currently offline." );
                throw new LifecycleExecutionException( mojoExecution, session.getCurrentProject(), cause );
            }
            else
            {
                eventCatapult.fire( ExecutionEvent.Type.MojoSkipped, session, mojoExecution );

                return;
            }
        }

        List<MavenProject> forkedProjects = executeForkedExecutions( mojoExecution, session, projectIndex );

        ensureDependenciesAreResolved( mojoDescriptor, session, dependencyContext );

        eventCatapult.fire( ExecutionEvent.Type.MojoStarted, session, mojoExecution );

        try
        {
            try
            {
                pluginManager.executeMojo( session, mojoExecution );
            }
            catch ( MojoFailureException | PluginManagerException | PluginConfigurationException
                | MojoExecutionException e )
            {
                throw new LifecycleExecutionException( mojoExecution, session.getCurrentProject(), e );
            }

            eventCatapult.fire( ExecutionEvent.Type.MojoSucceeded, session, mojoExecution );
        }
        catch ( LifecycleExecutionException e )
        {
            eventCatapult.fire( ExecutionEvent.Type.MojoFailed, session, mojoExecution, e );

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

    public void ensureDependenciesAreResolved( MojoDescriptor mojoDescriptor, MavenSession session,
                                               DependencyContext dependencyContext )
        throws LifecycleExecutionException

    {
        MavenProject project = dependencyContext.getProject();
        boolean aggregating = mojoDescriptor.isAggregator();

        if ( dependencyContext.isResolutionRequiredForCurrentProject() )
        {
            Collection<String> scopesToCollect = dependencyContext.getScopesToCollectForCurrentProject();
            Collection<String> scopesToResolve = dependencyContext.getScopesToResolveForCurrentProject();

            lifeCycleDependencyResolver.resolveProjectDependencies( project, scopesToCollect, scopesToResolve, session,
                                                                    aggregating, Collections.<Artifact>emptySet() );

            dependencyContext.synchronizeWithProjectState();
        }

        if ( aggregating )
        {
            Collection<String> scopesToCollect = toScopes( mojoDescriptor.getDependencyCollectionRequired() );
            Collection<String> scopesToResolve = toScopes( mojoDescriptor.getDependencyResolutionRequired() );

            if ( dependencyContext.isResolutionRequiredForAggregatedProjects( scopesToCollect, scopesToResolve ) )
            {
                for ( MavenProject aggregatedProject : session.getProjects() )
                {
                    if ( aggregatedProject != project )
                    {
                        lifeCycleDependencyResolver.resolveProjectDependencies( aggregatedProject, scopesToCollect,
                                                                                scopesToResolve, session, aggregating,
                                                                                Collections.<Artifact>emptySet() );
                    }
                }
            }
        }

        ArtifactFilter artifactFilter = getArtifactFilter( mojoDescriptor );
        List<MavenProject> projectsToResolve =
            LifecycleDependencyResolver.getProjects( session.getCurrentProject(), session,
                                                     mojoDescriptor.isAggregator() );
        for ( MavenProject projectToResolve : projectsToResolve )
        {
            projectToResolve.setArtifactFilter( artifactFilter );
        }
    }

    private ArtifactFilter getArtifactFilter( MojoDescriptor mojoDescriptor )
    {
        String scopeToResolve = mojoDescriptor.getDependencyResolutionRequired();
        String scopeToCollect = mojoDescriptor.getDependencyCollectionRequired();

        List<String> scopes = new ArrayList<>( 2 );
        if ( StringUtils.isNotEmpty( scopeToCollect ) )
        {
            scopes.add( scopeToCollect );
        }
        if ( StringUtils.isNotEmpty( scopeToResolve ) )
        {
            scopes.add( scopeToResolve );
        }

        if ( scopes.isEmpty() )
        {
            return null;
        }
        else
        {
            return new CumulativeScopeArtifactFilter( scopes );
        }
    }

    public List<MavenProject> executeForkedExecutions( MojoExecution mojoExecution, MavenSession session,
                                                       ProjectIndex projectIndex )
        throws LifecycleExecutionException
    {
        List<MavenProject> forkedProjects = Collections.emptyList();

        Map<String, List<MojoExecution>> forkedExecutions = mojoExecution.getForkedExecutions();

        if ( !forkedExecutions.isEmpty() )
        {
            eventCatapult.fire( ExecutionEvent.Type.ForkStarted, session, mojoExecution );

            MavenProject project = session.getCurrentProject();

            forkedProjects = new ArrayList<>( forkedExecutions.size() );

            try
            {
                for ( Map.Entry<String, List<MojoExecution>> fork : forkedExecutions.entrySet() )
                {
                    String projectId = fork.getKey();

                    int index = projectIndex.getIndices().get( projectId );

                    MavenProject forkedProject = projectIndex.getProjects().get( projectId );

                    forkedProjects.add( forkedProject );

                    MavenProject executedProject = forkedProject.clone();

                    forkedProject.setExecutionProject( executedProject );

                    List<MojoExecution> mojoExecutions = fork.getValue();

                    if ( mojoExecutions.isEmpty() )
                    {
                        continue;
                    }

                    try
                    {
                        session.setCurrentProject( executedProject );
                        session.getProjects().set( index, executedProject );
                        projectIndex.getProjects().put( projectId, executedProject );

                        eventCatapult.fire( ExecutionEvent.Type.ForkedProjectStarted, session, mojoExecution );

                        execute( session, mojoExecutions, projectIndex );

                        eventCatapult.fire( ExecutionEvent.Type.ForkedProjectSucceeded, session, mojoExecution );
                    }
                    catch ( LifecycleExecutionException e )
                    {
                        eventCatapult.fire( ExecutionEvent.Type.ForkedProjectFailed, session, mojoExecution, e );

                        throw e;
                    }
                    finally
                    {
                        projectIndex.getProjects().put( projectId, forkedProject );
                        session.getProjects().set( index, forkedProject );
                        session.setCurrentProject( project );
                    }
                }

                eventCatapult.fire( ExecutionEvent.Type.ForkSucceeded, session, mojoExecution );
            }
            catch ( LifecycleExecutionException e )
            {
                eventCatapult.fire( ExecutionEvent.Type.ForkFailed, session, mojoExecution, e );

                throw e;
            }
        }

        return forkedProjects;
    }
}
