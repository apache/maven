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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.Lifecycle;
import org.apache.maven.lifecycle.LifecycleMappingDelegate;
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
import org.apache.maven.project.MavenProject;

/**
 * Lifecycle mapping delegate component interface. Calculates project build execution plan given {@link Lifecycle} and
 * lifecycle phase. Standard lifecycles use plugin execution {@code <phase>} or mojo default lifecycle phase to
 * calculate the execution plan, but custom lifecycles can use alternative mapping strategies.
 */
@Named( DefaultLifecycleMappingDelegate.HINT )
@Singleton
public class DefaultLifecycleMappingDelegate
    implements LifecycleMappingDelegate
{
    public static final String HINT = "default";

    private final BuildPluginManager pluginManager;

    @Inject
    public DefaultLifecycleMappingDelegate( BuildPluginManager pluginManager )
    {
        this.pluginManager = pluginManager;
    }

    public Map<String, List<MojoExecution>> calculateLifecycleMappings( MavenSession session, MavenProject project,
                                                                        Lifecycle lifecycle, String lifecyclePhase )
        throws PluginNotFoundException, PluginResolutionException, PluginDescriptorParsingException,
        MojoNotFoundException, InvalidPluginDescriptorException
    {
        /*
         * Initialize mapping from lifecycle phase to bound mojos. The key set of this map denotes the phases the caller
         * is interested in, i.e. all phases up to and including the specified phase.
         */

        Map<String, Map<Integer, List<MojoExecution>>> mappings =
            new LinkedHashMap<>();

        for ( String phase : lifecycle.getPhases() )
        {
            Map<Integer, List<MojoExecution>> phaseBindings = new TreeMap<>();

            mappings.put( phase, phaseBindings );

            if ( phase.equals( lifecyclePhase ) )
            {
                break;
            }
        }

        /*
         * Grab plugin executions that are bound to the selected lifecycle phases from project. The effective model of
         * the project already contains the plugin executions induced by the project's packaging type. Remember, all
         * phases of interest and only those are in the lifecycle mapping, if a phase has no value in the map, we are
         * not interested in any of the executions bound to it.
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

        Map<String, List<MojoExecution>> lifecycleMappings = new LinkedHashMap<>();

        for ( Map.Entry<String, Map<Integer, List<MojoExecution>>> entry : mappings.entrySet() )
        {
            List<MojoExecution> mojoExecutions = new ArrayList<>();

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
        List<MojoExecution> mojoExecutions = phaseBindings.computeIfAbsent( priority, k -> new ArrayList<>() );

        mojoExecutions.add( mojoExecution );
    }

}
