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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.xml.stream.XMLStreamException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.api.plugin.descriptor.lifecycle.Execution;
import org.apache.maven.api.plugin.descriptor.lifecycle.Phase;
import org.apache.maven.api.xml.XmlNode;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.internal.xml.XmlNodeImpl;
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
import org.apache.maven.plugin.prefix.NoPluginFoundForPrefixException;
import org.apache.maven.plugin.version.PluginVersionResolutionException;
import org.apache.maven.project.MavenProject;

/**
 * <strong>NOTE:</strong> This class is not part of any public api and can be changed or deleted without prior notice.
 *
 * @since 3.0
 */
@Named
@Singleton
public class DefaultLifecycleExecutionPlanCalculator implements LifecycleExecutionPlanCalculator {

    private final BuildPluginManager pluginManager;

    private final DefaultLifecycles defaultLifecycles;

    private final MojoDescriptorCreator mojoDescriptorCreator;

    private final LifecyclePluginResolver lifecyclePluginResolver;

    private final LifecycleMappingDelegate standardDelegate;

    private final Map<String, LifecycleMappingDelegate> delegates;

    private final Map<String, MojoExecutionConfigurator> mojoExecutionConfigurators;

    @Inject
    public DefaultLifecycleExecutionPlanCalculator(
            BuildPluginManager pluginManager,
            DefaultLifecycles defaultLifecycles,
            MojoDescriptorCreator mojoDescriptorCreator,
            LifecyclePluginResolver lifecyclePluginResolver,
            @Named(DefaultLifecycleMappingDelegate.HINT) LifecycleMappingDelegate standardDelegate,
            Map<String, LifecycleMappingDelegate> delegates,
            Map<String, MojoExecutionConfigurator> mojoExecutionConfigurators) {
        this.pluginManager = pluginManager;
        this.defaultLifecycles = defaultLifecycles;
        this.mojoDescriptorCreator = mojoDescriptorCreator;
        this.lifecyclePluginResolver = lifecyclePluginResolver;
        this.standardDelegate = standardDelegate;
        this.delegates = delegates;
        this.mojoExecutionConfigurators = mojoExecutionConfigurators;
    }

    // Only used for testing
    public DefaultLifecycleExecutionPlanCalculator(
            BuildPluginManager pluginManager,
            DefaultLifecycles defaultLifecycles,
            MojoDescriptorCreator mojoDescriptorCreator,
            LifecyclePluginResolver lifecyclePluginResolver) {
        this.pluginManager = pluginManager;
        this.defaultLifecycles = defaultLifecycles;
        this.mojoDescriptorCreator = mojoDescriptorCreator;
        this.lifecyclePluginResolver = lifecyclePluginResolver;
        this.standardDelegate = null;
        this.delegates = null;
        this.mojoExecutionConfigurators = Collections.singletonMap("default", new DefaultMojoExecutionConfigurator());
    }

    @Override
    public MavenExecutionPlan calculateExecutionPlan(
            MavenSession session, MavenProject project, List<Task> tasks, boolean setup)
            throws PluginNotFoundException, PluginResolutionException, LifecyclePhaseNotFoundException,
                    PluginDescriptorParsingException, MojoNotFoundException, InvalidPluginDescriptorException,
                    NoPluginFoundForPrefixException, LifecycleNotFoundException, PluginVersionResolutionException {
        lifecyclePluginResolver.resolveMissingPluginVersions(project, session);

        final List<MojoExecution> executions = calculateMojoExecutions(session, project, tasks);

        if (setup) {
            setupMojoExecutions(session, project, executions);
        }

        final List<ExecutionPlanItem> planItem = ExecutionPlanItem.createExecutionPlanItems(project, executions);

        return new MavenExecutionPlan(planItem, defaultLifecycles);
    }

    @Override
    public MavenExecutionPlan calculateExecutionPlan(MavenSession session, MavenProject project, List<Task> tasks)
            throws PluginNotFoundException, PluginResolutionException, LifecyclePhaseNotFoundException,
                    PluginDescriptorParsingException, MojoNotFoundException, InvalidPluginDescriptorException,
                    NoPluginFoundForPrefixException, LifecycleNotFoundException, PluginVersionResolutionException {
        return calculateExecutionPlan(session, project, tasks, true);
    }

