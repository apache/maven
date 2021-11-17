package org.apache.maven.caching;

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

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.caching.xml.CacheConfig;
import org.apache.maven.caching.xml.DtoUtils;
import org.apache.maven.caching.xml.Build;
import org.apache.maven.caching.xml.config.TrackedProperty;
import org.apache.maven.caching.xml.build.CompletedExecution;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.MojoCheker;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.ReflectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.apache.maven.caching.CacheUtils.mojoExecutionKey;

/**
 * MojoExecutionManager
 */
public class MojoExecutionManager implements MojoCheker
{

    private static final Logger LOGGER = LoggerFactory.getLogger( MojoExecutionManager.class );

    private final long createdTimestamp;
    private final MavenProject project;
    private final Build build;
    private final AtomicBoolean consistent;
    private final CacheConfig cacheConfig;

    public MojoExecutionManager( MavenProject project,
                                 Build build,
                                 AtomicBoolean consistent,
                                 CacheConfig cacheConfig )
    {
        this.createdTimestamp = System.currentTimeMillis();
        this.project = project;
        this.build = build;
        this.consistent = consistent;
        this.cacheConfig = cacheConfig;
    }

    /**
     * runtime check is rather expensive for cached build and better to be avoided when possible
     */
    @Override
    public boolean needCheck( MojoExecution mojoExecution, MavenSession session )
    {
        return !cacheConfig.getTrackedProperties( mojoExecution ).isEmpty();
    }

    /**
     * this implementation has side effect of consistency check to force local save if local run is different than
     * cached
     *
     * @return false always returns false to prevent mojo execution
     */
    @Override
    public boolean check( MojoExecution execution, Mojo mojo, MavenSession session )
    {
        final CompletedExecution completedExecution = build.findMojoExecutionInfo( execution );
        final String fullGoalName = execution.getMojoDescriptor().getFullGoalName();

        if ( completedExecution != null && !isParamsMatched( execution, mojo, completedExecution ) )
        {
            LOGGER.info( "Mojo cached parameters mismatch with actual, forcing full project build. Mojo: {}",
                     fullGoalName );
            consistent.set( false );
        }

        if ( consistent.get() )
        {
            long elapsed = System.currentTimeMillis() - createdTimestamp;
            LOGGER.info( "Skipping plugin execution (reconciled in {} millis): {}", elapsed, fullGoalName );
        }

        LOGGER.debug( "Checked {}, resolved mojo: {}, cached params: {}",
                  fullGoalName, mojo, completedExecution );
        return false;
    }

    private boolean isParamsMatched( MojoExecution mojoExecution,
                                     Mojo mojo,
                                     CompletedExecution completedExecution )
    {
        List<TrackedProperty> tracked = cacheConfig.getTrackedProperties( mojoExecution );

        for ( TrackedProperty trackedProperty : tracked )
        {
            final String propertyName = trackedProperty.getPropertyName();

            String expectedValue = DtoUtils.findPropertyValue( propertyName, completedExecution );
            if ( expectedValue == null && trackedProperty.getDefaultValue() != null )
            {
                expectedValue = trackedProperty.getDefaultValue();
            }

            final String currentValue;
            try
            {
                currentValue = String.valueOf( ReflectionUtils.getValueIncludingSuperclasses( propertyName, mojo ) );
            }
            catch ( IllegalAccessException e )
            {
                LOGGER.error( "Cannot extract plugin property {} from mojo {}", propertyName, mojo, e );
                return false;
            }

            if ( !StringUtils.equals( currentValue, expectedValue ) )
            {
                if ( !StringUtils.equals( currentValue, trackedProperty.getSkipValue() ) )
                {
                    LOGGER.info( "Plugin parameter mismatch found. Parameter: {}, expected: {}, actual: {}",
                             propertyName, expectedValue, currentValue );
                    return false;
                }
                else
                {
                    LOGGER.warn( "Cache contains plugin execution with skip flag and might be incomplete. "
                                    + "Property: {}, execution {}",
                                    propertyName, mojoExecutionKey( mojoExecution ) );
                }
            }
        }
        return true;
    }

}
