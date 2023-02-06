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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.DefaultLifecycles;
import org.apache.maven.lifecycle.Lifecycle;
import org.apache.maven.lifecycle.LifecycleMappingDelegate;
import org.apache.maven.lifecycle.LifecycleNotFoundException;
import org.apache.maven.lifecycle.LifecyclePhaseNotFoundException;
import org.apache.maven.lifecycle.MavenExecutionPlan;
import org.apache.maven.lifecycle.MojoExecutionConfigurator;
import org.apache.maven.lifecycle.internal.builder.BuilderCommon;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.InvalidPluginDescriptorException;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoNotFoundException;
import org.apache.maven.plugin.PluginDescriptorParsingException;
import org.apache.maven.plugin.PluginNotFoundException;
import org.apache.maven.plugin.PluginResolutionException;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.Parameter;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.lifecycle.Execution;
import org.apache.maven.plugin.lifecycle.Phase;
import org.apache.maven.plugin.prefix.NoPluginFoundForPrefixException;
import org.apache.maven.plugin.version.PluginVersionResolutionException;
import org.apache.maven.plugin.version.PluginVersionResolver;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 * <strong>NOTE:</strong> This class is not part of any public api and can be changed or deleted without prior notice.
 *
 * @since 3.0
 * @author Benjamin Bentmann
 * @author Kristian Rosenvold (Extract class)
 */
@Component(role = LifecycleExecutionPlanCalculator.class)
public class DefaultLifecycleExecutionPlanCalculator implements LifecycleExecutionPlanCalculator {
    @Requirement
    private PluginVersionResolver pluginVersionResolver;

    @Requirement
    private BuildPluginManager pluginManager;

    @Requirement
    private DefaultLifecycles defaultLifeCycles;

    @Requirement
    private MojoDescriptorCreator mojoDescriptorCreator;

    @Requirement
    private LifecyclePluginResolver lifecyclePluginResolver;

    @Requirement(hint = DefaultLifecycleMappingDelegate.HINT)
    private LifecycleMappingDelegate standardDelegate;

    @Requirement
    private Map<String, LifecycleMappingDelegate> delegates;

    @Requirement
    private Map<String, MojoExecutionConfigurator> mojoExecutionConfigurators;

    @SuppressWarnings({"UnusedDeclaration"})
    public DefaultLifecycleExecutionPlanCalculator() {}

    // Only used for testing
    public DefaultLifecycleExecutionPlanCalculator(
            BuildPluginManager pluginManager,
            DefaultLifecycles defaultLifeCycles,
            MojoDescriptorCreator mojoDescriptorCreator,
            LifecyclePluginResolver lifecyclePluginResolver) {
        this.pluginManager = pluginManager;
        this.defaultLifeCycles = defaultLifeCycles;
        this.mojoDescriptorCreator = mojoDescriptorCreator;
        this.lifecyclePluginResolver = lifecyclePluginResolver;
        this.mojoExecutionConfigurators =
                Collections.singletonMap("default", (MojoExecutionConfigurator) new DefaultMojoExecutionConfigurator());
    }

    @Override
    public MavenExecutionPlan calculateExecutionPlan(
            MavenSession session, MavenProject project, List<Object> tasks, boolean setup)
            throws PluginNotFoundException, PluginResolutionException, LifecyclePhaseNotFoundException,
                    PluginDescriptorParsingException, MojoNotFoundException, InvalidPluginDescriptorException,
                    NoPluginFoundForPrefixException, LifecycleNotFoundException, PluginVersionResolutionException {
        lifecyclePluginResolver.resolveMissingPluginVersions(project, session);

        final List<MojoExecution> executions = calculateMojoExecutions(session, project, tasks);

        if (setup) {
            setupMojoExecutions(session, project, executions);
        }

        final List<ExecutionPlanItem> planItem = ExecutionPlanItem.createExecutionPlanItems(project, executions);

        return new MavenExecutionPlan(planItem, defaultLifeCycles);
    }

    @Override
    public MavenExecutionPlan calculateExecutionPlan(MavenSession session, MavenProject project, List<Object> tasks)
            throws PluginNotFoundException, PluginResolutionException, LifecyclePhaseNotFoundException,
                    PluginDescriptorParsingException, MojoNotFoundException, InvalidPluginDescriptorException,
                    NoPluginFoundForPrefixException, LifecycleNotFoundException, PluginVersionResolutionException {
        return calculateExecutionPlan(session, project, tasks, true);
    }

