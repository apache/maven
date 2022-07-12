package org.apache.maven.plugin.internal;

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

import javax.inject.Named;
import javax.inject.Singleton;

import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArraySet;

import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.execution.MavenSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Listens in session to collect plugin related information.
 *
 * @since TBD
 */
@Singleton
@Named
public final class SessionUsedPluginCollector
        extends AbstractMavenLifecycleParticipant
{
    private final Logger logger = LoggerFactory.getLogger( SessionUsedPluginCollector.class );

    private static final String DEPRECATED_PLUGIN_IDS_KEY =
            SessionUsedPluginCollector.class.getSimpleName() + ".deprecated";

    @Override
    public void afterSessionStart( MavenSession session )
    {
        session.getRepositorySession().getData().set( DEPRECATED_PLUGIN_IDS_KEY, new CopyOnWriteArraySet<>() );
    }

    @SuppressWarnings( "unchecked" )
    @Override
    public void afterSessionEnd( MavenSession session )
    {
        Set<String> deprecatedPlugins = (Set<String>) session
                .getRepositorySession().getData().get( DEPRECATED_PLUGIN_IDS_KEY );
        if ( deprecatedPlugins != null && !deprecatedPlugins.isEmpty() )
        {
            logger.warn( "Used pre-3.1 Maven plugins in session:" );
            for ( String pluginId : new TreeSet<>( deprecatedPlugins ) )
            {
                logger.warn( " * {}", pluginId );
            }
            logger.warn( "" );
            logger.warn( "Total of {}", deprecatedPlugins.size() );
            logger.warn( "Please update to newer versions of these plugins." );
            logger.warn( "" );
        }
    }

    @SuppressWarnings( "unchecked" )
    public void deprecatedPlugin( MavenSession session, String pluginId )
    {
        Set<String> deprecatedPlugins = (Set<String>) session
                .getRepositorySession().getData().get( DEPRECATED_PLUGIN_IDS_KEY );
        if ( deprecatedPlugins != null )
        {
            deprecatedPlugins.add( pluginId );
        }
    }
}
