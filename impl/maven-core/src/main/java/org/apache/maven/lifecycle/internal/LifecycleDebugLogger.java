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

import javax.inject.Named;
import javax.inject.Singleton;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.maven.lifecycle.MavenExecutionPlan;
import org.apache.maven.lifecycle.internal.builder.BuilderCommon;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
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
@Singleton
public class LifecycleDebugLogger {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public void debug(String s) {
        logger.trace(s);
    }

    public void info(String s) {
        logger.info(s);
    }

    public void debugReactorPlan(ProjectBuildList projectBuilds) {
        if (!logger.isTraceEnabled()) {
            return;
        }

        logger.trace("=== REACTOR BUILD PLAN ================================================");

        for (Iterator<ProjectSegment> it = projectBuilds.iterator(); it.hasNext(); ) {
            ProjectSegment projectBuild = it.next();

            logger.trace("Project: " + projectBuild.getProject().getId());
            logger.trace("Tasks:   " + projectBuild.getTaskSegment().getTasks());
            logger.trace("Style:   " + (projectBuild.getTaskSegment().isAggregating() ? "Aggregating" : "Regular"));

            if (it.hasNext()) {
                logger.trace("-----------------------------------------------------------------------");
            }
        }

        logger.trace("=======================================================================");
    }

    public void debugProjectPlan(MavenProject currentProject, MavenExecutionPlan executionPlan) {
        if (!logger.isTraceEnabled()) {
            return;
        }

        logger.trace("=== PROJECT BUILD PLAN ================================================");
        logger.trace("Project:       " + BuilderCommon.getKey(currentProject));

        debugDependencyRequirements(executionPlan.getMojoExecutions());

        logger.trace("Repositories (dependencies): " + currentProject.getRemoteProjectRepositories());
        logger.trace("Repositories (plugins)     : " + currentProject.getRemotePluginRepositories());

        for (ExecutionPlanItem mojoExecution : executionPlan) {
            debugMojoExecution(mojoExecution.getMojoExecution());
        }

        logger.trace("=======================================================================");
    }

    private void debugMojoExecution(MojoExecution mojoExecution) {
        String mojoExecId =
                mojoExecution.getGroupId() + ':' + mojoExecution.getArtifactId() + ':' + mojoExecution.getVersion()
                        + ':' + mojoExecution.getGoal() + " (" + mojoExecution.getExecutionId() + ')';

        Map<String, List<MojoExecution>> forkedExecutions = mojoExecution.getForkedExecutions();
        if (!forkedExecutions.isEmpty()) {
            for (Map.Entry<String, List<MojoExecution>> fork : forkedExecutions.entrySet()) {
                logger.trace("--- init fork of " + fork.getKey() + " for " + mojoExecId + " ---");

                debugDependencyRequirements(fork.getValue());

                for (MojoExecution forkedExecution : fork.getValue()) {
                    debugMojoExecution(forkedExecution);
                }

                logger.trace("--- exit fork of " + fork.getKey() + " for " + mojoExecId + " ---");
            }
        }

        logger.trace("-----------------------------------------------------------------------");
        logger.trace("Goal:          " + mojoExecId);
        logger.trace(
                "Style:         " + (mojoExecution.getMojoDescriptor().isAggregator() ? "Aggregating" : "Regular"));
        logger.trace("Configuration: " + mojoExecution.getConfiguration());
    }

    private void debugDependencyRequirements(List<MojoExecution> mojoExecutions) {
        Set<String> scopesToCollect = new TreeSet<>();
        Set<String> scopesToResolve = new TreeSet<>();

        for (MojoExecution mojoExecution : mojoExecutions) {
            MojoDescriptor mojoDescriptor = mojoExecution.getMojoDescriptor();

            String scopeToCollect = mojoDescriptor.getDependencyCollectionRequired();
            if (scopeToCollect != null && !scopeToCollect.isEmpty()) {
                scopesToCollect.add(scopeToCollect);
            }

            String scopeToResolve = mojoDescriptor.getDependencyResolutionRequired();
            if (scopeToResolve != null && !scopeToResolve.isEmpty()) {
                scopesToResolve.add(scopeToResolve);
            }
        }

        logger.trace("Dependencies (collect): " + scopesToCollect);
        logger.trace("Dependencies (resolve): " + scopesToResolve);
    }
}