    private void setupMojoExecutions(MavenSession session, MavenProject project, List<MojoExecution> mojoExecutions)
            throws PluginNotFoundException, PluginResolutionException, PluginDescriptorParsingException,
                    MojoNotFoundException, InvalidPluginDescriptorException, NoPluginFoundForPrefixException,
                    LifecyclePhaseNotFoundException, LifecycleNotFoundException, PluginVersionResolutionException {
        for (MojoExecution mojoExecution : mojoExecutions) {
            setupMojoExecution(session, project, mojoExecution);
        }
    }

    @Override
    public void setupMojoExecution(MavenSession session, MavenProject project, MojoExecution mojoExecution)
            throws PluginNotFoundException, PluginResolutionException, PluginDescriptorParsingException,
                    MojoNotFoundException, InvalidPluginDescriptorException, NoPluginFoundForPrefixException,
                    LifecyclePhaseNotFoundException, LifecycleNotFoundException, PluginVersionResolutionException {
        MojoDescriptor mojoDescriptor = mojoExecution.getMojoDescriptor();

        if (mojoDescriptor == null) {
            mojoDescriptor = pluginManager.getMojoDescriptor(
                    mojoExecution.getPlugin(),
                    mojoExecution.getGoal(),
                    project.getRemotePluginRepositories(),
                    session.getRepositorySession());

            mojoExecution.setMojoDescriptor(mojoDescriptor);
        }

        mojoExecutionConfigurator(mojoExecution)
                .configure(project, mojoExecution, MojoExecution.Source.CLI.equals(mojoExecution.getSource()));

        finalizeMojoConfiguration(mojoExecution);

        calculateForkedExecutions(mojoExecution, session, project, new HashSet<MojoDescriptor>());
    }

    public List<MojoExecution> calculateMojoExecutions(MavenSession session, MavenProject project, List<Object> tasks)
            throws PluginNotFoundException, PluginResolutionException, PluginDescriptorParsingException,
                    MojoNotFoundException, NoPluginFoundForPrefixException, InvalidPluginDescriptorException,
                    PluginVersionResolutionException, LifecyclePhaseNotFoundException {
        final List<MojoExecution> mojoExecutions = new ArrayList<>();

        for (Object task : tasks) {
            if (task instanceof GoalTask) {
                String pluginGoal = ((GoalTask) task).pluginGoal;

                String executionId = "default-cli";
                int executionIdx = pluginGoal.indexOf('@');
                if (executionIdx > 0) {
                    executionId = pluginGoal.substring(executionIdx + 1);
                }

                MojoDescriptor mojoDescriptor = mojoDescriptorCreator.getMojoDescriptor(pluginGoal, session, project);

                MojoExecution mojoExecution = new MojoExecution(mojoDescriptor, executionId, MojoExecution.Source.CLI);

                mojoExecutions.add(mojoExecution);
            } else if (task instanceof LifecycleTask) {
                String lifecyclePhase = ((LifecycleTask) task).getLifecyclePhase();

                Map<String, List<MojoExecution>> phaseToMojoMapping =
                        calculateLifecycleMappings(session, project, lifecyclePhase);

                for (List<MojoExecution> mojoExecutionsFromLifecycle : phaseToMojoMapping.values()) {
                    mojoExecutions.addAll(mojoExecutionsFromLifecycle);
                }
            } else {
                throw new IllegalStateException("unexpected task " + task);
            }
        }
        return mojoExecutions;
    }

