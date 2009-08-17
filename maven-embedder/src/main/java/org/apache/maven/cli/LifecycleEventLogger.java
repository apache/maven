package org.apache.maven.cli;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.embedder.MavenEmbedderLogger;
import org.apache.maven.lifecycle.AbstractLifecycleListener;
import org.apache.maven.lifecycle.LifecycleEvent;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;

/**
 * Logs lifecycle events to a user-supplied logger.
 * 
 * @author Benjamin Bentmann
 */
class LifecycleEventLogger
    extends AbstractLifecycleListener
{

    private final MavenEmbedderLogger logger;

    public LifecycleEventLogger( MavenEmbedderLogger logger )
    {
        if ( logger == null )
        {
            throw new IllegalArgumentException( "logger missing" );
        }

        this.logger = logger;
    }

    // TODO: log the events

    @Override
    public void sessionStarted( LifecycleEvent event )
    {
        if ( logger.isInfoEnabled() )
        {
            logger.info( "Build Order:" );

            logger.info( "" );

            for ( MavenProject project : event.getSession().getProjects() )
            {
                logger.info( project.getName() );
            }

            logger.info( "" );
        }
    }

    @Override
    public void projectSkipped( LifecycleEvent event )
    {
        if ( logger.isInfoEnabled() )
        {
            logger.info( "Skipping " + event.getProject().getName() );
            logger.info( "This project has been banned from the build due to previous failures." );
        }
    }

    @Override
    public void projectStarted( LifecycleEvent event )
    {
        if ( logger.isInfoEnabled() )
        {
            logger.info( "Building " + event.getProject().getName() );
        }
    }

    @Override
    public void mojoSkipped( LifecycleEvent event )
    {
        if ( logger.isWarnEnabled() )
        {
            logger.warn( "Goal " + event.getMojoExecution().getMojoDescriptor().getGoal()
                + " requires online mode for execution but Maven is currently offline, skipping" );
        }
    }

    @Override
    public void mojoStarted( LifecycleEvent event )
    {
        if ( logger.isInfoEnabled() )
        {
            MojoDescriptor md = event.getMojoExecution().getMojoDescriptor();
            PluginDescriptor pd = md.getPluginDescriptor();
            logger.info( "Executing " + pd.getArtifactId() + ':' + pd.getVersion() + ':' + md.getGoal() + " on "
                + event.getProject().getArtifactId() );
        }
    }

    @Override
    public void forkStarted( LifecycleEvent event )
    {
        if ( logger.isDebugEnabled() )
        {
            logger.debug( "Forking execution for " + event.getMojoExecution().getMojoDescriptor().getId() );
        }
    }

    @Override
    public void forkSucceeded( LifecycleEvent event )
    {
        if ( logger.isDebugEnabled() )
        {
            logger.debug( "Completed forked execution for " + event.getMojoExecution().getMojoDescriptor().getId() );
        }
    }

}
