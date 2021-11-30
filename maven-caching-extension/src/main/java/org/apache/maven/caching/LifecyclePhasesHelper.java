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
package org.apache.maven.caching;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.apache.maven.caching.xml.Build;
import org.apache.maven.lifecycle.DefaultLifecycles;
import org.apache.maven.lifecycle.Lifecycle;
import org.apache.maven.plugin.MojoExecution;

@Singleton
@Named
public class LifecyclePhasesHelper
{

    private final DefaultLifecycles defaultLifecycles;
    private final List<String> phases;
    private final String lastCleanPhase;

    @Inject
    public LifecyclePhasesHelper( DefaultLifecycles defaultLifecycles,
            @Named( "clean" ) Lifecycle cleanLifecycle )
    {
        this.defaultLifecycles = Objects.requireNonNull( defaultLifecycles );
        this.phases = defaultLifecycles.getLifeCycles().stream()
                .flatMap( lf -> lf.getPhases().stream() )
                .collect( Collectors.toList() );
        this.lastCleanPhase = CacheUtils.getLast( cleanLifecycle.getPhases() );
    }

    /**
     * Check if the given phase is later than the clean lifecycle.
     */
    public boolean isLaterPhaseThanClean( String phase )
    {
        return isLaterPhase( phase, lastCleanPhase );
    }

    public boolean isLaterPhaseThanBuild( String phase, Build build )
    {
        return isLaterPhase( phase, build.getHighestCompletedGoal() );
    }

    /**
     * Check if the given phase is later than the other in maven lifecycle.
     * Example: isLaterPhase("install", "clean") returns true;
     */
    public boolean isLaterPhase( String phase, String other )
    {
        if ( !phases.contains( phase ) )
        {
            throw new IllegalArgumentException( "Unsupported phase: " + phase );
        }
        if ( !phases.contains( other ) )
        {
            throw new IllegalArgumentException( "Unsupported phase: " + other );
        }

        return phases.indexOf( phase ) > phases.indexOf( other );
    }

    /**
     * Computes the list of mojos executions in the clean phase
     */
    public List<MojoExecution> getCleanSegment( List<MojoExecution> mojoExecutions )
    {
        List<MojoExecution> list = new ArrayList<>();
        for ( MojoExecution mojoExecution : mojoExecutions )
        {
            if ( mojoExecution.getLifecyclePhase() == null
                    || isLaterPhaseThanClean( mojoExecution.getLifecyclePhase() ) )
            {
                break;
            }
            list.add( mojoExecution );
        }
        return list;
    }

    /**
     * Computes the list of mojos executions that are cached.
     */
    public List<MojoExecution> getCachedSegment( List<MojoExecution> mojoExecutions, Build build )
    {
        List<MojoExecution> list = new ArrayList<>();
        for ( MojoExecution mojoExecution : mojoExecutions )
        {
            if ( !isLaterPhaseThanClean( mojoExecution.getLifecyclePhase() ) )
            {
                continue;
            }
            if ( isLaterPhaseThanBuild( mojoExecution.getLifecyclePhase(), build ) )
            {
                break;
            }
            list.add( mojoExecution );
        }
        return list;
    }

    /**
     * Computes the list of mojos executions that will have to be executed after cache restoration.
     */
    public List<MojoExecution> getPostCachedSegment( List<MojoExecution> mojoExecutions, Build build )
    {
        List<MojoExecution> list = new ArrayList<>();
        for ( MojoExecution mojoExecution : mojoExecutions )
        {
            if ( isLaterPhaseThanBuild( mojoExecution.getLifecyclePhase(), build ) )
            {
                list.add( mojoExecution );
            }
        }
        return list;
    }

}
