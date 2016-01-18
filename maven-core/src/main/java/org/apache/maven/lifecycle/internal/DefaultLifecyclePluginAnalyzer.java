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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.maven.lifecycle.DefaultLifecycles;
import org.apache.maven.lifecycle.LifecyclePluginAnalyzer;
import org.apache.maven.lifecycle.Lifecycle;
import org.apache.maven.lifecycle.LifecycleMappingNotFoundException;
import org.apache.maven.lifecycle.mapping.LifecycleMapping;
import org.apache.maven.lifecycle.mapping.LifecycleMojo;
import org.apache.maven.lifecycle.mapping.LifecyclePhase;
import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.model.PluginManagement;
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
@Component( role = LifecyclePluginAnalyzer.class )
public class DefaultLifecyclePluginAnalyzer
    implements LifecyclePluginAnalyzer
{

    @Requirement( role = LifecycleMapping.class )
    private Map<String, LifecycleMapping> lifecycleMappings;

    @Requirement
    private DefaultLifecycles defaultLifeCycles;

    @Requirement
    private Logger logger;

    public DefaultLifecyclePluginAnalyzer()
    {
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Comment regarding these methods moved here from LifecycleExecuter:
    // -----------------------------------------------------------------------------------------------------------------
    // For a given project packaging find all the plugins that are bound to any registered
    // lifecycles. The project builder needs to now what default plugin information needs to be
    // merged into POM being built. Once the POM builder has this plugin information, versions can be assigned
    // by the POM builder because they will have to be defined in plugin management. Once this is setComplete then it
    // can be passed back so that the default configuration information can be populated.
    //
    // We need to know the specific version so that we can lookup the right version of the plugin descriptor
    // which tells us what the default configuration is.
    // -----------------------------------------------------------------------------------------------------------------
    // Comment regarding these methods moved here from DefaultLifecycleExecutor:
    // -----------------------------------------------------------------------------------------------------------------
    // These methods deal with construction intact Plugin object that look like they come from a standard
    // <plugin/> block in a Maven POM. We have to do some wiggling to pull the sources of information
    // together and this really shows the problem of constructing a sensible default configuration but
    // it's all encapsulated here so it appears normalized to the POM builder.
    // We are going to take the project packaging and find all plugin in the default lifecycle and create
    // fully populated Plugin objects, including executions with goals and default configuration taken
    // from the plugin.xml inside a plugin.
    @Override
    @Deprecated
    public Set<Plugin> getPluginsBoundByDefaultToAllLifecycles( String packaging )
        throws LifecycleMappingNotFoundException
    {
        if ( packaging == null )
        {
            throw new NullPointerException( "packaging" );
        }

        final Model model = new Model();
        model.setPackaging( packaging );

        final Set<Plugin> plugins = new HashSet<>( this.getLifecycleModel( model ).getBuild().getPlugins() );
        return Collections.unmodifiableSet( plugins );
    }

    @Override
    public Model getLifecycleModel( final Model model )
        throws LifecycleMappingNotFoundException
    {
        if ( model == null )
        {
            throw new NullPointerException( "model" );
        }

        final PluginManagement pluginManagement =
            model.getBuild() != null && model.getBuild().getPluginManagement() != null
                ? model.getBuild().getPluginManagement().clone()
                : null;

        final Model lifecycleModel = new Model();
        lifecycleModel.setBuild( new Build() );
        lifecycleModel.getBuild().setPluginManagement( pluginManagement != null
                                                           ? pluginManagement.clone()
                                                           : new PluginManagement() );

        for ( final Plugin managedPlugin : lifecycleModel.getBuild().getPluginManagement().getPlugins() )
        {
            managedPlugin.getExecutions().clear();
        }

        if ( logger.isDebugEnabled() )
        {
            logger.debug( "Looking up lifecyle mappings for packaging " + model.getPackaging() + " from "
                              + Thread.currentThread().getContextClassLoader() );

        }

        final LifecycleMapping lifecycleMappingForPackaging = this.lifecycleMappings.get( model.getPackaging() );

        if ( lifecycleMappingForPackaging == null )
        {
            throw new LifecycleMappingNotFoundException( model.getPackaging() );
        }

        final Map<Plugin, Plugin> plugins = new LinkedHashMap<>();

        for ( Lifecycle lifecycle : getOrderedLifecycles() )
        {
            final org.apache.maven.lifecycle.mapping.Lifecycle lifecycleConfiguration =
                lifecycleMappingForPackaging.getLifecycles().get( lifecycle.getId() );

            final Map<String, LifecyclePhase> phaseToGoalMapping =
                lifecycleConfiguration != null
                    ? lifecycleConfiguration.getLifecyclePhases()
                    : lifecycle.getDefaultLifecyclePhases() != null
                          ? lifecycle.getDefaultLifecyclePhases()
                          : null;

            if ( phaseToGoalMapping != null )
            {
                for ( final Map.Entry<String, LifecyclePhase> goalsForLifecyclePhase : phaseToGoalMapping.entrySet() )
                {
                    final String phase = goalsForLifecyclePhase.getKey();
                    final LifecyclePhase goals = goalsForLifecyclePhase.getValue();

                    if ( goals != null )
                    {
                        parseLifecyclePhaseDefinitions( plugins, phase, goals, lifecycle, lifecycleModel,
                                                        pluginManagement );

                    }
                }
            }
        }

        lifecycleModel.getBuild().getPlugins().addAll( plugins.keySet() );

        return lifecycleModel;
    }

    private List<Lifecycle> getOrderedLifecycles()
    {
        // NOTE: The lifecycle order can affect implied execution ids so we better be deterministic.

        List<Lifecycle> lifecycles = new ArrayList<>( defaultLifeCycles.getLifeCycles() );

        Collections.sort( lifecycles, new Comparator<Lifecycle>()
        {

            @Override
            public int compare( Lifecycle l1, Lifecycle l2 )
            {
                return l1.getId().compareTo( l2.getId() );
            }

        } );

        return lifecycles;
    }

    private void parseLifecyclePhaseDefinitions( Map<Plugin, Plugin> plugins, String phase, LifecyclePhase goals,
                                                 final Lifecycle lifecycle, final Model lifecycleModel,
                                                 final PluginManagement pluginManagement )
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
                                     + "' from lifecycle mapping for phase '" + phase + "'" );
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

                if ( pluginManagement != null )
                {
                    final Plugin managedPlugin = this.getManagedPlugin( pluginManagement, plugin );

                    if ( managedPlugin != null )
                    {
                        final List<PluginExecution> defaultExecutions =
                            new ArrayList<>( managedPlugin.getExecutions().size() );

                        for ( final PluginExecution pluginExecution : managedPlugin.getExecutions() )
                        {
                            // What if the plugin's default phase (== null) is not from the lifecyle?
                            if ( pluginExecution.getPhase() == null
                                     || lifecycle.getPhases().contains( pluginExecution.getPhase() ) )
                            {
                                defaultExecutions.add( pluginExecution );
                            }
                        }

                        final Plugin defaultManagedPlugin =
                            this.getManagedPlugin( lifecycleModel.getBuild().getPluginManagement(),
                                                   managedPlugin );

                        for ( final PluginExecution pluginExecution : defaultExecutions )
                        {
                            defaultManagedPlugin.addExecution( pluginExecution );
                        }

                        managedPlugin.getExecutions().removeAll( defaultExecutions );
                    }
                }
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

    private Plugin getManagedPlugin( final PluginManagement pluginManagement, final Plugin plugin )
    {
        Plugin managedPlugin = null;
        final String key = plugin.getKey();

        if ( pluginManagement != null )
        {
            for ( int i = 0, s0 = pluginManagement.getPlugins().size(); i < s0; i++ )
            {
                final Plugin current = pluginManagement.getPlugins().get( i );

                if ( current.getKey().equals( key ) )
                {
                    managedPlugin = current;
                    break;
                }
            }
        }

        return managedPlugin;
    }

    static class GoalSpec
    {

        String groupId;

        String artifactId;

        String version;

        String goal;

    }

}
