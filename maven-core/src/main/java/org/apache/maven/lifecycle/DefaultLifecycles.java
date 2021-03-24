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

import org.codehaus.plexus.logging.Logger;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

/**
 * @since 3.0
 * @author Jason van Zyl
 * @author Kristian Rosenvold
 */
// TODO The configuration for the lifecycle needs to be externalized so that I can use the annotations properly for the
// wiring and reference and external source for the lifecycle configuration.
@Named
@Singleton
public class DefaultLifecycles
{
    public static final String[] STANDARD_LIFECYCLES = { "default", "clean", "site", "wrapper" };

    // @Configuration(source="org/apache/maven/lifecycle/lifecycles.xml")

    private final Map<String, Lifecycle> lifecyclesMap;

    private final Logger logger;

    public DefaultLifecycles()
    {
        this.lifecyclesMap = null;
        this.logger = null;
    }

    @Inject
    public DefaultLifecycles( Map<String, Lifecycle> lifecyclesMap, Logger logger )
    {
        // Must keep the lifecyclesMap as is.
        // During initialization it only contains the default lifecycles.
        // However, extensions might add custom lifecycles later on.
        this.lifecyclesMap = lifecyclesMap;
        this.logger = logger;
    }

    /**
     * Get lifecycle based on phase
     *
     * @param phase
     * @return
     */
    public Lifecycle get( String phase )
    {
        return getPhaseToLifecycleMap().get( phase );
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

        Map<String, Lifecycle> phaseToLifecycleMap = new HashMap<>();

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
                    logger.warn( "Duplicated lifecycle phase " + phase + ". Defined in " + original.getId()
                        + " but also in " + lifecycle.getId() );
                }
            }
        }

        return phaseToLifecycleMap;
    }

    public List<Lifecycle> getLifeCycles()
    {
        List<String> lifecycleIds = Arrays.asList( STANDARD_LIFECYCLES );

        Comparator<String> comparator = ( l, r ) ->
        {
            int lx = lifecycleIds.indexOf( l );
            int rx = lifecycleIds.indexOf( r );

            if ( lx < 0 || rx < 0 )
            {
                return rx - lx;
            }
            else
            {
                return lx - rx;
            }
        };

        // ensure canonical order of standard lifecycles
        return lifecyclesMap.values().stream()
                                .peek( l -> Objects.requireNonNull( l.getId(), "A lifecycle must have an id." ) )
                                .sorted( Comparator.comparing( Lifecycle::getId, comparator ) )
                                .collect( Collectors.toList() );
    }

    public String getLifecyclePhaseList()
    {
        return getLifeCycles().stream()
                        .flatMap( l -> l.getPhases().stream() )
                        .collect( Collectors.joining( ", " ) );
    }

}
