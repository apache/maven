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

import java.util.List;
import java.util.Set;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.LifecycleNotFoundException;
import org.apache.maven.lifecycle.LifecyclePhaseNotFoundException;
import org.apache.maven.lifecycle.MavenExecutionPlan;
import org.apache.maven.plugin.InvalidPluginDescriptorException;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoNotFoundException;
import org.apache.maven.plugin.PluginDescriptorParsingException;
import org.apache.maven.plugin.PluginNotFoundException;
import org.apache.maven.plugin.PluginResolutionException;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.prefix.NoPluginFoundForPrefixException;
import org.apache.maven.plugin.version.PluginVersionResolutionException;
import org.apache.maven.project.MavenProject;

/**
 * @since 3.0
 */
public interface LifecycleExecutionPlanCalculator {
    MavenExecutionPlan calculateExecutionPlan(MavenSession session, MavenProject project, List<Task> tasks)
            throws PluginNotFoundException, PluginResolutionException, LifecyclePhaseNotFoundException,
                    PluginDescriptorParsingException, MojoNotFoundException, InvalidPluginDescriptorException,
                    NoPluginFoundForPrefixException, LifecycleNotFoundException, PluginVersionResolutionException;

    MavenExecutionPlan calculateExecutionPlan(
            MavenSession session, MavenProject project, List<Task> tasks, boolean setup)
            throws PluginNotFoundException, PluginResolutionException, LifecyclePhaseNotFoundException,
                    PluginDescriptorParsingException, MojoNotFoundException, InvalidPluginDescriptorException,
                    NoPluginFoundForPrefixException, LifecycleNotFoundException, PluginVersionResolutionException;

    void calculateForkedExecutions(MojoExecution mojoExecution, MavenSession session)
            throws MojoNotFoundException, PluginNotFoundException, PluginResolutionException,
                    PluginDescriptorParsingException, NoPluginFoundForPrefixException, InvalidPluginDescriptorException,
                    LifecyclePhaseNotFoundException, LifecycleNotFoundException, PluginVersionResolutionException;

    void setupMojoExecution(
            MavenSession session,
            MavenProject project,
            MojoExecution mojoExecution,
            Set<MojoDescriptor> alreadyPlannedExecutions)
            throws PluginNotFoundException, PluginResolutionException, PluginDescriptorParsingException,
                    MojoNotFoundException, InvalidPluginDescriptorException, NoPluginFoundForPrefixException,
                    LifecyclePhaseNotFoundException, LifecycleNotFoundException, PluginVersionResolutionException;
}
