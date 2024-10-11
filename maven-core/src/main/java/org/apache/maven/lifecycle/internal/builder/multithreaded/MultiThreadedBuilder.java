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
package org.apache.maven.lifecycle.internal.builder.multithreaded;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.internal.BuildThreadFactory;
import org.apache.maven.lifecycle.internal.LifecycleModuleBuilder;
import org.apache.maven.lifecycle.internal.ProjectBuildList;
import org.apache.maven.lifecycle.internal.ProjectSegment;
import org.apache.maven.lifecycle.internal.ReactorBuildStatus;
import org.apache.maven.lifecycle.internal.ReactorContext;
import org.apache.maven.lifecycle.internal.TaskSegment;
import org.apache.maven.lifecycle.internal.builder.Builder;
import org.apache.maven.project.MavenProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Builds the full lifecycle in weave-mode (phase by phase as opposed to project-by-project).
 * <p>
 * This builder uses a number of threads equal to the minimum of the degree of concurrency (which is the thread count
 * set with <code>-T</code> on the command-line) and the number of projects to build. As such, building a single project
 * will always result in a sequential build, regardless of the thread count.
 * </p>
 * <strong>NOTE:</strong> This class is not part of any public api and can be changed or deleted without prior notice.
 *
 * @since 3.0
 *         Builds one or more lifecycles for a full module
 *         NOTE: This class is not part of any public api and can be changed or deleted without prior notice.
 */
@Named("multithreaded")
@Singleton
public class MultiThreadedBuilder implements Builder {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final LifecycleModuleBuilder lifecycleModuleBuilder;

    @Inject
    public MultiThreadedBuilder(LifecycleModuleBuilder lifecycleModuleBuilder) {
        this.lifecycleModuleBuilder = lifecycleModuleBuilder;
    }

    @Override
    public void build(
            MavenSession session,
            ReactorContext reactorContext,
            ProjectBuildList projectBuilds,
            List<TaskSegment> taskSegments,
            ReactorBuildStatus reactorBuildStatus)
            throws ExecutionException, InterruptedException {
        int nThreads = Math.min(
                session.getRequest().getDegreeOfConcurrency(),
                session.getProjects().size());
        boolean parallel = nThreads > 1;
        // Propagate the parallel flag to the root session and all of the cloned sessions in each project segment
        session.setParallel(parallel);
        for (ProjectSegment segment : projectBuilds) {
            segment.getSession().setParallel(parallel);
        }
        ExecutorService executor = Executors.newFixedThreadPool(nThreads, new BuildThreadFactory());
        CompletionService<ProjectSegment> service = new ExecutorCompletionService<>(executor);

        for (TaskSegment taskSegment : taskSegments) {
            ProjectBuildList segmentProjectBuilds = projectBuilds.getByTaskSegment(taskSegment);
            Map<MavenProject, ProjectSegment> projectBuildMap = projectBuilds.selectSegment(taskSegment);
            try {
                ConcurrencyDependencyGraph analyzer =
                        new ConcurrencyDependencyGraph(segmentProjectBuilds, session.getProjectDependencyGraph());
                multiThreadedProjectTaskSegmentBuild(
                        analyzer, reactorContext, session, service, taskSegment, projectBuildMap);
                if (reactorContext.getReactorBuildStatus().isHalted()) {
                    break;
                }
            } catch (Exception e) {
                session.getResult().addException(e);
                break;
            }
        }

        executor.shutdown();
        executor.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
    }

    private void multiThreadedProjectTaskSegmentBuild(
            ConcurrencyDependencyGraph analyzer,
            ReactorContext reactorContext,
            MavenSession rootSession,
            CompletionService<ProjectSegment> service,
            TaskSegment taskSegment,
            Map<MavenProject, ProjectSegment> projectBuildList) {
        // gather artifactIds which are not unique so that the respective thread names can be extended with the groupId
        Set<String> duplicateArtifactIds = projectBuildList.keySet().stream()
                .map(MavenProject::getArtifactId)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .entrySet()
                .stream()
                .filter(p -> p.getValue() > 1)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        // schedule independent projects
        for (MavenProject mavenProject : analyzer.getRootSchedulableBuilds()) {
            ProjectSegment projectSegment = projectBuildList.get(mavenProject);
            logger.debug("Scheduling: {}", projectSegment.getProject());
            Callable<ProjectSegment> cb =
                    createBuildCallable(rootSession, projectSegment, reactorContext, taskSegment, duplicateArtifactIds);
            service.submit(cb);
        }

        // for each finished project
        for (int i = 0; i < analyzer.getNumberOfBuilds(); i++) {
            try {
                ProjectSegment projectBuild = service.take().get();
                if (reactorContext.getReactorBuildStatus().isHalted()) {
                    break;
                }

                // MNG-6170: Only schedule other modules from reactor if we have more modules to build than one.
                if (analyzer.getNumberOfBuilds() > 1) {
                    final List<MavenProject> newItemsThatCanBeBuilt =
                            analyzer.markAsFinished(projectBuild.getProject());
                    for (MavenProject mavenProject : newItemsThatCanBeBuilt) {
                        ProjectSegment scheduledDependent = projectBuildList.get(mavenProject);
                        logger.debug("Scheduling: {}", scheduledDependent);
                        Callable<ProjectSegment> cb = createBuildCallable(
                                rootSession, scheduledDependent, reactorContext, taskSegment, duplicateArtifactIds);
                        service.submit(cb);
                    }
                }
            } catch (InterruptedException e) {
                rootSession.getResult().addException(e);
                break;
            } catch (ExecutionException e) {
                // TODO MNG-5766 changes likely made this redundant
                rootSession.getResult().addException(e);
                break;
            }
        }
    }

    private Callable<ProjectSegment> createBuildCallable(
            final MavenSession rootSession,
            final ProjectSegment projectBuild,
            final ReactorContext reactorContext,
            final TaskSegment taskSegment,
            final Set<String> duplicateArtifactIds) {
        return () -> {
            final Thread currentThread = Thread.currentThread();
            final String originalThreadName = currentThread.getName();
            final MavenProject project = projectBuild.getProject();

            final String threadNameSuffix = duplicateArtifactIds.contains(project.getArtifactId())
                    ? project.getGroupId() + ":" + project.getArtifactId()
                    : project.getArtifactId();
            currentThread.setName("mvn-builder-" + threadNameSuffix);

            try {
                lifecycleModuleBuilder.buildProject(
                        projectBuild.getSession(), rootSession, reactorContext, project, taskSegment);

                return projectBuild;
            } finally {
                currentThread.setName(originalThreadName);
            }
        };
    }
}
