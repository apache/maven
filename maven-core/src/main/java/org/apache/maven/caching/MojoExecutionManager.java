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
import org.apache.maven.caching.xml.BuildInfo;
import org.apache.maven.caching.xml.CacheConfig;
import org.apache.maven.caching.xml.DtoUtils;
import org.apache.maven.caching.xml.config.TrackedPropertyType;
import org.apache.maven.caching.xml.domain.CompletedExecutionType;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.MojoCheker;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.ReflectionUtils;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.apache.maven.caching.ProjectUtils.mojoExecutionKey;

/**
 * MojoExecutionManager
 */
public class MojoExecutionManager implements MojoCheker
{

    private final long createdTimestamp;
    private final Logger logger;
    private final MavenProject project;
    private final BuildInfo buildInfo;
    private final AtomicBoolean consistent;
    private final CacheController cacheController;
    private final CacheConfig cacheConfig;

    public MojoExecutionManager( MavenProject project,
                                 CacheController cacheController,
                                 BuildInfo buildInfo,
                                 AtomicBoolean consistent,
                                 Logger logger, CacheConfig cacheConfig )
    {
        this.createdTimestamp = System.currentTimeMillis();
        this.project = project;
        this.cacheController = cacheController;
        this.buildInfo = buildInfo;
        this.consistent = consistent;
        this.logger = logger;
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

        final CompletedExecutionType completedExecution = buildInfo.findMojoExecutionInfo( execution );
        final String fullGoalName = execution.getMojoDescriptor().getFullGoalName();

        if ( completedExecution != null && !isParamsMatched( project, execution, mojo, completedExecution ) )
        {
            logInfo( project,
                    "Mojo cached parameters mismatch with actual, forcing full project build. Mojo: " + fullGoalName );
            consistent.set( false );
        }

        if ( consistent.get() )
        {
            long elapsed = System.currentTimeMillis() - createdTimestamp;
            logInfo( project, "Skipping plugin execution (reconciled in " + elapsed + " millis): " + fullGoalName );
        }

        if ( logger.isDebugEnabled() )
        {
            logger.debug(
                    "[CACHE][" + project.getArtifactId() + "] Checked " + fullGoalName + ", resolved mojo: " + mojo
                            + ", cached params:" + completedExecution );
        }
        return false;
    }

    private boolean isParamsMatched( MavenProject project,
                                     MojoExecution mojoExecution,
                                     Mojo mojo,
                                     CompletedExecutionType completedExecution )
    {

        List<TrackedPropertyType> tracked = cacheConfig.getTrackedProperties( mojoExecution );

        for ( TrackedPropertyType trackedProperty : tracked )
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
                logError( project, "Cannot extract plugin property " + propertyName + " from mojo " + mojo, e );
                return false;
            }

            if ( !StringUtils.equals( currentValue, expectedValue ) )
            {
                if ( !StringUtils.equals( currentValue, trackedProperty.getSkipValue() ) )
                {
                    logInfo( project,
                            "Plugin parameter mismatch found. Parameter: " + propertyName + ", expected: "
                                    + expectedValue + ", actual: " + currentValue );
                    return false;
                }
                else
                {
                    logWarn( project,
                            "Cache contains plugin execution with skip flag and might be incomplete. Property: "
                                    + propertyName + ", execution: " + mojoExecutionKey( mojoExecution ) );
                }
            }
        }
        return true;
    }

    private void logInfo( MavenProject project, String message )
    {
        logger.info( "[CACHE][" + project.getArtifactId() + "] " + message );
    }

    private void logError( MavenProject project, String message, Exception e )
    {
        logger.error( "[CACHE][" + project.getArtifactId() + "] " + message, e );
    }

    private void logWarn( MavenProject project, String message )
    {
        logger.warn( "[CACHE][" + project.getArtifactId() + "] " + message );
    }

}
