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

import org.apache.maven.plugin.MojoExecution;

import java.util.List;

/**
 * Class Scheduling.
 * 
 * @since 3.0
 */
public class Scheduling
{
    private String lifecycle;

    private List<Schedule> schedules;

    public Scheduling()
    {
    }

    public Scheduling( String lifecycle, List<Schedule> schedules )
    {
        this.lifecycle = lifecycle;
        this.schedules = schedules;
    }

    public String getLifecycle()
    {
        return lifecycle;
    }

    public void setLifecycle( String lifecycle )
    {
        this.lifecycle = lifecycle;
    }

    public List<Schedule> getSchedules()
    {
        return schedules;
    }


    public Schedule getSchedule( String phaseName )
    {
        if ( phaseName != null )
        {
            for ( Schedule schedule : schedules )
            {
                if ( phaseName.equals( schedule.getPhase() ) )
                {
                    return schedule;
                }
            }
        }

        return null;
    }

    public Schedule getSchedule( MojoExecution mojoExecution )
    {
        if ( mojoExecution != null )
        {
            for ( Schedule schedule : schedules )
            {
                if ( schedule.appliesTo( mojoExecution ) )
                {
                    return schedule;
                }
            }
        }

        return null;
    }

    public void setSchedules( List<Schedule> schedules )
    {
        this.schedules = schedules;
    }
}