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
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl </a>
 * @version $Id$
 */
public class MavenSession
{
    private PlexusContainer container;

    private EventDispatcher eventDispatcher;

    private ReactorManager reactorManager;

    private boolean usingPOMsFromFilesystem = true;

    private MavenExecutionRequest request;

    public MavenSession( PlexusContainer container,
                         MavenExecutionRequest request,
                         EventDispatcher eventDispatcher,
                         ReactorManager reactorManager )
    {
        this.container = container;

        this.request = request;

        this.eventDispatcher = eventDispatcher;

        this.reactorManager = reactorManager;
    }

    public Map getPluginContext( PluginDescriptor pluginDescriptor,
                                 MavenProject project )
    {
        return reactorManager.getPluginContext( pluginDescriptor, project );
    }

    public PlexusContainer getContainer()
    {
        return container;
    }

    public ArtifactRepository getLocalRepository()
    {
        return request.getLocalRepository();
    }

    public List getGoals()
    {
        return request.getGoals();
    }

    public Properties getExecutionProperties()
    {
        return request.getProperties();
    }

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    public Object lookup( String role )
        throws ComponentLookupException
    {
        return container.lookup( role );
    }

    public Object lookup( String role,
                          String roleHint )
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
        return request.getSettings();
    }

    public List getSortedProjects()
    {
        return reactorManager.getSortedProjects();
    }

    public String getExecutionRootDirectory()
    {
        return request.getBaseDirectory();
    }

    public void setUsingPOMsFromFilesystem( boolean usingPOMsFromFilesystem )
    {
        this.usingPOMsFromFilesystem = usingPOMsFromFilesystem;
    }

    public boolean isUsingPOMsFromFilesystem()
    {
        return usingPOMsFromFilesystem;
    }

    public Date getStartTime()
    {
        return request.getStartTime();
    }

    public MavenExecutionRequest getRequest()
    {
        return request;
    }
}