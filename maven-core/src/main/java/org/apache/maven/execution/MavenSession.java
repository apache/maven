package org.apache.maven.execution;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.monitor.event.EventDispatcher;
import org.apache.maven.plugin.mapping.PluginMappingManager;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;

import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl </a>
 * @version $Id$
 */
public class MavenSession
{
    private PlexusContainer container;

    private ArtifactRepository localRepository;

    private List goals;

    private EventDispatcher eventDispatcher;
    
    private PluginMappingManager pluginMappingManager;

    // TODO: make this the central one, get rid of build settings...
    private final Settings settings;

    private List sortedProjects;

    private final String executionRootDir;

    public MavenSession( PlexusContainer container, Settings settings,
                         ArtifactRepository localRepository, EventDispatcher eventDispatcher, List sortedProjects, 
                         List goals, String executionRootDir )
    {
        this.container = container;

        this.settings = settings;

        this.localRepository = localRepository;

        this.eventDispatcher = eventDispatcher;
        
        this.sortedProjects = sortedProjects;

        this.goals = goals;
        
        this.executionRootDir = executionRootDir;
    }

    public PlexusContainer getContainer()
    {
        return container;
    }

    public ArtifactRepository getLocalRepository()
    {
        return localRepository;
    }

    public List getGoals()
    {
        return goals;
    }

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    public Object lookup( String role )
        throws ComponentLookupException
    {
        return container.lookup( role );
    }

    public Object lookup( String role, String roleHint )
        throws ComponentLookupException
    {
        return container.lookup( role, roleHint );
    }

    public List lookupList( String role )
        throws ComponentLookupException
    {
        return container.lookupList( role );
    }

    public Map lookupMap( String role )
        throws ComponentLookupException
    {
        return container.lookupMap( role );
    }

    public EventDispatcher getEventDispatcher()
    {
        return eventDispatcher;
    }

    public Settings getSettings()
    {
        return settings;
    }
    
    public void setPluginMappingManager( PluginMappingManager pluginMappingManager )
    {
        this.pluginMappingManager = pluginMappingManager;
    }
    
    public PluginMappingManager getPluginMappingManager()
    {
        return pluginMappingManager;
    }
    
    public List getSortedProjects()
    {
        return sortedProjects;
    }
    
    public String getExecutionRootDirectory()
    {
        return executionRootDir;
    }
}