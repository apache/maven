package org.apache.maven.execution;

/* ====================================================================
 *   Copyright 2001-2004 The Apache Software Foundation.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 * ====================================================================
 */

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.user.UserModel;
import org.apache.maven.monitor.event.EventDispatcher;
import org.apache.maven.monitor.logging.Log;
import org.apache.maven.plugin.PluginManager;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.util.dag.CycleDetectedException;
import org.codehaus.plexus.util.dag.DAG;
import org.codehaus.plexus.util.dag.TopologicalSorter;
import org.codehaus.plexus.util.dag.Vertex;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl </a>
 * @version $Id$
 */
public class MavenSession
{
    private PlexusContainer container;

    private MavenProject project;

    private ArtifactRepository localRepository;

    private PluginManager pluginManager;

    private Set remoteRepositories;

    private DAG dag;

    private List goals;

    private Map preGoalMappings;

    private Map postGoalMappings;

    private EventDispatcher eventDispatcher;

    private Log log;

    private final UserModel userModel;

    private List remoteArtifactRepos = Collections.EMPTY_LIST;

    public MavenSession( PlexusContainer container, PluginManager pluginManager, UserModel userModel,
        ArtifactRepository localRepository, EventDispatcher eventDispatcher, Log log, List goals )
    {
        this.container = container;

        this.pluginManager = pluginManager;

        this.userModel = userModel;

        this.localRepository = localRepository;

        this.eventDispatcher = eventDispatcher;

        this.log = log;

        this.dag = new DAG();

        this.goals = goals;
    }

    public PlexusContainer getContainer()
    {
        return container;
    }

    public PluginManager getPluginManager()
    {
        return pluginManager;
    }

    public MavenProject getProject()
    {
        return project;
    }

    public void setProject( MavenProject project )
    {
        this.project = project;
    }

    public ArtifactRepository getLocalRepository()
    {
        return localRepository;
    }
    
    public void setRemoteRepositories(List remoteArtifactRepos)
    {
        this.remoteArtifactRepos = remoteArtifactRepos;
    }

    public List getRemoteRepositories()
    {
        return remoteArtifactRepos;
    }

    public List getGoals()
    {
        return goals;
    }

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    public Object lookup( String role ) throws ComponentLookupException
    {
        return container.lookup( role );
    }

    public Object lookup( String role, String roleHint ) throws ComponentLookupException
    {
        return container.lookup( role, roleHint );
    }

    public EventDispatcher getEventDispatcher()
    {
        return eventDispatcher;
    }

    public Log getLog()
    {
        return log;
    }

    public UserModel getUserModel()
    {
        return userModel;
    }

    public void addImpliedExecution( String goal, String implied ) throws CycleDetectedException
    {
        dag.addEdge( goal, implied );
    }

    public void addSingleExecution( String goal )
    {
        dag.addVertex( goal );
    }

    public List getExecutionChain( String goal )
    {
        Vertex vertex = dag.getVertex( goal );

        List sorted = TopologicalSorter.sort( vertex );

        int goalIndex = sorted.indexOf( goal );

        List chainToHere = sorted.subList( 0, goalIndex + 1 );

        return chainToHere;
    }

}