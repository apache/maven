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

/**
 * @since 3.0
 * @author Kristian Rosenvold
 */
public class Schedule
{
    private String phase;

    private String upstreamPhase; // The upstream phase to lock to.

    private String pluginKey;

    private String mojoGoal;

    private boolean mojoSynchronized;
    // Indicates that this phase/mojo does not need to respect the reactor-dependency graph
    // (Module lifecycle order still must be respected )

    private boolean parallel;

    public Schedule()
    {
    }

    public Schedule( String phase, boolean mojoSynchronized, boolean parallel )
    {
        this.phase = phase;
        this.mojoSynchronized = mojoSynchronized;
        this.parallel = parallel;
    }


    public String getPhase()
    {
        return phase;
    }

    public void setPhase( String phase )
    {
        this.phase = phase;
    }

    public String getPluginKey()
    {
        return pluginKey;
    }

    public void setPluginKey( String pluginKey )
    {
        this.pluginKey = pluginKey;
    }

    public boolean isMojoSynchronized()
    {
        return mojoSynchronized;
    }

    public void setMojoSynchronized( boolean mojoSynchronized )
    {
        this.mojoSynchronized = mojoSynchronized;
    }


    public boolean isParallel()
    {
        return parallel;
    }

    public void setParallel( boolean parallel )
    {
        this.parallel = parallel;
    }

    public String getUpstreamPhase()
    {
        return upstreamPhase;
    }

    public void setUpstreamPhase( String upstreamPhase )
    {
        this.upstreamPhase = upstreamPhase;
    }

    public String getMojoGoal()
    {
        return mojoGoal;
    }

    public void setMojoGoal( String mojoGoal )
    {
        this.mojoGoal = mojoGoal;
    }

    public boolean hasUpstreamPhaseDefined()
    {
        return getUpstreamPhase() != null;
    }

    public boolean appliesTo( MojoExecution mojoExecution )
    {
        if ( pluginKey == null && mojoGoal == null )
        {
            return false;
        }

        boolean pluginKeyMatches = ( pluginKey == null ) || pluginKey.equals( mojoExecution.getPlugin().getKey() );

        boolean pluginGoalMatches = ( mojoGoal == null ) || mojoGoal.equals( mojoExecution.getGoal() );

        return pluginKeyMatches && pluginGoalMatches;
    }

    @Override
    public String toString()
    {
        return "Schedule{" + "phase='" + phase + "', upstreamPhase='" + upstreamPhase + "', pluginKey='"
            + pluginKey + "', mojoGoal='" + mojoGoal + "', mojoSynchronized=" + mojoSynchronized
            + ", parallel=" + parallel + '}';
    }
}