    private Map<String, List<MojoExecution>> calculateLifecycleMappings(
            MavenSession session, MavenProject project, String lifecyclePhase)
            throws LifecyclePhaseNotFoundException, PluginNotFoundException, PluginResolutionException,
                    PluginDescriptorParsingException, MojoNotFoundException, InvalidPluginDescriptorException {
        /*
         * Determine the lifecycle that corresponds to the given phase.
         */

        Lifecycle lifecycle = defaultLifeCycles.get(lifecyclePhase);

        if (lifecycle == null) {
            throw new LifecyclePhaseNotFoundException(
                    "Unknown lifecycle phase \"" + lifecyclePhase
                            + "\". You must specify a valid lifecycle phase"
                            + " or a goal in the format <plugin-prefix>:<goal> or"
                            + " <plugin-group-id>:<plugin-artifact-id>[:<plugin-version>]:<goal>. Available lifecycle phases are: "
                            + defaultLifeCycles.getLifecyclePhaseList() + ".",
                    lifecyclePhase);
        }

        LifecycleMappingDelegate delegate;
        if (Arrays.binarySearch(DefaultLifecycles.STANDARD_LIFECYCLES, lifecycle.getId()) >= 0) {
            delegate = standardDelegate;
        } else {
            delegate = delegates.get(lifecycle.getId());
            if (delegate == null) {
                delegate = standardDelegate;
            }
        }

        return delegate.calculateLifecycleMappings(session, project, lifecycle, lifecyclePhase);
    }

    /**
     * Post-processes the effective configuration for the specified mojo execution. This step discards all parameters
     * from the configuration that are not applicable to the mojo and injects the default values for any missing
     * parameters.
     *
     * @param mojoExecution The mojo execution whose configuration should be finalized, must not be {@code null}.
     */
    private void finalizeMojoConfiguration(MojoExecution mojoExecution) {
        MojoDescriptor mojoDescriptor = mojoExecution.getMojoDescriptor();

        Xpp3Dom executionConfiguration = mojoExecution.getConfiguration();
        if (executionConfiguration == null) {
            executionConfiguration = new Xpp3Dom("configuration");
        }

        Xpp3Dom defaultConfiguration = getMojoConfiguration(mojoDescriptor);

        Xpp3Dom finalConfiguration = new Xpp3Dom("configuration");

        if (mojoDescriptor.getParameters() != null) {
            for (Parameter parameter : mojoDescriptor.getParameters()) {
                Xpp3Dom parameterConfiguration = executionConfiguration.getChild(parameter.getName());

                if (parameterConfiguration == null) {
                    parameterConfiguration = executionConfiguration.getChild(parameter.getAlias());
                }

                Xpp3Dom parameterDefaults = defaultConfiguration.getChild(parameter.getName());

                parameterConfiguration = Xpp3Dom.mergeXpp3Dom(parameterConfiguration, parameterDefaults, Boolean.TRUE);

                if (parameterConfiguration != null) {
                    parameterConfiguration = new Xpp3Dom(parameterConfiguration, parameter.getName());

                    if (StringUtils.isEmpty(parameterConfiguration.getAttribute("implementation"))
                            && StringUtils.isNotEmpty(parameter.getImplementation())) {
                        parameterConfiguration.setAttribute("implementation", parameter.getImplementation());
                    }

                    finalConfiguration.addChild(parameterConfiguration);
                }
            }
        }

        mojoExecution.setConfiguration(finalConfiguration);
    }

    private Xpp3Dom getMojoConfiguration(MojoDescriptor mojoDescriptor) {
        return MojoDescriptorCreator.convert(mojoDescriptor);
    }

    @Override
    public void calculateForkedExecutions(MojoExecution mojoExecution, MavenSession session)
            throws MojoNotFoundException, PluginNotFoundException, PluginResolutionException,
                    PluginDescriptorParsingException, NoPluginFoundForPrefixException, InvalidPluginDescriptorException,
                    LifecyclePhaseNotFoundException, LifecycleNotFoundException, PluginVersionResolutionException {
        calculateForkedExecutions(mojoExecution, session, session.getCurrentProject(), new HashSet<MojoDescriptor>());
    }

