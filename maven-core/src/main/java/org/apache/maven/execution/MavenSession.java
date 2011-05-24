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
import org.apache.maven.artifact.repository.RepositoryCache;
import org.apache.maven.monitor.event.EventDispatcher;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.repository.automirror.MirrorRoutingTable;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.sonatype.aether.RepositorySystemSession;

import java.io.File;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Jason van Zyl
 */
public class MavenSession
    implements Cloneable
{
    private final PlexusContainer container;

    private final MavenExecutionRequest request;

    private MavenExecutionResult result;

    private RepositorySystemSession repositorySession;

    private final Settings settings;

    private Properties executionProperties;

    private MavenProject currentProject;

    /**
     * These projects have already been topologically sorted in the {@link org.apache.maven.Maven} component before
     * being passed into the session.
     */
    private List<MavenProject> projects;

    private MavenProject topLevelProject;

    private ProjectDependencyGraph projectDependencyGraph;

    private boolean parallel;

    private final Map<String, Map<String, Map<String, Object>>> pluginContextsByProjectAndPluginKey =
        new ConcurrentHashMap<String, Map<String, Map<String, Object>>>();

    @Deprecated
    public MavenSession( final PlexusContainer container, final MavenExecutionRequest request,
                         final MavenExecutionResult result, final MavenProject project )
    {
        this( container, request, result, Arrays.asList( new MavenProject[] { project } ) );
    }

    @Deprecated
    public MavenSession( final PlexusContainer container, final Settings settings,
                         final ArtifactRepository localRepository, final EventDispatcher eventDispatcher,
                         final ReactorManager unused, final List<String> goals, final String executionRootDir,
                         final Properties executionProperties, final Date startTime )
    {
        this( container, settings, localRepository, eventDispatcher, unused, goals, executionRootDir,
              executionProperties, null, startTime );
    }

    @Deprecated
    public MavenSession( final PlexusContainer container, final Settings settings,
                         final ArtifactRepository localRepository, final EventDispatcher eventDispatcher,
                         final ReactorManager unused, final List<String> goals, final String executionRootDir,
                         final Properties executionProperties, final Properties userProperties, final Date startTime )
    {
        this.container = container;
        this.settings = settings;
        this.executionProperties = executionProperties;
        request = new DefaultMavenExecutionRequest();
        request.setUserProperties( userProperties );
        request.setLocalRepository( localRepository );
        request.setGoals( goals );
        request.setBaseDirectory( ( executionRootDir != null ) ? new File( executionRootDir ) : null );
        request.setStartTime( startTime );
    }

    @Deprecated
    public MavenSession( final PlexusContainer container, final MavenExecutionRequest request,
                         final MavenExecutionResult result, final List<MavenProject> projects )
    {
        this.container = container;
        this.request = request;
        this.result = result;
        settings = new SettingsAdapter( request );
        setProjects( projects );
    }

    public MavenSession( final PlexusContainer container, final RepositorySystemSession repositorySession,
                         final MavenExecutionRequest request, final MavenExecutionResult result )
    {
        this.container = container;
        this.request = request;
        this.result = result;
        settings = new SettingsAdapter( request );
        this.repositorySession = repositorySession;
    }

    public void setProjects( final List<MavenProject> projects )
    {
        if ( !projects.isEmpty() )
        {
            currentProject = projects.get( 0 );
            topLevelProject = currentProject;
            for ( final MavenProject project : projects )
            {
                if ( project.isExecutionRoot() )
                {
                    topLevelProject = project;
                    break;
                }
            }
        }
        else
        {
            currentProject = null;
            topLevelProject = null;
        }
        this.projects = projects;
    }

    @Deprecated
    public PlexusContainer getContainer()
    {
        return container;
    }

    @Deprecated
    public Object lookup( final String role )
        throws ComponentLookupException
    {
        return container.lookup( role );
    }

    @Deprecated
    public Object lookup( final String role, final String roleHint )
        throws ComponentLookupException
    {
        return container.lookup( role, roleHint );
    }

    @Deprecated
    public List<Object> lookupList( final String role )
        throws ComponentLookupException
    {
        return container.lookupList( role );
    }

    @Deprecated
    public Map<String, Object> lookupMap( final String role )
        throws ComponentLookupException
    {
        return container.lookupMap( role );
    }

    @Deprecated
    public RepositoryCache getRepositoryCache()
    {
        return null;
    }

    public ArtifactRepository getLocalRepository()
    {
        return request.getLocalRepository();
    }

    public List<String> getGoals()
    {
        return request.getGoals();
    }

    /**
     * Gets the user properties to use for interpolation and profile activation. The user properties have been
     * configured directly by the user on his discretion, e.g. via the {@code -Dkey=value} parameter on the command
     * line.
     * 
     * @return The user properties, never {@code null}.
     */
    public Properties getUserProperties()
    {
        return request.getUserProperties();
    }

    /**
     * Gets the system properties to use for interpolation and profile activation. The system properties are collected
     * from the runtime environment like {@link System#getProperties()} and environment variables.
     * 
     * @return The system properties, never {@code null}.
     */
    public Properties getSystemProperties()
    {
        return request.getSystemProperties();
    }

    /**
     * @deprecated Use either {@link #getUserProperties()} or {@link #getSystemProperties()}.
     */
    @Deprecated
    public Properties getExecutionProperties()
    {
        if ( executionProperties == null )
        {
            executionProperties = new Properties();
            executionProperties.putAll( request.getSystemProperties() );
            executionProperties.putAll( request.getUserProperties() );
        }

        return executionProperties;
    }

    public Settings getSettings()
    {
        return settings;
    }

    public List<MavenProject> getProjects()
    {
        return projects;
    }

    @Deprecated
    public List<MavenProject> getSortedProjects()
    {
        return getProjects();
    }

    public String getExecutionRootDirectory()
    {
        return request.getBaseDirectory();
    }

    public boolean isUsingPOMsFromFilesystem()
    {
        return request.isProjectPresent();
    }

    public MavenExecutionRequest getRequest()
    {
        return request;
    }

    public void setCurrentProject( final MavenProject currentProject )
    {
        this.currentProject = currentProject;
    }

    public MavenProject getCurrentProject()
    {
        return currentProject;
    }

    public ProjectBuildingRequest getProjectBuildingRequest()
    {
        return request.getProjectBuildingRequest().setRepositorySession( getRepositorySession() );
    }

    public List<String> getPluginGroups()
    {
        return request.getPluginGroups();
    }

    public boolean isOffline()
    {
        return request.isOffline();
    }

    public MavenProject getTopLevelProject()
    {
        return topLevelProject;
    }

    public MavenExecutionResult getResult()
    {
        return result;
    }

    // Backward compat

    public Map<String, Object> getPluginContext( final PluginDescriptor plugin, final MavenProject project )
    {
        final String projectKey = project.getId();

        Map<String, Map<String, Object>> pluginContextsByKey = pluginContextsByProjectAndPluginKey.get( projectKey );

        if ( pluginContextsByKey == null )
        {
            pluginContextsByKey = new ConcurrentHashMap<String, Map<String, Object>>();

            pluginContextsByProjectAndPluginKey.put( projectKey, pluginContextsByKey );
        }

        final String pluginKey = plugin.getPluginLookupKey();

        Map<String, Object> pluginContext = pluginContextsByKey.get( pluginKey );

        if ( pluginContext == null )
        {
            pluginContext = new ConcurrentHashMap<String, Object>();

            pluginContextsByKey.put( pluginKey, pluginContext );
        }

        return pluginContext;
    }

    public ProjectDependencyGraph getProjectDependencyGraph()
    {
        return projectDependencyGraph;
    }

    public void setProjectDependencyGraph( final ProjectDependencyGraph projectDependencyGraph )
    {
        this.projectDependencyGraph = projectDependencyGraph;
    }

    public String getReactorFailureBehavior()
    {
        return request.getReactorFailureBehavior();
    }

    @Override
    public MavenSession clone()
    {
        try
        {
            return (MavenSession) super.clone();
        }
        catch ( final CloneNotSupportedException e )
        {
            throw new RuntimeException( "Bug", e );
        }
    }

    private String getId( final MavenProject project )
    {
        return project.getGroupId() + ':' + project.getArtifactId() + ':' + project.getVersion();
    }

    @Deprecated
    public EventDispatcher getEventDispatcher()
    {
        return null;
    }

    public Date getStartTime()
    {
        return request.getStartTime();
    }

    public boolean isParallel()
    {
        return parallel;
    }

    public void setParallel( final boolean parallel )
    {
        this.parallel = parallel;
    }

    public RepositorySystemSession getRepositorySession()
    {
        return repositorySession;
    }

    public MirrorRoutingTable getMirrorRoutingTable()
    {
        return request.getMirrorRoutingTable();
    }

}
