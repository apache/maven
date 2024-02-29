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
package org.apache.maven.lifecycle.internal.concurrent;

import javax.inject.Named;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * Logs debug output from the various lifecycle phases.
 * </p>
 * <strong>NOTE:</strong> This class is not part of any public api and can be changed or deleted without prior notice.
 *
 * @since 3.0
 */
@Named
public class BuildPlanLogger {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public void writePlan(BuildPlan plan) {
        if (logger.isDebugEnabled()) {
            writePlan(logger::debug, plan);
        }
    }

    public void writePlan(BuildPlan plan, MavenProject project) {
        if (logger.isDebugEnabled()) {
            writePlan(logger::debug, plan, project);
        }
    }

    public void writePlan(Consumer<String> writer, BuildPlan plan) {
        plan.projects().forEach(project -> writePlan(writer, plan, project));
    }

    public void writePlan(Consumer<String> writer, BuildPlan plan, MavenProject project) {
        writer.accept("=== PROJECT BUILD PLAN ================================================");
        writer.accept("Project:                     " + getKey(project));
        writer.accept("Repositories (dependencies): " + project.getRemoteProjectRepositories());
        writer.accept("Repositories (plugins):      " + project.getRemotePluginRepositories());

        Optional<BuildStep> planStep = plan.step(project, BuildStep.PLAN);
        if (planStep.isPresent() && planStep.get().status.get() == BuildStep.PLANNING) {
            writer.accept("Build plan will be lazily computed");
        } else {
            plan.steps(project)
                    .filter(step ->
                            step.phase != null && step.executions().findAny().isPresent())
                    .sorted(Comparator.comparingInt(plan.sortedNodes()::indexOf))
                    .forEach(step -> {
                        writer.accept("\t-----------------------------------------------------------------------");
                        writer.accept("\tPhase:         " + step.name);
                        if (!step.predecessors.isEmpty()) {
                            writer.accept("\tPredecessors:  "
                                    + nonEmptyPredecessors(step)
                                            .map(n -> phase(project, n, plan.duplicateIds()))
                                            .collect(Collectors.joining(", ")));
                        }
                        /*
                        if (!node.successors.isEmpty()) {
                            writer.accept("\tSuccessors:    "
                                    + node.successors.stream()
                                            .map(n -> phase(currentProject, n, duplicateIds))
                                            .collect(Collectors.joining(", ")));
                        }
                        */
                        step.mojos.values().stream()
                                .flatMap(m -> m.values().stream())
                                .forEach(mojo -> mojo(writer, mojo));
                    });
        }

        writer.accept("=======================================================================");
    }

    protected Stream<BuildStep> nonEmptyPredecessors(BuildStep step) {
        HashSet<BuildStep> preds = new HashSet<>();
        nonEmptyPredecessors(step, preds, new HashSet<>());
        return preds.stream();
    }

    private void nonEmptyPredecessors(BuildStep step, Set<BuildStep> preds, Set<BuildStep> visited) {
        if (visited.add(step)) {
            step.predecessors.forEach(ch -> {
                if (ch.executions().findAny().isPresent()) {
                    preds.add(ch);
                } else {
                    nonEmptyPredecessors(ch, preds, visited);
                }
            });
        }
    }

    protected String phase(MavenProject currentProject, BuildStep step, Set<String> duplicateIds) {
        if (step.project == currentProject) {
            return step.name;
        } else {
            String artifactId = step.project.getArtifactId();
            if (duplicateIds.contains(artifactId)) {
                return step.name + "(" + step.project.getGroupId() + ":" + artifactId + ")";
            } else {
                return step.name + "(:" + artifactId + ")";
            }
        }
    }

    protected void mojo(Consumer<String> writer, MojoExecution mojoExecution) {
        String mojoExecId =
                mojoExecution.getGroupId() + ':' + mojoExecution.getArtifactId() + ':' + mojoExecution.getVersion()
                        + ':' + mojoExecution.getGoal() + " (" + mojoExecution.getExecutionId() + ')';

        Map<String, List<MojoExecution>> forkedExecutions = mojoExecution.getForkedExecutions();
        if (!forkedExecutions.isEmpty()) {
            for (Map.Entry<String, List<MojoExecution>> fork : forkedExecutions.entrySet()) {
                writer.accept("\t--- init fork of " + fork.getKey() + " for " + mojoExecId + " ---");

                for (MojoExecution forkedExecution : fork.getValue()) {
                    mojo(writer, forkedExecution);
                }

                writer.accept("\t--- exit fork of " + fork.getKey() + " for " + mojoExecId + " ---");
            }
        }

        writer.accept("\t\t-----------------------------------------------------------------------");
        if (mojoExecution.getMojoDescriptor().isAggregator()) {
            writer.accept("\t\tAggregator goal:        " + mojoExecId);
        } else {
            writer.accept("\t\tGoal:                   " + mojoExecId);
        }
        if (mojoExecution.getConfiguration() != null) {
            writer.accept("\t\tConfiguration:          " + mojoExecution.getConfiguration());
        }
        if (mojoExecution.getMojoDescriptor().getDependencyCollectionRequired() != null) {
            writer.accept("\t\tDependencies (collect): "
                    + mojoExecution.getMojoDescriptor().getDependencyCollectionRequired());
        }
        if (mojoExecution.getMojoDescriptor().getDependencyResolutionRequired() != null) {
            writer.accept("\t\tDependencies (resolve): "
                    + mojoExecution.getMojoDescriptor().getDependencyResolutionRequired());
        }
    }

    protected String getKey(MavenProject project) {
        return project.getGroupId() + ':' + project.getArtifactId() + ':' + project.getVersion();
    }
}