    private void calculateForkedExecutions(
            MojoExecution mojoExecution,
            MavenSession session,
            MavenProject project,
            Collection<MojoDescriptor> alreadyForkedExecutions)
            throws MojoNotFoundException, PluginNotFoundException, PluginResolutionException,
                    PluginDescriptorParsingException, NoPluginFoundForPrefixException, InvalidPluginDescriptorException,
                    LifecyclePhaseNotFoundException, LifecycleNotFoundException, PluginVersionResolutionException {
        MojoDescriptor mojoDescriptor = mojoExecution.getMojoDescriptor();

        if (!mojoDescriptor.isForking()) {
            return;
        }

        if (!alreadyForkedExecutions.add(mojoDescriptor)) {
            return;
        }

        List<MavenProject> forkedProjects =
                LifecycleDependencyResolver.getProjects(project, session, mojoDescriptor.isAggregator());

        for (MavenProject forkedProject : forkedProjects) {
            if (forkedProject != project) {
                lifecyclePluginResolver.resolveMissingPluginVersions(forkedProject, session);
            }

            List<MojoExecution> forkedExecutions;

            if (StringUtils.isNotEmpty(mojoDescriptor.getExecutePhase())) {
                forkedExecutions =
                        calculateForkedLifecycle(mojoExecution, session, forkedProject, alreadyForkedExecutions);
            } else {
                forkedExecutions = calculateForkedGoal(mojoExecution, session, forkedProject, alreadyForkedExecutions);
            }

            mojoExecution.setForkedExecutions(BuilderCommon.getKey(forkedProject), forkedExecutions);
        }

        alreadyForkedExecutions.remove(mojoDescriptor);
    }

    private List<MojoExecution> calculateForkedLifecycle(
            MojoExecution mojoExecution,
            MavenSession session,
            MavenProject project,
            Collection<MojoDescriptor> alreadyForkedExecutions)
            throws MojoNotFoundException, PluginNotFoundException, PluginResolutionException,
                    PluginDescriptorParsingException, NoPluginFoundForPrefixException, InvalidPluginDescriptorException,
                    LifecyclePhaseNotFoundException, LifecycleNotFoundException, PluginVersionResolutionException {
        MojoDescriptor mojoDescriptor = mojoExecution.getMojoDescriptor();

        String forkedPhase = mojoDescriptor.getExecutePhase();

        Map<String, List<MojoExecution>> lifecycleMappings = calculateLifecycleMappings(session, project, forkedPhase);

        for (List<MojoExecution> forkedExecutions : lifecycleMappings.values()) {
            for (MojoExecution forkedExecution : forkedExecutions) {
                if (forkedExecution.getMojoDescriptor() == null) {
                    MojoDescriptor forkedMojoDescriptor = pluginManager.getMojoDescriptor(
                            forkedExecution.getPlugin(),
                            forkedExecution.getGoal(),
                            project.getRemotePluginRepositories(),
                            session.getRepositorySession());

                    forkedExecution.setMojoDescriptor(forkedMojoDescriptor);
                }

                mojoExecutionConfigurator(forkedExecution).configure(project, forkedExecution, false);
            }
        }

        injectLifecycleOverlay(lifecycleMappings, mojoExecution, session, project);

        List<MojoExecution> mojoExecutions = new ArrayList<>();

        for (List<MojoExecution> forkedExecutions : lifecycleMappings.values()) {
            for (MojoExecution forkedExecution : forkedExecutions) {
                if (!alreadyForkedExecutions.contains(forkedExecution.getMojoDescriptor())) {
                    finalizeMojoConfiguration(forkedExecution);

                    calculateForkedExecutions(forkedExecution, session, project, alreadyForkedExecutions);

                    mojoExecutions.add(forkedExecution);
                }
            }
        }

        return mojoExecutions;
    }

