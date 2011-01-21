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
package org.apache.maven.lifecycle.internal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.DefaultLifecycles;
import org.apache.maven.lifecycle.DefaultSchedules;
import org.apache.maven.lifecycle.Lifecycle;
import org.apache.maven.lifecycle.LifecycleNotFoundException;
import org.apache.maven.lifecycle.LifecyclePhaseNotFoundException;
import org.apache.maven.lifecycle.MavenExecutionPlan;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
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
 * @since 3.0
 * @author Benjamin Bentmann
 * @author Kristian Rosenvold (Extract class)
 *         <p/>
 *         NOTE: This class is not part of any public api and can be changed or deleted without prior notice.
 */
@Component( role = LifecycleExecutionPlanCalculator.class )
public class DefaultLifecycleExecutionPlanCalculator
    implements LifecycleExecutionPlanCalculator
{
    @Requirement
    private PluginVersionResolver pluginVersionResolver;

    @Requirement
    private BuildPluginManager pluginManager;

    @Requirement
    private DefaultLifecycles defaultLifeCycles;

    @Requirement
    private DefaultSchedules defaultSchedules;

    @Requirement
    private MojoDescriptorCreator mojoDescriptorCreator;

    @Requirement
    private LifecyclePluginResolver lifecyclePluginResolver;

    @SuppressWarnings( { "UnusedDeclaration" } )
    public DefaultLifecycleExecutionPlanCalculator()
    {
    }

    public DefaultLifecycleExecutionPlanCalculator( BuildPluginManager pluginManager,
                                                    DefaultLifecycles defaultLifeCycles,
                                                    MojoDescriptorCreator mojoDescriptorCreator,
                                                    LifecyclePluginResolver lifecyclePluginResolver,
                                                    DefaultSchedules defaultSchedules )
    {
        this.pluginManager = pluginManager;
        this.defaultLifeCycles = defaultLifeCycles;
        this.mojoDescriptorCreator = mojoDescriptorCreator;
        this.lifecyclePluginResolver = lifecyclePluginResolver;
        this.defaultSchedules = defaultSchedules;
    }

    public MavenExecutionPlan calculateExecutionPlan( MavenSession session, MavenProject project, List<Object> tasks, boolean setup )
        throws PluginNotFoundException, PluginResolutionException, LifecyclePhaseNotFoundException,
        PluginDescriptorParsingException, MojoNotFoundException, InvalidPluginDescriptorException,
        NoPluginFoundForPrefixException, LifecycleNotFoundException, PluginVersionResolutionException
    {
        lifecyclePluginResolver.resolveMissingPluginVersions( project, session );

        final List<MojoExecution> executions = calculateMojoExecutions( session, project, tasks );

        if ( setup )
        {
            setupMojoExecutions( session, project, executions );
        }

        final List<ExecutionPlanItem> planItem = defaultSchedules.createExecutionPlanItem( project, executions );

        return new MavenExecutionPlan( planItem, defaultLifeCycles );
    }

    public MavenExecutionPlan calculateExecutionPlan( MavenSession session, MavenProject project, List<Object> tasks )
        throws PluginNotFoundException, PluginResolutionException, LifecyclePhaseNotFoundException,
        PluginDescriptorParsingException, MojoNotFoundException, InvalidPluginDescriptorException,
        NoPluginFoundForPrefixException, LifecycleNotFoundException, PluginVersionResolutionException
    {
        return calculateExecutionPlan( session, project, tasks, true );
    }

    private void setupMojoExecutions( MavenSession session, MavenProject project, List<MojoExecution> mojoExecutions )
        throws PluginNotFoundException, PluginResolutionException, PluginDescriptorParsingException,
        MojoNotFoundException, InvalidPluginDescriptorException, NoPluginFoundForPrefixException,
        LifecyclePhaseNotFoundException, LifecycleNotFoundException, PluginVersionResolutionException
    {
        for ( MojoExecution mojoExecution : mojoExecutions )
        {
            setupMojoExecution( session, project, mojoExecution );
        }
    }

    public void setupMojoExecution( MavenSession session, MavenProject project, MojoExecution mojoExecution )
        throws PluginNotFoundException, PluginResolutionException, PluginDescriptorParsingException,
        MojoNotFoundException, InvalidPluginDescriptorException, NoPluginFoundForPrefixException,
        LifecyclePhaseNotFoundException, LifecycleNotFoundException, PluginVersionResolutionException
    {
        MojoDescriptor mojoDescriptor = mojoExecution.getMojoDescriptor();

        if ( mojoDescriptor == null )
        {
            mojoDescriptor =
                pluginManager.getMojoDescriptor( mojoExecution.getPlugin(), mojoExecution.getGoal(),
                                                 project.getRemotePluginRepositories(),
                                                 session.getRepositorySession() );

            mojoExecution.setMojoDescriptor( mojoDescriptor );
        }

        populateMojoExecutionConfiguration( project, mojoExecution,
                                            MojoExecution.Source.CLI.equals( mojoExecution.getSource() ) );

        finalizeMojoConfiguration( mojoExecution );

        calculateForkedExecutions( mojoExecution, session, project, new HashSet<MojoDescriptor>() );
    }
    
    public List<MojoExecution> calculateMojoExecutions( MavenSession session, MavenProject project,
                                                         List<Object> tasks )
        throws PluginNotFoundException, PluginResolutionException, PluginDescriptorParsingException,
        MojoNotFoundException, NoPluginFoundForPrefixException, InvalidPluginDescriptorException,
        PluginVersionResolutionException, LifecyclePhaseNotFoundException
    {
        final List<MojoExecution> mojoExecutions = new ArrayList<MojoExecution>();

        for ( Object task : tasks )
        {
            if ( task instanceof GoalTask )
            {
                String pluginGoal = ( (GoalTask) task ).pluginGoal;

                MojoDescriptor mojoDescriptor = mojoDescriptorCreator.getMojoDescriptor( pluginGoal, session, project );

                MojoExecution mojoExecution =
                    new MojoExecution( mojoDescriptor, "default-cli", MojoExecution.Source.CLI );

                mojoExecutions.add( mojoExecution );
            }
            else if ( task instanceof LifecycleTask )
            {
                String lifecyclePhase = ( (LifecycleTask) task ).getLifecyclePhase();

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
        return mojoExecutions;
    }

    private Map<String, List<MojoExecution>> calculateLifecycleMappings( MavenSession session, MavenProject project,
                                                                         String lifecyclePhase )
        throws LifecyclePhaseNotFoundException, PluginNotFoundException, PluginResolutionException,
        PluginDescriptorParsingException, MojoNotFoundException, InvalidPluginDescriptorException
    {
        /*
         * Determine the lifecycle that corresponds to the given phase.
         */

        Lifecycle lifecycle = defaultLifeCycles.get( lifecyclePhase );

        if ( lifecycle == null )
        {
            throw new LifecyclePhaseNotFoundException(
                "Unknown lifecycle phase \"" + lifecyclePhase + "\". You must specify a valid lifecycle phase" +
                    " or a goal in the format <plugin-prefix>:<goal> or" +
                    " <plugin-group-id>:<plugin-artifact-id>[:<plugin-version>]:<goal>. Available lifecycle phases are: " +
                    defaultLifeCycles.getLifecyclePhaseList() + ".", lifecyclePhase );
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
                            mojoExecution.setLifecyclePhase( execution.getPhase() );
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
                            pluginManager.getMojoDescriptor( plugin, goal, project.getRemotePluginRepositories(),
                                                             session.getRepositorySession() );

                        Map<Integer, List<MojoExecution>> phaseBindings = mappings.get( mojoDescriptor.getPhase() );
                        if ( phaseBindings != null )
                        {
                            MojoExecution mojoExecution = new MojoExecution( mojoDescriptor, execution.getId() );
                            mojoExecution.setLifecyclePhase( mojoDescriptor.getPhase() );
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

                parameterConfiguration =
                    Xpp3Dom.mergeXpp3Dom( parameterConfiguration, parameterDefaults, Boolean.TRUE );

                if ( parameterConfiguration != null )
                {
                    parameterConfiguration = new Xpp3Dom( parameterConfiguration, parameter.getName() );

                    if ( StringUtils.isEmpty( parameterConfiguration.getAttribute( "implementation" ) ) &&
                        StringUtils.isNotEmpty( parameter.getImplementation() ) )
                    {
                        parameterConfiguration.setAttribute( "implementation", parameter.getImplementation() );
                    }

                    finalConfiguration.addChild( parameterConfiguration );
                }
            }
        }

        mojoExecution.setConfiguration( finalConfiguration );
    }

    private Xpp3Dom getMojoConfiguration( MojoDescriptor mojoDescriptor )
    {
        return MojoDescriptorCreator.convert( mojoDescriptor );
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

        List<MavenProject> forkedProjects =
            LifecycleDependencyResolver.getProjects( project, session, mojoDescriptor.isAggregator() );

        for ( MavenProject forkedProject : forkedProjects )
        {
            if ( forkedProject != project )
            {
                lifecyclePluginResolver.resolveMissingPluginVersions( forkedProject, session );
            }

            List<MojoExecution> forkedExecutions;

            if ( StringUtils.isNotEmpty( mojoDescriptor.getExecutePhase() ) )
            {
                forkedExecutions =
                    calculateForkedLifecycle( mojoExecution, session, forkedProject, alreadyForkedExecutions );
            }
            else
            {
                forkedExecutions =
                    calculateForkedGoal( mojoExecution, session, forkedProject, alreadyForkedExecutions );
            }

            mojoExecution.setForkedExecutions( BuilderCommon.getKey( forkedProject ), forkedExecutions );
        }

        alreadyForkedExecutions.remove( mojoDescriptor );
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

        Map<String, List<MojoExecution>> lifecycleMappings =
            calculateLifecycleMappings( session, project, forkedPhase );

        for ( List<MojoExecution> forkedExecutions : lifecycleMappings.values() )
        {
            for ( MojoExecution forkedExecution : forkedExecutions )
            {
                if ( forkedExecution.getMojoDescriptor() == null )
                {
                    MojoDescriptor forkedMojoDescriptor =
                        pluginManager.getMojoDescriptor( forkedExecution.getPlugin(), forkedExecution.getGoal(),
                                                         project.getRemotePluginRepositories(),
                                                         session.getRepositorySession() );

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
                            forkedMojoDescriptor = mojoDescriptorCreator.getMojoDescriptor( goal, session, project );
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
    // org.apache.maven.plugins:maven-remote-resources-plugin:1.0:process
    //TODO: take repo mans into account as one may be aggregating prefixes of many
    //TODO: collect at the root of the repository, read the one at the root, and fetch remote if something is missing
    //      or the user forces the issue

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


}
