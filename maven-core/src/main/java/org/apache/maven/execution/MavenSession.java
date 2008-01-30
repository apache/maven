package org.apache.maven.execution;

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

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.lifecycle.plan.BuildPlan;
import org.apache.maven.monitor.event.EventDispatcher;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;
import org.apache.maven.realm.MavenRealmManager;
import org.apache.maven.reporting.MavenReport;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Stack;

/**
 * @author Jason van Zyl
 * @version $Id$
 */
public class MavenSession
{
    private PlexusContainer container;

    private EventDispatcher eventDispatcher;

    private ReactorManager reactorManager;

    private boolean usingPOMsFromFilesystem = true;

    private MavenExecutionRequest request;

    private MavenProject currentProject;

    private Stack forkedProjectStack = new Stack();

    private Map reports = new LinkedHashMap();

    private Map buildPlans = new HashMap();

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

    public MavenRealmManager getRealmManager()
    {
        return request.getRealmManager();
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

    public boolean isUsingPOMsFromFilesystem()
    {
        return request.isProjectPresent();
    }

    public Date getStartTime()
    {
        return request.getStartTime();
    }

    public MavenExecutionRequest getRequest()
    {
        return request;
    }

    /**
     * Push the existing currentProject onto the forked-project stack, and set the specified project
     * as the new current project. This signifies the beginning of a new forked-execution context.
     */
    public void addForkedProject( MavenProject project )
    {
        forkedProjectStack.push( currentProject );
        currentProject = project;
    }

    /**
     * Peel off the last forked project from the stack, and restore it as the currentProject. This
     * signifies the cleanup of a completed forked-execution context.
     */
    public MavenProject removeForkedProject()
    {
        if ( !forkedProjectStack.isEmpty() )
        {
            MavenProject lastCurrent = currentProject;
            currentProject = (MavenProject) forkedProjectStack.pop();

            return lastCurrent;
        }

        return null;
    }

    public void setCurrentProject( MavenProject currentProject )
    {
        this.currentProject = currentProject;
    }

    /**
     * Return the current project for use in a mojo execution.
     */
    public MavenProject getCurrentProject()
    {
        return currentProject;
    }

    /**
     * Retrieve the list of reports ({@link MavenReport} instances) that have been executed against
     * this project, for use in another mojo's execution.
     */
    public List getReports()
    {
        if ( reports == null )
        {
            return Collections.EMPTY_LIST;
        }

        return new ArrayList( reports.values() );
    }

    /**
     * Clear the reports for this project
     */
    public void clearReports()
    {
        reports.clear();
    }

    /**
     * Add a newly-executed report ({@link MavenReport} instance) to the reports collection, for
     * future reference.
     */
    public void addReport( MojoDescriptor mojoDescriptor, MavenReport report )
    {
        reports.put( mojoDescriptor, report );
    }

    public Set getReportMojoDescriptors()
    {
        if ( reports == null )
        {
            return Collections.EMPTY_SET;
        }

        return reports.keySet();
    }

    public BuildPlan getBuildPlan( String projectId )
    {
        return (BuildPlan) buildPlans.get( projectId );
    }

    public BuildPlan getBuildPlan( MavenProject project )
    {
        return (BuildPlan) buildPlans.get( project.getId() );
    }

    public void setBuildPlan( MavenProject project, BuildPlan buildPlan )
    {
        buildPlans.put( project.getId(), buildPlan );
    }

    public Map getBuildPlans()
    {
        return buildPlans;
    }

}