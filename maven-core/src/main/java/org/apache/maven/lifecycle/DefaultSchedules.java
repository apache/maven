package org.apache.maven.lifecycle;

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

import org.apache.maven.lifecycle.internal.BuilderCommon;
import org.apache.maven.lifecycle.internal.ExecutionPlanItem;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;

import java.util.ArrayList;
import java.util.List;

/**
 * Defines scheduling information needed by weave mode.
 * 
 * @since 3.0
 * @author Kristian Rosenvold
 */
public class DefaultSchedules
{
    List<Scheduling> schedules;

    public DefaultSchedules()
    {
    }

    public DefaultSchedules( List<Scheduling> schedules )
    {
        this.schedules = schedules;
    }

    public List<ExecutionPlanItem> createExecutionPlanItem( MavenProject mavenProject, List<MojoExecution> executions )
    {
        BuilderCommon.attachToThread( mavenProject );

        List<ExecutionPlanItem> result = new ArrayList<ExecutionPlanItem>();
        for ( MojoExecution mojoExecution : executions )
        {
            String lifeCyclePhase = mojoExecution.getLifecyclePhase();
            final Scheduling scheduling = getScheduling( "default" );

            Schedule schedule = null;
            if ( scheduling != null )
            {
                schedule = scheduling.getSchedule( mojoExecution );
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

    Scheduling getScheduling( String lifecyclePhaseName )
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

    public List<Scheduling> getSchedules()
    {
        return schedules;
    }
}
