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
package org.apache.maven.lifecycle.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.resolver.filter.CumulativeScopeArtifactFilter;
import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.internal.MultilineMessageHelper;
import org.apache.maven.lifecycle.LifecycleExecutionException;
import org.apache.maven.lifecycle.MissingProjectException;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MavenPluginManager;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoExecutionRunner;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.MojosExecutionStrategy;
import org.apache.maven.plugin.PluginConfigurationException;
import org.apache.maven.plugin.PluginIncompatibleException;
import org.apache.maven.plugin.PluginManagerException;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.util.StringUtils;
import org.eclipse.aether.SessionData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
@Component(role = MojoExecutor.class)
public class MojoExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(MojoExecutor.class);

    @Requirement
    private BuildPluginManager pluginManager;

    @Requirement
    private MavenPluginManager mavenPluginManager;

    @Requirement
    private LifecycleDependencyResolver lifeCycleDependencyResolver;

    @Requirement
    private ExecutionEventCatapult eventCatapult;

    private final OwnerReentrantReadWriteLock aggregatorLock = new OwnerReentrantReadWriteLock();

    @Requirement
    private PlexusContainer container;

    private final Map<Thread, MojoDescriptor> mojos = new ConcurrentHashMap<>();

    public MojoExecutor() {}

    public DependencyContext newDependencyContext(MavenSession session, List<MojoExecution> mojoExecutions) {
        Set<String> scopesToCollect = new TreeSet<>();
        Set<String> scopesToResolve = new TreeSet<>();

        collectDependencyRequirements(scopesToResolve, scopesToCollect, mojoExecutions);

        return new DependencyContext(session.getCurrentProject(), scopesToCollect, scopesToResolve);
    }

    private void collectDependencyRequirements(
            Set<String> scopesToResolve, Set<String> scopesToCollect, Collection<MojoExecution> mojoExecutions) {
        for (MojoExecution mojoExecution : mojoExecutions) {
            MojoDescriptor mojoDescriptor = mojoExecution.getMojoDescriptor();

            scopesToResolve.addAll(toScopes(mojoDescriptor.getDependencyResolutionRequired()));

            scopesToCollect.addAll(toScopes(mojoDescriptor.getDependencyCollectionRequired()));
        }
    }

    private Collection<String> toScopes(String classpath) {
        Collection<String> scopes = Collections.emptyList();

        if (StringUtils.isNotEmpty(classpath)) {
            if (Artifact.SCOPE_COMPILE.equals(classpath)) {
                scopes = Arrays.asList(Artifact.SCOPE_COMPILE, Artifact.SCOPE_SYSTEM, Artifact.SCOPE_PROVIDED);
            } else if (Artifact.SCOPE_RUNTIME.equals(classpath)) {
                scopes = Arrays.asList(Artifact.SCOPE_COMPILE, Artifact.SCOPE_RUNTIME);
            } else if (Artifact.SCOPE_COMPILE_PLUS_RUNTIME.equals(classpath)) {
                scopes = Arrays.asList(
                        Artifact.SCOPE_COMPILE, Artifact.SCOPE_SYSTEM, Artifact.SCOPE_PROVIDED, Artifact.SCOPE_RUNTIME);
            } else if (Artifact.SCOPE_RUNTIME_PLUS_SYSTEM.equals(classpath)) {
                scopes = Arrays.asList(Artifact.SCOPE_COMPILE, Artifact.SCOPE_SYSTEM, Artifact.SCOPE_RUNTIME);
            } else if (Artifact.SCOPE_TEST.equals(classpath)) {
                scopes = Arrays.asList(
                        Artifact.SCOPE_COMPILE,
                        Artifact.SCOPE_SYSTEM,
                        Artifact.SCOPE_PROVIDED,
                        Artifact.SCOPE_RUNTIME,
                        Artifact.SCOPE_TEST);
            }
        }
        return Collections.unmodifiableCollection(scopes);
    }

    public void execute(
            final MavenSession session, final List<MojoExecution> mojoExecutions, final ProjectIndex projectIndex)
            throws LifecycleExecutionException {

        final DependencyContext dependencyContext = newDependencyContext(session, mojoExecutions);

        final PhaseRecorder phaseRecorder = new PhaseRecorder(session.getCurrentProject());

        MojosExecutionStrategy strategy;
        try {
            strategy = container.lookup(MojosExecutionStrategy.class);
        } catch (ComponentLookupException e) {
            throw new IllegalStateException("Unable to lookup MojosExecutionStrategy", e);
        }
        strategy.execute(mojoExecutions, session, new MojoExecutionRunner() {
            @Override
            public void run(MojoExecution mojoExecution) throws LifecycleExecutionException {
                MojoExecutor.this.execute(session, mojoExecution, projectIndex, dependencyContext, phaseRecorder);
            }
        });
    }

    private void execute(
            MavenSession session,
            MojoExecution mojoExecution,
            ProjectIndex projectIndex,
            DependencyContext dependencyContext,
            PhaseRecorder phaseRecorder)
            throws LifecycleExecutionException {
        execute(session, mojoExecution, projectIndex, dependencyContext);
        phaseRecorder.observeExecution(mojoExecution);
    }

    private void execute(
            MavenSession session,
            MojoExecution mojoExecution,
            ProjectIndex projectIndex,
            DependencyContext dependencyContext)
            throws LifecycleExecutionException {
        MojoDescriptor mojoDescriptor = mojoExecution.getMojoDescriptor();

        try {
            mavenPluginManager.checkRequiredMavenVersion(mojoDescriptor.getPluginDescriptor());
        } catch (PluginIncompatibleException e) {
            throw new LifecycleExecutionException(mojoExecution, session.getCurrentProject(), e);
        }

        if (mojoDescriptor.isProjectRequired() && !session.getRequest().isProjectPresent()) {
            Throwable cause = new MissingProjectException(
                    "Goal requires a project to execute" + " but there is no POM in this directory ("
                            + session.getExecutionRootDirectory() + ")."
                            + " Please verify you invoked Maven from the correct directory.");
            throw new LifecycleExecutionException(mojoExecution, null, cause);
        }

        if (mojoDescriptor.isOnlineRequired() && session.isOffline()) {
            if (MojoExecution.Source.CLI.equals(mojoExecution.getSource())) {
                Throwable cause = new IllegalStateException(
                        "Goal requires online mode for execution" + " but Maven is currently offline.");
                throw new LifecycleExecutionException(mojoExecution, session.getCurrentProject(), cause);
            } else {
                eventCatapult.fire(ExecutionEvent.Type.MojoSkipped, session, mojoExecution);

                return;
            }
        }

        doExecute(session, mojoExecution, projectIndex, dependencyContext);
    }

    /**
     * Aggregating mojo executions (possibly) modify all MavenProjects, including those that are currently in use
     * by concurrently running mojo executions. To prevent race conditions, an aggregating execution will block
     * all other executions until finished.
     * We also lock on a given project to forbid a forked lifecycle to be executed concurrently with the project.
     * TODO: ideally, the builder should take care of the ordering in a smarter way
     * TODO: and concurrency issues fixed with MNG-7157
     */
    private class ProjectLock implements AutoCloseable {
        final Lock acquiredAggregatorLock;
        final OwnerReentrantLock acquiredProjectLock;

        ProjectLock(MavenSession session, MojoDescriptor mojoDescriptor) {
            mojos.put(Thread.currentThread(), mojoDescriptor);
            if (session.getRequest().getDegreeOfConcurrency() > 1) {
                boolean aggregator = mojoDescriptor.isAggregator();
                acquiredAggregatorLock = aggregator ? aggregatorLock.writeLock() : aggregatorLock.readLock();
                acquiredProjectLock = getProjectLock(session);
                if (!acquiredAggregatorLock.tryLock()) {
                    Thread owner = aggregatorLock.getOwner();
                    MojoDescriptor ownerMojo = owner != null ? mojos.get(owner) : null;
                    String str = ownerMojo != null ? " The " + ownerMojo.getId() : "An";
                    String msg = str + " aggregator mojo is already being executed "
                            + "in this parallel build, those kind of mojos require exclusive access to "
                            + "reactor to prevent race conditions. This mojo execution will be blocked "
                            + "until the aggregator mojo is done.";
                    warn(msg);
                    acquiredAggregatorLock.lock();
                }
                if (!acquiredProjectLock.tryLock()) {
                    Thread owner = acquiredProjectLock.getOwner();
                    MojoDescriptor ownerMojo = owner != null ? mojos.get(owner) : null;
                    String str = ownerMojo != null ? " The " + ownerMojo.getId() : "A";
                    String msg = str + " mojo is already being executed "
                            + "on the project " + session.getCurrentProject().getGroupId()
                            + ":" + session.getCurrentProject().getArtifactId() + ". "
                            + "This mojo execution will be blocked "
                            + "until the mojo is done.";
                    warn(msg);
                    acquiredProjectLock.lock();
                }
            } else {
                acquiredAggregatorLock = null;
                acquiredProjectLock = null;
            }
        }

        @Override
        public void close() {
            // release the lock in the reverse order of the acquisition
            if (acquiredProjectLock != null) {
                acquiredProjectLock.unlock();
            }
            if (acquiredAggregatorLock != null) {
                acquiredAggregatorLock.unlock();
            }
            mojos.remove(Thread.currentThread());
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        private OwnerReentrantLock getProjectLock(MavenSession session) {
            SessionData data = session.getRepositorySession().getData();
            Map<MavenProject, OwnerReentrantLock> locks =
                    (Map) data.computeIfAbsent(ProjectLock.class, ConcurrentHashMap::new);
            return locks.computeIfAbsent(session.getCurrentProject(), p -> new OwnerReentrantLock());
        }
    }

    static class OwnerReentrantLock extends ReentrantLock {
        @Override
        public Thread getOwner() {
            return super.getOwner();
        }
    }

    static class OwnerReentrantReadWriteLock extends ReentrantReadWriteLock {
        @Override
        public Thread getOwner() {
            return super.getOwner();
        }
    }

    private static void warn(String msg) {
        for (String s : MultilineMessageHelper.format(msg)) {
            LOGGER.warn(s);
        }
    }

    private void doExecute(
            MavenSession session,
            MojoExecution mojoExecution,
            ProjectIndex projectIndex,
            DependencyContext dependencyContext)
            throws LifecycleExecutionException {
        MojoDescriptor mojoDescriptor = mojoExecution.getMojoDescriptor();

        List<MavenProject> forkedProjects = executeForkedExecutions(mojoExecution, session, projectIndex);

        try (ProjectLock lock = new ProjectLock(session, mojoDescriptor)) {
            ensureDependenciesAreResolved(mojoDescriptor, session, dependencyContext);

            doExecute2(session, mojoExecution);
        } finally {
            for (MavenProject forkedProject : forkedProjects) {
                forkedProject.setExecutionProject(null);
            }
        }
    }

    private void doExecute2(MavenSession session, MojoExecution mojoExecution) throws LifecycleExecutionException {
        eventCatapult.fire(ExecutionEvent.Type.MojoStarted, session, mojoExecution);
        try {
            try {
                pluginManager.executeMojo(session, mojoExecution);
            } catch (MojoFailureException
                    | PluginManagerException
                    | PluginConfigurationException
                    | MojoExecutionException e) {
                throw new LifecycleExecutionException(mojoExecution, session.getCurrentProject(), e);
            }

            eventCatapult.fire(ExecutionEvent.Type.MojoSucceeded, session, mojoExecution);
        } catch (LifecycleExecutionException e) {
            eventCatapult.fire(ExecutionEvent.Type.MojoFailed, session, mojoExecution, e);

            throw e;
        }
    }

    public void ensureDependenciesAreResolved(
            MojoDescriptor mojoDescriptor, MavenSession session, DependencyContext dependencyContext)
            throws LifecycleExecutionException {

        MavenProject project = dependencyContext.getProject();
        boolean aggregating = mojoDescriptor.isAggregator();

        if (dependencyContext.isResolutionRequiredForCurrentProject()) {
            Collection<String> scopesToCollect = dependencyContext.getScopesToCollectForCurrentProject();
            Collection<String> scopesToResolve = dependencyContext.getScopesToResolveForCurrentProject();

            lifeCycleDependencyResolver.resolveProjectDependencies(
                    project, scopesToCollect, scopesToResolve, session, aggregating, Collections.<Artifact>emptySet());

            dependencyContext.synchronizeWithProjectState();
        }

        if (aggregating) {
            Collection<String> scopesToCollect = toScopes(mojoDescriptor.getDependencyCollectionRequired());
            Collection<String> scopesToResolve = toScopes(mojoDescriptor.getDependencyResolutionRequired());

            if (dependencyContext.isResolutionRequiredForAggregatedProjects(scopesToCollect, scopesToResolve)) {
                for (MavenProject aggregatedProject : session.getProjects()) {
                    if (aggregatedProject != project) {
                        lifeCycleDependencyResolver.resolveProjectDependencies(
                                aggregatedProject,
                                scopesToCollect,
                                scopesToResolve,
                                session,
                                aggregating,
                                Collections.<Artifact>emptySet());
                    }
                }
            }
        }

        ArtifactFilter artifactFilter = getArtifactFilter(mojoDescriptor);
        List<MavenProject> projectsToResolve = LifecycleDependencyResolver.getProjects(
                session.getCurrentProject(), session, mojoDescriptor.isAggregator());
        for (MavenProject projectToResolve : projectsToResolve) {
            projectToResolve.setArtifactFilter(artifactFilter);
        }
    }

    private ArtifactFilter getArtifactFilter(MojoDescriptor mojoDescriptor) {
        String scopeToResolve = mojoDescriptor.getDependencyResolutionRequired();
        String scopeToCollect = mojoDescriptor.getDependencyCollectionRequired();

        List<String> scopes = new ArrayList<>(2);
        if (StringUtils.isNotEmpty(scopeToCollect)) {
            scopes.add(scopeToCollect);
        }
        if (StringUtils.isNotEmpty(scopeToResolve)) {
            scopes.add(scopeToResolve);
        }

        if (scopes.isEmpty()) {
            return null;
        } else {
            return new CumulativeScopeArtifactFilter(scopes);
        }
    }

    public List<MavenProject> executeForkedExecutions(
            MojoExecution mojoExecution, MavenSession session, ProjectIndex projectIndex)
            throws LifecycleExecutionException {
        List<MavenProject> forkedProjects = Collections.emptyList();

        Map<String, List<MojoExecution>> forkedExecutions = mojoExecution.getForkedExecutions();

        if (!forkedExecutions.isEmpty()) {
            eventCatapult.fire(ExecutionEvent.Type.ForkStarted, session, mojoExecution);

            MavenProject project = session.getCurrentProject();

            forkedProjects = new ArrayList<>(forkedExecutions.size());

            try {
                for (Map.Entry<String, List<MojoExecution>> fork : forkedExecutions.entrySet()) {
                    String projectId = fork.getKey();

                    int index = projectIndex.getIndices().get(projectId);

                    MavenProject forkedProject = projectIndex.getProjects().get(projectId);

                    forkedProjects.add(forkedProject);

                    MavenProject executedProject = forkedProject.clone();

                    forkedProject.setExecutionProject(executedProject);

                    List<MojoExecution> mojoExecutions = fork.getValue();

                    if (mojoExecutions.isEmpty()) {
                        continue;
                    }

                    try {
                        session.setCurrentProject(executedProject);
                        session.getProjects().set(index, executedProject);
                        projectIndex.getProjects().put(projectId, executedProject);

                        eventCatapult.fire(ExecutionEvent.Type.ForkedProjectStarted, session, mojoExecution);

                        execute(session, mojoExecutions, projectIndex);

                        eventCatapult.fire(ExecutionEvent.Type.ForkedProjectSucceeded, session, mojoExecution);
                    } catch (LifecycleExecutionException e) {
                        eventCatapult.fire(ExecutionEvent.Type.ForkedProjectFailed, session, mojoExecution, e);

                        throw e;
                    } finally {
                        projectIndex.getProjects().put(projectId, forkedProject);
                        session.getProjects().set(index, forkedProject);
                        session.setCurrentProject(project);
                    }
                }

                eventCatapult.fire(ExecutionEvent.Type.ForkSucceeded, session, mojoExecution);
            } catch (LifecycleExecutionException e) {
                eventCatapult.fire(ExecutionEvent.Type.ForkFailed, session, mojoExecution, e);

                throw e;
            }
        }

        return forkedProjects;
    }
}