    private void setupMojoExecutions(MavenSession session, MavenProject project, List<MojoExecution> mojoExecutions)
            throws PluginNotFoundException, PluginResolutionException, PluginDescriptorParsingException,
                    MojoNotFoundException, InvalidPluginDescriptorException, NoPluginFoundForPrefixException,
                    LifecyclePhaseNotFoundException, LifecycleNotFoundException, PluginVersionResolutionException {
        Set<MojoDescriptor> alreadyPlannedExecutions = fillMojoDescriptors(session, project, mojoExecutions);

        for (MojoExecution mojoExecution : mojoExecutions) {
            setupMojoExecution(session, project, mojoExecution, alreadyPlannedExecutions);
        }
    }

    private Set<MojoDescriptor> fillMojoDescriptors(
            MavenSession session, MavenProject project, List<MojoExecution> mojoExecutions)
            throws InvalidPluginDescriptorException, MojoNotFoundException, PluginResolutionException,
                    PluginDescriptorParsingException, PluginNotFoundException {
        Set<MojoDescriptor> descriptors = new HashSet<>(mojoExecutions.size());

        for (MojoExecution execution : mojoExecutions) {
            MojoDescriptor mojoDescriptor = fillMojoDescriptor(session, project, execution);
            descriptors.add(mojoDescriptor);
        }

        return descriptors;
    }

    private MojoDescriptor fillMojoDescriptor(MavenSession session, MavenProject project, MojoExecution execution)
            throws PluginNotFoundException, PluginResolutionException, PluginDescriptorParsingException,
                    MojoNotFoundException, InvalidPluginDescriptorException {
        MojoDescriptor mojoDescriptor = execution.getMojoDescriptor();

        if (mojoDescriptor == null) {
            mojoDescriptor = pluginManager.getMojoDescriptor(
                    execution.getPlugin(),
                    execution.getGoal(),
                    project.getRemotePluginRepositories(),
                    session.getRepositorySession());

            execution.setMojoDescriptor(mojoDescriptor);
        }

        return mojoDescriptor;
    }

    @Override
    public void setupMojoExecution(
            MavenSession session,
            MavenProject project,
            MojoExecution mojoExecution,
            Set<MojoDescriptor> alreadyPlannedExecutions)
            throws PluginNotFoundException, PluginResolutionException, PluginDescriptorParsingException,
                    MojoNotFoundException, InvalidPluginDescriptorException, NoPluginFoundForPrefixException,
                    LifecyclePhaseNotFoundException, LifecycleNotFoundException, PluginVersionResolutionException {
        fillMojoDescriptor(session, project, mojoExecution);

        mojoExecutionConfigurator(mojoExecution)
                .configure(project, mojoExecution, MojoExecution.Source.CLI.equals(mojoExecution.getSource()));

        finalizeMojoConfiguration(mojoExecution);

        calculateForkedExecutions(mojoExecution, session, project, alreadyPlannedExecutions);
    }

