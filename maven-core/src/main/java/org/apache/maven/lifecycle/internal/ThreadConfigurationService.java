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

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @since 3.0
 */
@Component( role = ThreadConfigurationService.class )
public class ThreadConfigurationService
{
    @Requirement
    private Logger logger;

    private final int cpuCores;


    @SuppressWarnings( { "UnusedDeclaration" } )
    public ThreadConfigurationService()
    {
        cpuCores = Runtime.getRuntime().availableProcessors();
    }

    public ThreadConfigurationService( Logger logger, int cpuCores )
    {
        this.logger = logger;
        this.cpuCores = cpuCores;
    }


    public ExecutorService getExecutorService( String threadCountConfiguration, boolean perCoreThreadCount,
                                               int largestBuildListSize )
    {
        Integer threadCount = getThreadCount( threadCountConfiguration, perCoreThreadCount, largestBuildListSize );
        return getExecutorService( threadCount );


    }

    private ExecutorService getExecutorService( Integer threadCount )
    {
        if ( threadCount == null )
        {
            logger.info( "Building with unlimited threads" );
            return Executors.newCachedThreadPool();
        }

        logger.info( "Building with " + threadCount + " threads" );
        return Executors.newFixedThreadPool( threadCount );
    }

    /**
     * Returns the thread count to use or null for unlimited threads.
     *
     * @param threadCountConfiguration The property passed from the command line.
     * @param perCoreThreadCount       Indicates if the threa count should be scaled per cpu core.
     * @param largestBuildListSize     the size of the largest module list (the number of modules)
     * @return The number of threads to use or null if unlimited
     */

    Integer getThreadCount( String threadCountConfiguration, boolean perCoreThreadCount, int largestBuildListSize )
    {
        // Default to a value that is not larger than what we can use ;)
        float threadCount = Math.min( cpuCores, largestBuildListSize );
        if ( threadCountConfiguration != null )
        {
            try
            {
                threadCount = Float.parseFloat( threadCountConfiguration );
            }
            catch ( NumberFormatException e )
            {
                logger.warn(
                    "Couldn't parse thread count, will default to " + threadCount + ": " + threadCountConfiguration );
            }
        }
        if ( perCoreThreadCount )
        {
            threadCount = threadCount * cpuCores;
        }

        final int endResult = Math.round( threadCount );
        if ( logger.isDebugEnabled() )
        {
            logger.debug( "Thread pool size: " + endResult );
        }
        return endResult;
    }
}