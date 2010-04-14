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
package org.apache.maven.lifecycle;

import org.apache.maven.lifecycle.internal.BuilderCommon;
import org.apache.maven.lifecycle.internal.ExecutionPlanItem;
import org.apache.maven.plugin.InvalidPluginDescriptorException;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoNotFoundException;
import org.apache.maven.plugin.PluginDescriptorParsingException;
import org.apache.maven.plugin.PluginNotFoundException;
import org.apache.maven.plugin.PluginResolutionException;
import org.apache.maven.plugin.prefix.NoPluginFoundForPrefixException;
import org.apache.maven.plugin.version.PluginVersionResolutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Jason van Zyl
 * @author Kristian Rosenvold
 */
// TODO: The configuration for the lifecycle needs to be externalized so that I can use the annotations properly for the
// wiring and reference and external source for the lifecycle configuration.
public class DefaultLifecycles
{
    public static final String[] STANDARD_LIFECYCLES = { "default", "clean", "site" };

    // @Configuration(source="org/apache/maven/lifecycle/lifecycles.xml")

    private Map<String, Lifecycle> lifecycles;

    private Logger logger;

    private List<Scheduling> schedules;

    @SuppressWarnings( { "UnusedDeclaration" } )
    public DefaultLifecycles()
    {
    }

    public DefaultLifecycles( Map<String, Lifecycle> lifecycles, List<Scheduling> schedules, Logger logger )
    {
        this.lifecycles = new LinkedHashMap<String, Lifecycle>();
        this.schedules = schedules;
        this.logger = logger;
        this.lifecycles = lifecycles;
    }

    public List<ExecutionPlanItem> createExecutionPlanItem( MavenProject mavenProject, List<MojoExecution> executions )
        throws PluginNotFoundException, PluginResolutionException, LifecyclePhaseNotFoundException,
        PluginDescriptorParsingException, MojoNotFoundException, InvalidPluginDescriptorException,
        NoPluginFoundForPrefixException, LifecycleNotFoundException, PluginVersionResolutionException
    {
        BuilderCommon.attachToThread( mavenProject );

        List<ExecutionPlanItem> result = new ArrayList<ExecutionPlanItem>();
        for ( MojoExecution mojoExecution : executions )
        {
            String lifeCyclePhase = mojoExecution.getMojoDescriptor().getPhase();
            final Scheduling scheduling = getScheduling( "default" );
            Schedule schedule = null;
            if ( scheduling != null )
            {
                schedule = scheduling.getSchedule( mojoExecution.getPlugin() );
                if ( schedule == null )
                {
                    schedule = scheduling.getSchedule( lifeCyclePhase );
                }
            }
            result.add( new ExecutionPlanItem( mojoExecution, schedule ) );

        }
        return result;
    }

    /**
     * Gets scheduling associated with a given phase.
     * <p/>
     * This is part of the experimental weave mode and therefore not part of the public api.
     *
     * @param lifecyclePhaseName The name of the lifecycle phase
     * @return Schecduling information related to phase
     */

    private Scheduling getScheduling( String lifecyclePhaseName )
    {
        for ( Scheduling schedule : schedules )
        {
            if ( lifecyclePhaseName.equals( schedule.getLifecycle() ) )
            {
                return schedule;
            }
        }
        return null;
    }

    public Lifecycle get( String key )
    {
        return getPhaseToLifecycleMap().get( key );
    }

    /**
     * We use this to map all phases to the lifecycle that contains it. This is used so that a user can specify the
     * phase they want to execute and we can easily determine what lifecycle we need to run.
     * 
     * @return A map of lifecycles, indexed on id
     */
    public Map<String, Lifecycle> getPhaseToLifecycleMap()
    {
        // If people are going to make their own lifecycles then we need to tell people how to namespace them correctly
        // so that they don't interfere with internally defined lifecycles.

        HashMap<String, Lifecycle> phaseToLifecycleMap = new HashMap<String, Lifecycle>();

        for ( Lifecycle lifecycle : getLifeCycles() )
        {
            if ( logger.isDebugEnabled() )
            {
                logger.debug( "Lifecycle " + lifecycle );
            }

            for ( String phase : lifecycle.getPhases() )
            {
                // The first definition wins.
                if ( !phaseToLifecycleMap.containsKey( phase ) )
                {
                    phaseToLifecycleMap.put( phase, lifecycle );
                }
                else
                {
                    Lifecycle original = phaseToLifecycleMap.get( phase );
                    logger.warn(
                        "Duplicated lifecycle phase " + phase + ". Defined in " + original.getId() + " but also in " +
                            lifecycle.getId() );
                }
            }
        }

        return phaseToLifecycleMap;
    }

    public List<Lifecycle> getLifeCycles()
    {
        // ensure canonical order of standard lifecycles

        Map<String, Lifecycle> lifecycles = new LinkedHashMap<String, Lifecycle>( this.lifecycles );

        LinkedHashSet<String> lifecycleNames = new LinkedHashSet<String>( Arrays.asList( STANDARD_LIFECYCLES ) );
        lifecycleNames.addAll( lifecycles.keySet() );
        ArrayList<Lifecycle> result = new ArrayList<Lifecycle>();
        for ( String name : lifecycleNames )
        {
            result.add( lifecycles.get( name ) );
        }

        return result;
    }

    public List<Scheduling> getSchedules()
    {
        return schedules;
    }

    public String getLifecyclePhaseList()
    {
        Set<String> phases = new LinkedHashSet<String>();

        for ( Lifecycle lifecycle : lifecycles.values() )
        {
            phases.addAll( lifecycle.getPhases() );
        }

        return StringUtils.join( phases.iterator(), ", " );
    }

}