    public List<MojoExecution> calculateMojoExecutions(MavenSession session, MavenProject project, List<Task> tasks)
            throws PluginNotFoundException, PluginResolutionException, PluginDescriptorParsingException,
                    MojoNotFoundException, NoPluginFoundForPrefixException, InvalidPluginDescriptorException,
                    PluginVersionResolutionException, LifecyclePhaseNotFoundException {
        final List<MojoExecution> mojoExecutions = new ArrayList<>();

        for (Task task : tasks) {
            if (task instanceof GoalTask) {
                String pluginGoal = task.getValue();

                String executionId = "default-cli";
                int executionIdx = pluginGoal.indexOf('@');
                if (executionIdx > 0) {
                    executionId = pluginGoal.substring(executionIdx + 1);
                }

                MojoDescriptor mojoDescriptor = mojoDescriptorCreator.getMojoDescriptor(pluginGoal, session, project);

                MojoExecution mojoExecution = new MojoExecution(mojoDescriptor, executionId, MojoExecution.Source.CLI);

                mojoExecutions.add(mojoExecution);
            } else if (task instanceof LifecycleTask) {
                String lifecyclePhase = task.getValue();

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

        Lifecycle lifecycle = defaultLifecycles.get(lifecyclePhase);

        if (lifecycle == null) {
            throw new LifecyclePhaseNotFoundException(
                    "Unknown lifecycle phase \"" + lifecyclePhase
                            + "\". You must specify a valid lifecycle phase"
                            + " or a goal in the format <plugin-prefix>:<goal> or"
                            + " <plugin-group-id>:<plugin-artifact-id>[:<plugin-version>]:<goal>. Available lifecycle phases are: "
                            + defaultLifecycles.getLifecyclePhaseList() + ".",
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

        XmlNode executionConfiguration = mojoExecution.getConfiguration() != null
                ? mojoExecution.getConfiguration().getDom()
                : null;
        if (executionConfiguration == null) {
            executionConfiguration = new XmlNodeImpl("configuration");
        }

        XmlNode defaultConfiguration = getMojoConfiguration(mojoDescriptor);

        List<XmlNode> children = new ArrayList<>();
        if (mojoDescriptor.getParameters() != null) {
            for (Parameter parameter : mojoDescriptor.getParameters()) {
                XmlNode parameterConfiguration = executionConfiguration.getChild(parameter.getName());

                if (parameterConfiguration == null) {
                    parameterConfiguration = executionConfiguration.getChild(parameter.getAlias());
                }

                XmlNode parameterDefaults = defaultConfiguration.getChild(parameter.getName());

                if (parameterConfiguration != null) {
                    parameterConfiguration = parameterConfiguration.merge(parameterDefaults, Boolean.TRUE);
                } else {
                    parameterConfiguration = parameterDefaults;
                }

                if (parameterConfiguration != null) {
                    Map<String, String> attributes = new HashMap<>(parameterConfiguration.getAttributes());

                    String attributeForImplementation = parameterConfiguration.getAttribute("implementation");
                    String parameterForImplementation = parameter.getImplementation();
                    if ((attributeForImplementation == null || attributeForImplementation.isEmpty())
                            && ((parameterForImplementation != null) && !parameterForImplementation.isEmpty())) {
                        attributes.put("implementation", parameter.getImplementation());
                    }

                    parameterConfiguration = new XmlNodeImpl(
                            parameter.getName(),
                            parameterConfiguration.getValue(),
                            attributes,
                            parameterConfiguration.getChildren(),
                            parameterConfiguration.getInputLocation());

                    children.add(parameterConfiguration);
                }
            }
        }
        XmlNode finalConfiguration = new XmlNodeImpl("configuration", null, null, children, null);

        mojoExecution.setConfiguration(finalConfiguration);
    }

    private XmlNode getMojoConfiguration(MojoDescriptor mojoDescriptor) {
        if (mojoDescriptor.isV4Api()) {
            return MojoDescriptorCreator.convert(mojoDescriptor.getMojoDescriptorV4());
        } else {
            return MojoDescriptorCreator.convert(mojoDescriptor).getDom();
        }
    }

    @Override
    public void calculateForkedExecutions(MojoExecution mojoExecution, MavenSession session)
            throws MojoNotFoundException, PluginNotFoundException, PluginResolutionException,
                    PluginDescriptorParsingException, NoPluginFoundForPrefixException, InvalidPluginDescriptorException,
                    LifecyclePhaseNotFoundException, LifecycleNotFoundException, PluginVersionResolutionException {
        calculateForkedExecutions(mojoExecution, session, session.getCurrentProject(), new HashSet<>());
    }

    private void calculateForkedExecutions(
            MojoExecution mojoExecution,
            MavenSession session,
            MavenProject project,
            Collection<MojoDescriptor> alreadyPlannedExecutions)
            throws MojoNotFoundException, PluginNotFoundException, PluginResolutionException,
                    PluginDescriptorParsingException, NoPluginFoundForPrefixException, InvalidPluginDescriptorException,
                    LifecyclePhaseNotFoundException, LifecycleNotFoundException, PluginVersionResolutionException {
        MojoDescriptor mojoDescriptor = mojoExecution.getMojoDescriptor();

        if (!mojoDescriptor.isForking()) {
            return;
        }

        alreadyPlannedExecutions.add(mojoDescriptor);

        List<MavenProject> forkedProjects =
                LifecycleDependencyResolver.getProjects(project, session, mojoDescriptor.isAggregator());

        for (MavenProject forkedProject : forkedProjects) {
            if (forkedProject != project) {
                lifecyclePluginResolver.resolveMissingPluginVersions(forkedProject, session);
            }

            List<MojoExecution> forkedExecutions;

            if (mojoDescriptor.getExecutePhase() != null
                    && !mojoDescriptor.getExecutePhase().isEmpty()) {
                forkedExecutions =
                        calculateForkedLifecycle(mojoExecution, session, forkedProject, alreadyPlannedExecutions);
            } else {
                forkedExecutions = calculateForkedGoal(mojoExecution, session, forkedProject, alreadyPlannedExecutions);
            }

            // This List can be empty when the executions are already present in the plan
            if (!forkedExecutions.isEmpty()) {
                mojoExecution.setForkedExecutions(BuilderCommon.getKey(forkedProject), forkedExecutions);
            }
        }
    }

    private List<MojoExecution> calculateForkedLifecycle(
            MojoExecution mojoExecution,
            MavenSession session,
            MavenProject project,
            Collection<MojoDescriptor> alreadyPlannedExecutions)
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
                if (!alreadyPlannedExecutions.contains(forkedExecution.getMojoDescriptor())) {
                    finalizeMojoConfiguration(forkedExecution);

                    calculateForkedExecutions(forkedExecution, session, project, alreadyPlannedExecutions);

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

        if (forkedLifecycle == null || forkedLifecycle.isEmpty()) {
            return;
        }

        org.apache.maven.api.plugin.descriptor.lifecycle.Lifecycle lifecycleOverlay;

        try {
            lifecycleOverlay = pluginDescriptor.getLifecycleMapping(forkedLifecycle);
        } catch (IOException | XMLStreamException e) {
            throw new PluginDescriptorParsingException(pluginDescriptor.getPlugin(), pluginDescriptor.getSource(), e);
        }

        if (lifecycleOverlay == null) {
            throw new LifecycleNotFoundException(forkedLifecycle);
        }

        for (Phase phase : lifecycleOverlay.getPhases()) {
            String phaseId = defaultLifecycles.getLifeCycles().stream()
                    .flatMap(l -> l.getDelegate().aliases().stream())
                    .filter(a -> phase.getId().equals(a.v3Phase()))
                    .findFirst()
                    .map(a -> a.v4Phase())
                    .orElse(phase.getId());

            List<MojoExecution> forkedExecutions = lifecycleMappings.get(phaseId);

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

                        XmlNode forkedConfiguration = execution.getConfiguration();

                        forkedExecution.setConfiguration(forkedConfiguration);

                        mojoExecutionConfigurator(forkedExecution).configure(project, forkedExecution, true);

                        forkedExecutions.add(forkedExecution);
                    }
                }

                XmlNode phaseConfiguration = phase.getConfiguration();

                if (phaseConfiguration != null) {
                    for (MojoExecution forkedExecution : forkedExecutions) {
                        org.codehaus.plexus.util.xml.Xpp3Dom config = forkedExecution.getConfiguration();

                        if (config != null) {
                            XmlNode forkedConfiguration = config.getDom();

                            forkedConfiguration = phaseConfiguration.merge(forkedConfiguration);

                            forkedExecution.setConfiguration(forkedConfiguration);
                        }
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
            Collection<MojoDescriptor> alreadyPlannedExecutions)
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

        if (alreadyPlannedExecutions.contains(forkedMojoDescriptor)) {
            return Collections.emptyList();
        }

        MojoExecution forkedExecution = new MojoExecution(forkedMojoDescriptor, forkedGoal);

        mojoExecutionConfigurator(forkedExecution).configure(project, forkedExecution, true);

        finalizeMojoConfiguration(forkedExecution);

        calculateForkedExecutions(forkedExecution, session, project, alreadyPlannedExecutions);

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