    private void injectLifecycleOverlay(
            Map<String, List<MojoExecution>> lifecycleMappings,
            MojoExecution mojoExecution,
            MavenSession session,
            MavenProject project)
            throws PluginDescriptorParsingException, LifecycleNotFoundException, MojoNotFoundException,
                    PluginNotFoundException, PluginResolutionException, NoPluginFoundForPrefixException,
                    InvalidPluginDescriptorException, PluginVersionResolutionException {
        MojoDescriptor mojoDescriptor = mojoExecution.getMojoDescriptor();

        PluginDescriptor pluginDescriptor = mojoDescriptor.getPluginDescriptor();

        String forkedLifecycle = mojoDescriptor.getExecuteLifecycle();

        if (StringUtils.isEmpty(forkedLifecycle)) {
            return;
        }

        org.apache.maven.plugin.lifecycle.Lifecycle lifecycleOverlay;

        try {
            lifecycleOverlay = pluginDescriptor.getLifecycleMapping(forkedLifecycle);
        } catch (IOException | XmlPullParserException e) {
            throw new PluginDescriptorParsingException(pluginDescriptor.getPlugin(), pluginDescriptor.getSource(), e);
        }

        if (lifecycleOverlay == null) {
            throw new LifecycleNotFoundException(forkedLifecycle);
        }

        for (Phase phase : lifecycleOverlay.getPhases()) {
            List<MojoExecution> forkedExecutions = lifecycleMappings.get(phase.getId());

            if (forkedExecutions != null) {
                for (Execution execution : phase.getExecutions()) {
                    for (String goal : execution.getGoals()) {
                        MojoDescriptor forkedMojoDescriptor;

                        if (goal.indexOf(':') < 0) {
                            forkedMojoDescriptor = pluginDescriptor.getMojo(goal);
                            if (forkedMojoDescriptor == null) {
                                throw new MojoNotFoundException(goal, pluginDescriptor);
                            }
                        } else {
                            forkedMojoDescriptor = mojoDescriptorCreator.getMojoDescriptor(goal, session, project);
                        }

                        MojoExecution forkedExecution =
                                new MojoExecution(forkedMojoDescriptor, mojoExecution.getExecutionId());

                        Xpp3Dom forkedConfiguration = (Xpp3Dom) execution.getConfiguration();

                        forkedExecution.setConfiguration(forkedConfiguration);

                        mojoExecutionConfigurator(forkedExecution).configure(project, forkedExecution, true);

                        forkedExecutions.add(forkedExecution);
                    }
                }

                Xpp3Dom phaseConfiguration = (Xpp3Dom) phase.getConfiguration();

                if (phaseConfiguration != null) {
                    for (MojoExecution forkedExecution : forkedExecutions) {
                        Xpp3Dom forkedConfiguration = forkedExecution.getConfiguration();

                        forkedConfiguration = Xpp3Dom.mergeXpp3Dom(phaseConfiguration, forkedConfiguration);

                        forkedExecution.setConfiguration(forkedConfiguration);
                    }
                }
            }
        }
    }

    // org.apache.maven.plugins:maven-remote-resources-plugin:1.0:process
    // TODO take repo mans into account as one may be aggregating prefixes of many
    // TODO collect at the root of the repository, read the one at the root, and fetch remote if something is missing
    // or the user forces the issue

    private List<MojoExecution> calculateForkedGoal(
            MojoExecution mojoExecution,
            MavenSession session,
            MavenProject project,
            Collection<MojoDescriptor> alreadyForkedExecutions)
            throws MojoNotFoundException, PluginNotFoundException, PluginResolutionException,
                    PluginDescriptorParsingException, NoPluginFoundForPrefixException, InvalidPluginDescriptorException,
                    LifecyclePhaseNotFoundException, LifecycleNotFoundException, PluginVersionResolutionException {
        MojoDescriptor mojoDescriptor = mojoExecution.getMojoDescriptor();

        PluginDescriptor pluginDescriptor = mojoDescriptor.getPluginDescriptor();

        String forkedGoal = mojoDescriptor.getExecuteGoal();

        MojoDescriptor forkedMojoDescriptor = pluginDescriptor.getMojo(forkedGoal);
        if (forkedMojoDescriptor == null) {
            throw new MojoNotFoundException(forkedGoal, pluginDescriptor);
        }

        if (alreadyForkedExecutions.contains(forkedMojoDescriptor)) {
            return Collections.emptyList();
        }

        MojoExecution forkedExecution = new MojoExecution(forkedMojoDescriptor, forkedGoal);

        mojoExecutionConfigurator(forkedExecution).configure(project, forkedExecution, true);

        finalizeMojoConfiguration(forkedExecution);

        calculateForkedExecutions(forkedExecution, session, project, alreadyForkedExecutions);

        return Collections.singletonList(forkedExecution);
    }

    private MojoExecutionConfigurator mojoExecutionConfigurator(MojoExecution mojoExecution) {
        String configuratorId = mojoExecution.getMojoDescriptor().getComponentConfigurator();
        if (configuratorId == null) {
            configuratorId = "default";
        }

        MojoExecutionConfigurator mojoExecutionConfigurator = mojoExecutionConfigurators.get(configuratorId);

        if (mojoExecutionConfigurator == null) {
            //
            // The plugin has a custom component configurator but does not have a custom mojo execution configurator
            // so fall back to the default mojo execution configurator.
            //
            mojoExecutionConfigurator = mojoExecutionConfigurators.get("default");
        }
        return mojoExecutionConfigurator;
    }
}
