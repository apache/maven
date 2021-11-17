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

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.maven.SessionScoped;
import org.apache.maven.caching.xml.Build;
import org.apache.maven.caching.xml.CacheConfig;
import org.apache.maven.caching.xml.CacheState;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.execution.MojoExecutionEvent;
import org.apache.maven.lifecycle.LifecycleExecutionException;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecution.Source;
import org.apache.maven.plugin.MojoExecutionRunner;
import org.apache.maven.plugin.MojosExecutionStrategy;
import org.apache.maven.project.MavenProject;
import org.eclipse.sisu.Priority;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.maven.caching.checksum.KeyUtils.getVersionlessProjectKey;
import static org.apache.maven.caching.xml.CacheState.DISABLED;
import static org.apache.maven.caching.xml.CacheState.INITIALIZED;


/**
 * <p>
 * Cache-enabled version of the MojoExecutor
 * </p>
 */
@SessionScoped
@Named
@Priority( 10 )
@SuppressWarnings( "unused" )
public class CachingMojosExecutionStrategy implements MojosExecutionStrategy
{
    private static final Logger LOGGER = LoggerFactory.getLogger( CachingMojosExecutionStrategy.class );

    private final CacheController cacheController;
    private final CacheConfig cacheConfig;
    private final MojoParametersListener mojoListener;
    private final LifecyclePhasesHelper lifecyclePhasesHelper;

    @Inject
    public CachingMojosExecutionStrategy(
            CacheController cacheController,
            CacheConfig cacheConfig,
            MojoParametersListener mojoListener,
            LifecyclePhasesHelper lifecyclePhasesHelper )
    {
        this.cacheController = cacheController;
        this.cacheConfig = cacheConfig;
        this.mojoListener = mojoListener;
        this.lifecyclePhasesHelper = lifecyclePhasesHelper;
    }

    public void execute( List<MojoExecution> mojoExecutions,
                         MavenSession session,
                         MojoExecutionRunner mojoExecutionRunner )
            throws LifecycleExecutionException
    {
        final MavenProject project = session.getCurrentProject();
        final Source source = getSource( mojoExecutions );

        // execute clean bound goals before restoring to not interfere/slowdown clean
        CacheState cacheState = DISABLED;
        CacheResult result = CacheResult.empty();
        if ( source == Source.LIFECYCLE )
        {
            List<MojoExecution> cleanPhase = lifecyclePhasesHelper.getCleanSegment( mojoExecutions );
            for ( MojoExecution mojoExecution : cleanPhase )
            {
                mojoExecutionRunner.run( mojoExecution );
            }
            cacheState = cacheConfig.initialize();
            if ( cacheState == INITIALIZED )
            {
                result = cacheController.findCachedBuild( session, project, mojoExecutions );
            }
        }

        boolean restorable = result.isSuccess() || result.isPartialSuccess();
        boolean restored = result.isSuccess(); // if partially restored need to save increment
        if ( restorable )
        {
            restored &= restoreProject( result, mojoExecutions, mojoExecutionRunner, cacheConfig );
        }
        else
        {
            for ( MojoExecution mojoExecution : mojoExecutions )
            {
                if ( source == Source.CLI
                        || mojoExecution.getLifecyclePhase() == null
                        || lifecyclePhasesHelper.isLaterPhaseThanClean( mojoExecution.getLifecyclePhase() ) )
                {
                    mojoExecutionRunner.run( mojoExecution );
                }
            }
        }

        if ( cacheState == INITIALIZED && ( !restorable || !restored ) )
        {
            final Map<String, MojoExecutionEvent> executionEvents = mojoListener.getProjectExecutions( project );
            cacheController.save( result, mojoExecutions, executionEvents );
        }

        if ( cacheConfig.isFailFast() && !result.isSuccess() )
        {
            throw new LifecycleExecutionException(
                    "Failed to restore project[" + getVersionlessProjectKey( project ) + "] from cache, failing build.",
                    project );
        }
    }

    private Source getSource( List<MojoExecution> mojoExecutions )
    {
        if ( mojoExecutions == null || mojoExecutions.isEmpty() )
        {
            return null;
        }
        for ( MojoExecution mojoExecution : mojoExecutions )
        {
            if ( mojoExecution.getSource() == Source.CLI )
            {
                return Source.CLI;
            }
        }
        return Source.LIFECYCLE;
    }

    private boolean restoreProject( CacheResult cacheResult,
                                    List<MojoExecution> mojoExecutions,
                                    MojoExecutionRunner mojoExecutionRunner,
                                    CacheConfig cacheConfig ) throws LifecycleExecutionException
    {
        final Build build = cacheResult.getBuildInfo();
        final MavenProject project = cacheResult.getContext().getProject();
        final MavenSession session = cacheResult.getContext().getSession();
        final List<MojoExecution> cachedSegment = lifecyclePhasesHelper.getCachedSegment( mojoExecutions, build );

        boolean restored = cacheController.restoreProjectArtifacts( cacheResult );
        if ( !restored )
        {
            LOGGER.info( "Cannot restore project artifacts, continuing with non cached build" );
            return false;
        }

        for ( MojoExecution cacheCandidate : cachedSegment )
        {
            if ( cacheController.isForcedExecution( project, cacheCandidate ) )
            {
                LOGGER.info( "Mojo execution is forced by project property: {}",
                             cacheCandidate.getMojoDescriptor().getFullGoalName() );
                mojoExecutionRunner.run( cacheCandidate );
            }
            else
            {
                restored = verifyCacheConsistency( cacheCandidate, build, project, session,
                        mojoExecutionRunner, cacheConfig );
                if ( !restored )
                {
                    break;
                }
            }
        }

        if ( !restored )
        {
            // cleanup partial state
            project.getArtifact().setFile( null );
            project.getArtifact().setResolved( false );
            project.getAttachedArtifacts().clear();
            mojoListener.remove( project );
            // build as usual
            for ( MojoExecution mojoExecution : cachedSegment )
            {
                mojoExecutionRunner.run( mojoExecution );
            }
        }

        for ( MojoExecution mojoExecution : lifecyclePhasesHelper.getPostCachedSegment( mojoExecutions, build ) )
        {
            mojoExecutionRunner.run( mojoExecution );
        }
        return restored;
    }

    private boolean verifyCacheConsistency( MojoExecution cacheCandidate,
                                            Build cachedBuild,
                                            MavenProject project,
                                            MavenSession session,
                                            MojoExecutionRunner mojoExecutionRunner,
                                            CacheConfig cacheConfig ) throws LifecycleExecutionException
    {
        AtomicBoolean consistent = new AtomicBoolean( true );
        final MojoExecutionManager mojoChecker = new MojoExecutionManager( project, cachedBuild,
                consistent, cacheConfig );

        if ( mojoChecker.needCheck( cacheCandidate, session ) )
        {
            try
            {
                // actual execution will not happen (if not forced). decision delayed to execution time
                // then all properties are resolved.
                cacheCandidate.setMojoExecutionManager( mojoChecker );
                mojoExecutionRunner.run( cacheCandidate );
            }
            finally
            {
                cacheCandidate.setMojoExecutionManager( null );
            }
        }
        else
        {
            LOGGER.info( "Skipping plugin execution (cached): {}",
                         cacheCandidate.getMojoDescriptor().getFullGoalName() );
        }

        return consistent.get();
    }

}
