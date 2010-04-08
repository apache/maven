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
import org.apache.maven.plugin.*;
import org.apache.maven.plugin.prefix.NoPluginFoundForPrefixException;
import org.apache.maven.plugin.version.PluginVersionResolutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;
import org.codehaus.plexus.util.StringUtils;

import java.util.*;

/**
 * @author Jason van Zyl
 * @author Kristian Rosenvold
 */
//TODO: The configuration for the lifecycle needs to be externalized so that I can use the annotations properly for the wiring and reference and external source for the lifecycle configuration.
public class DefaultLifecycles
    implements Initializable
{
    // @Configuration(source="org/apache/maven/lifecycle/lifecycles.xml")

    private List<Lifecycle> lifecycles;

    private List<Scheduling> schedules;

    /**
     * We use this to display all the lifecycles available and their phases to users. Currently this is primarily
     * used in the IDE integrations where a UI is presented to the user and they can select the lifecycle phase
     * they would like to execute.
     */
    private Map<String, Lifecycle> lifecycleMap;

    /**
     * We use this to map all phases to the lifecycle that contains it. This is used so that a user can specify the
     * phase they want to execute and we can easily determine what lifecycle we need to run.
     */
    private Map<String, Lifecycle> phaseToLifecycleMap;

    @SuppressWarnings({"UnusedDeclaration"})
    public DefaultLifecycles()
    {
    }

    public DefaultLifecycles( List<Lifecycle> lifecycles, List<Scheduling> schedules )
    {
        this.lifecycles = lifecycles;
        this.schedules = schedules;
    }

    public void initialize()
        throws InitializationException
    {
        lifecycleMap = new HashMap<String, Lifecycle>();

        // If people are going to make their own lifecycles then we need to tell people how to namespace them correctly so
        // that they don't interfere with internally defined lifecycles.

        phaseToLifecycleMap = new HashMap<String, Lifecycle>();

        for ( Lifecycle lifecycle : lifecycles )
        {
            for ( String phase : lifecycle.getPhases() )
            {
                // The first definition wins.
                if ( !phaseToLifecycleMap.containsKey( phase ) )
                {
                    phaseToLifecycleMap.put( phase, lifecycle );
                }
            }

            lifecycleMap.put( lifecycle.getId(), lifecycle );
        }
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
     * @param lifecyclePhaseName
     * @return
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
        return phaseToLifecycleMap.get( key );
    }

    public Map<String, Lifecycle> getPhaseToLifecycleMap()
    {
        return phaseToLifecycleMap;
    }

    public List<Lifecycle> getLifeCycles()
    {
        return lifecycles;
    }

    public List<Scheduling> getSchedules()
    {
        return schedules;
    }

    public String getLifecyclePhaseList()
    {
        Set<String> phases = new LinkedHashSet<String>();

        for ( Lifecycle lifecycle : lifecycles )
        {
            phases.addAll( lifecycle.getPhases() );
        }

        return StringUtils.join( phases.iterator(), ", " );
    }

}
