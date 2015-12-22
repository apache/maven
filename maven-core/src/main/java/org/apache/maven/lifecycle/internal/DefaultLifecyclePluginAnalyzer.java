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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.lifecycle.DefaultLifecycles;
import org.apache.maven.lifecycle.LifeCyclePluginAnalyzer;
import org.apache.maven.lifecycle.Lifecycle;
import org.apache.maven.lifecycle.LifecycleMappingDelegate;
import org.apache.maven.lifecycle.LifecycleMappingNotFoundException;
import org.apache.maven.lifecycle.mapping.LifecycleMapping;
import org.apache.maven.lifecycle.mapping.LifecycleMojo;
import org.apache.maven.lifecycle.mapping.LifecyclePhase;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.StringUtils;

/**
 * @since 3.0
 * @author Benjamin Bentmann
 * @author Jason van Zyl
 * @author jdcasey
 * @author Kristian Rosenvold (extracted class only)
 * <p/>
 * NOTE: This class is not part of any public api and can be changed or deleted without prior notice.
 */
@Component( role = LifeCyclePluginAnalyzer.class )
public class DefaultLifecyclePluginAnalyzer
    implements LifeCyclePluginAnalyzer
{

    @Requirement( role = LifecycleMapping.class )
    private Map<String, LifecycleMapping> lifecycleMappings;

    @Requirement
    private DefaultLifecycles defaultLifeCycles;

    @Requirement( hint = DefaultLifecycleMappingDelegate.HINT )
    private LifecycleMappingDelegate defaultLifecycleMappingDelegate;

    @Requirement
    private Map<String, LifecycleMappingDelegate> lifecycleMappingDelegates;

    @Requirement
    private Logger logger;

    public DefaultLifecyclePluginAnalyzer()
    {
    }

    // These methods deal with construction intact Plugin object that look like they come from a standard
    // <plugin/> block in a Maven POM. We have to do some wiggling to pull the sources of information
    // together and this really shows the problem of constructing a sensible default configuration but
    // it's all encapsulated here so it appears normalized to the POM builder.
    // We are going to take the project packaging and find all plugin in the default lifecycle and create
    // fully populated Plugin objects, including executions with goals and default configuration taken
    // from the plugin.xml inside a plugin.
    public Set<Plugin> getPluginsBoundByDefaultToAllLifecycles( String packaging )
        throws LifecycleMappingNotFoundException
    {
        if ( logger.isDebugEnabled() )
        {
            logger.debug( "Looking up lifecyle mappings for packaging " + packaging + " from "
                              + Thread.currentThread().getContextClassLoader() );
        }

        LifecycleMapping lifecycleMappingForPackaging = lifecycleMappings.get( packaging );

        if ( lifecycleMappingForPackaging == null )
        {
            throw new LifecycleMappingNotFoundException( packaging );
        }

        Map<Plugin, Plugin> plugins = new LinkedHashMap<>();

        for ( Lifecycle lifecycle : getOrderedLifecycles() )
        {
            org.apache.maven.lifecycle.mapping.Lifecycle lifecycleConfiguration =
                lifecycleMappingForPackaging.getLifecycles().get( lifecycle.getId() );

            Map<String, LifecyclePhase> phaseToGoalMapping = null;

            if ( lifecycleConfiguration != null )
            {
                phaseToGoalMapping = lifecycleConfiguration.getLifecyclePhases();
            }
            else if ( lifecycle.getDefaultLifecyclePhases() != null )
            {
                phaseToGoalMapping = lifecycle.getDefaultLifecyclePhases();
            }

            if ( phaseToGoalMapping != null )
            {
                for ( Map.Entry<String, LifecyclePhase> goalsForLifecyclePhase : phaseToGoalMapping.entrySet() )
                {
                    String phase = goalsForLifecyclePhase.getKey();
                    LifecyclePhase goals = goalsForLifecyclePhase.getValue();
                    if ( goals != null )
                    {
                        parseLifecyclePhaseDefinitions( plugins, phase, goals );
                    }
                }
            }
        }

        return plugins.keySet();
    }

    @Override
    public Set<Plugin> getDefaultBuildPlugins( final Model model, final Set<String> goals )
        throws LifecycleMappingNotFoundException
    {
        final Map<Plugin, Plugin> defaultBuildPlugins = new LinkedHashMap<>();
        final Set<String> executedLifecycles = new HashSet<>();

        // Plugins of lifecycles of the 'MavenExecutionRequest' goals or the project default goals.
        if ( goals != null )
        {
            if ( !goals.isEmpty() )
            {
                // Command line goals.
                for ( final String goal : goals )
                {
                    if ( !isGoalSpecification( goal ) )
                    {
                        final Lifecycle lifecycle = this.defaultLifeCycles.get( goal );

                        if ( lifecycle != null )
                        {
                            executedLifecycles.add( lifecycle.getId() );
                        }
                    }
                }
            }
            else if ( model.getBuild() != null && model.getBuild().getDefaultGoal() != null )
            {
                // No command line goals -> default goal(s).
                // Copied from 'DefaultLifecycleTaskSegmentCalculator'.
                if ( !StringUtils.isEmpty( model.getBuild().getDefaultGoal() ) )
                {
                    for ( final String goal
                              : Arrays.asList( StringUtils.split( model.getBuild().getDefaultGoal() ) ) )
                    {
                        if ( !isGoalSpecification( goal ) )
                        {
                            final Lifecycle lifecycle = this.defaultLifeCycles.get( goal );

                            if ( lifecycle != null )
                            {
                                executedLifecycles.add( lifecycle.getId() );
                            }
                        }
                    }
                }
            }
        }

        // If nothing is to be executed via MavenExecutionRequest or default goals from the model, no need to provide
        // anything as nothing will ever get executed.
        if ( !executedLifecycles.isEmpty() )
        {
            // Plugins of lifecycles plugin executions are bound to manually.
            if ( model.getBuild() != null && model.getBuild().getPlugins() != null )
            {
                for ( final Plugin plugin : model.getBuild().getPlugins() )
                {
                    executedLifecycles.addAll( this.getLifecycleRequirements( plugin ) );
                }
            }

            // Plugins of lifecycles required by lifecycle mapping delegates.
            for ( final Lifecycle lifecycle : this.defaultLifeCycles.getLifeCycles() )
            {
                // Keep in sync with
                //   DefaultLifecycleExecutionPlanCalculator#calculateLifecycleMappings( MavenSession session,
                //                                                                       MavenProject project,
                //                                                                       String lifecyclePhase )
                final LifecycleMappingDelegate lifecycleMappingDelegate =
                    Arrays.binarySearch( DefaultLifecycles.STANDARD_LIFECYCLES, lifecycle.getId() ) >= 0
                        ? defaultLifecycleMappingDelegate
                        : lifecycleMappingDelegates.containsKey( lifecycle.getId() )
                              ? lifecycleMappingDelegates.get( lifecycle.getId() )
                              : defaultLifecycleMappingDelegate;

                for ( final String id : lifecycleMappingDelegate.getRequiredLifecycles() )
                {
                    executedLifecycles.add( id );
                }
            }
        }

        for ( final Lifecycle lifecycle : this.getOrderedLifecycles() )
        {
            if ( executedLifecycles.contains( lifecycle.getId() ) )
            {
                org.apache.maven.lifecycle.mapping.Lifecycle lifecycleConfiguration = null;

                if ( lifecycle.getId().equals( "default" ) )
                {
                    if ( logger.isDebugEnabled() )
                    {
                        logger.debug( String.format( "Looking up lifecyle mappings for packaging '%s' from '%s'",
                                                     model.getPackaging(),
                                                     Thread.currentThread().getContextClassLoader() ) );

                    }

                    final LifecycleMapping lifecycleMappingForPackaging =
                        this.lifecycleMappings.get( model.getPackaging() );

                    if ( lifecycleMappingForPackaging == null )
                    {
                        throw new LifecycleMappingNotFoundException( model.getPackaging() );
                    }

                    lifecycleConfiguration = lifecycleMappingForPackaging.getLifecycles().get( lifecycle.getId() );
                }

                Map<String, LifecyclePhase> phaseToGoalMapping = null;

                if ( lifecycleConfiguration != null )
                {
                    phaseToGoalMapping = lifecycleConfiguration.getLifecyclePhases();
                }
                else if ( lifecycle.getDefaultLifecyclePhases() != null )
                {
                    phaseToGoalMapping = lifecycle.getDefaultLifecyclePhases();
                }

                if ( phaseToGoalMapping != null )
                {
                    for ( final Map.Entry<String, LifecyclePhase> goalsForLifecyclePhase
                              : phaseToGoalMapping.entrySet() )
                    {
                        final String phase = goalsForLifecyclePhase.getKey();
                        final LifecyclePhase lifecyclePhase = goalsForLifecyclePhase.getValue();

                        if ( goals != null )
                        {
                            parseLifecyclePhaseDefinitions( defaultBuildPlugins, phase, lifecyclePhase );
                        }
                    }
                }
            }
        }

        return defaultBuildPlugins.keySet();
    }

    private Set<String> getLifecycleRequirements( final Plugin plugin )
    {
        final Set<String> executedLifecycles = new HashSet<>();

        for ( final PluginExecution pluginExecution : plugin.getExecutions() )
        {
            if ( pluginExecution.getPhase() != null )
            {
                final Lifecycle lifecycle = this.defaultLifeCycles.get( pluginExecution.getPhase() );

                if ( lifecycle != null )
                {
                    executedLifecycles.add( lifecycle.getId() );
                }
            }
            else
            {
                // TODO: If omitted, the goals will be bound to the default phase specified by the plugin.
                // TODO: Forked executions.
            }
        }

        return executedLifecycles;
    }

    // Copied from 'DefaultLifecycleTaskSegmentCalculator'.
    private static boolean isGoalSpecification( String task )
    {
        return task.indexOf( ':' ) >= 0;
    }

    private List<Lifecycle> getOrderedLifecycles()
    {
        // NOTE: The lifecycle order can affect implied execution ids so we better be deterministic.

        List<Lifecycle> lifecycles = new ArrayList<>( defaultLifeCycles.getLifeCycles() );

        Collections.sort( lifecycles, new Comparator<Lifecycle>()
        {

            public int compare( Lifecycle l1, Lifecycle l2 )
            {
                return l1.getId().compareTo( l2.getId() );
            }

        } );

        return lifecycles;
    }

    private void parseLifecyclePhaseDefinitions( Map<Plugin, Plugin> plugins, String phase, LifecyclePhase goals )
    {
        List<LifecycleMojo> mojos = goals.getMojos();
        if ( mojos != null )
        {

            for ( int i = 0; i < mojos.size(); i++ )
            {
                LifecycleMojo mojo = mojos.get( i );

                GoalSpec gs = parseGoalSpec( mojo.getGoal() );

                if ( gs == null )
                {
                    logger.warn( "Ignored invalid goal specification '" + mojo.getGoal()
                                     + "' from lifecycle mapping for phase " + phase );
                    continue;
                }

                Plugin plugin = new Plugin();
                plugin.setGroupId( gs.groupId );
                plugin.setArtifactId( gs.artifactId );
                plugin.setVersion( gs.version );

                Plugin existing = plugins.get( plugin );
                if ( existing != null )
                {
                    if ( existing.getVersion() == null )
                    {
                        existing.setVersion( plugin.getVersion() );
                    }
                    plugin = existing;
                }
                else
                {
                    plugins.put( plugin, plugin );
                }

                PluginExecution execution = new PluginExecution();
                execution.setId( getExecutionId( plugin, gs.goal ) );
                execution.setPhase( phase );
                execution.setPriority( i - mojos.size() );
                execution.getGoals().add( gs.goal );
                execution.setConfiguration( mojo.getConfiguration() );

                plugin.setDependencies( mojo.getDependencies() );
                plugin.getExecutions().add( execution );
            }
        }
    }

    private GoalSpec parseGoalSpec( String goalSpec )
    {
        GoalSpec gs = new GoalSpec();

        String[] p = StringUtils.split( goalSpec.trim(), ":" );

        if ( p.length == 3 )
        {
            // <groupId>:<artifactId>:<goal>
            gs.groupId = p[0];
            gs.artifactId = p[1];
            gs.goal = p[2];
        }
        else if ( p.length == 4 )
        {
            // <groupId>:<artifactId>:<version>:<goal>
            gs.groupId = p[0];
            gs.artifactId = p[1];
            gs.version = p[2];
            gs.goal = p[3];
        }
        else
        {
            // invalid
            gs = null;
        }

        return gs;
    }

    private String getExecutionId( Plugin plugin, String goal )
    {
        Set<String> existingIds = new HashSet<>();
        for ( PluginExecution execution : plugin.getExecutions() )
        {
            existingIds.add( execution.getId() );
        }

        String base = "default-" + goal;
        String id = base;

        for ( int index = 1; existingIds.contains( id ); index++ )
        {
            id = base + '-' + index;
        }

        return id;
    }

    static class GoalSpec
    {

        String groupId;

        String artifactId;

        String version;

        String goal;

    }

}
