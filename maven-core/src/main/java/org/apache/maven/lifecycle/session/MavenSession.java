package org.apache.maven.lifecycle.session;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.PostGoal;
import org.apache.maven.model.PreGoal;
import org.apache.maven.plugin.PluginManager;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositoryUtils;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.util.dag.CycleDetectedException;
import org.codehaus.plexus.util.dag.DAG;
import org.codehaus.plexus.util.dag.TopologicalSorter;
import org.codehaus.plexus.util.dag.Vertex;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/*
 * Copyright 2001-2004 The Apache Software Foundation.
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

    public MavenSession( PlexusContainer container, PluginManager pluginManager, MavenProject project,
        ArtifactRepository localRepository, List goals )
    {
        this.container = container;

        this.pluginManager = pluginManager;

        this.project = project;

        this.localRepository = localRepository;

        this.dag = new DAG();

        this.goals = goals;

        this.preGoalMappings = new TreeMap();

        this.postGoalMappings = new TreeMap();

        initGoalDecoratorMappings();
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

    public ArtifactRepository getLocalRepository()
    {
        return localRepository;
    }

    public Set getRemoteRepositories()
    {
        if ( remoteRepositories == null )
        {
            remoteRepositories = RepositoryUtils.mavenToWagon( project.getRepositories() );
        }

        return remoteRepositories;
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

    public void release( Object component )
    {
        if ( component != null )
        {
            try
            {
                container.release( component );
            }
            catch ( Exception e )
            {
                //@todo what to do here?
            }
        }
    }

    public List getPreGoals( String goal )
    {
        List result = (List) preGoalMappings.get( goal );
        return result;
    }

    public List getPostGoals( String goal )
    {
        List result = (List) postGoalMappings.get( goal );
        return result;
    }

    private void initGoalDecoratorMappings()
    {
        List allPreGoals = project.getPreGoals();
        for ( Iterator it = allPreGoals.iterator(); it.hasNext(); )
        {
            PreGoal preGoal = (PreGoal) it.next();

            List preGoalList = (List) preGoalMappings.get( preGoal.getName() );
            if ( preGoalList == null )
            {
                preGoalList = new LinkedList();
                preGoalMappings.put( preGoal.getName(), preGoalList );
            }

            preGoalList.add( preGoal.getAttain() );
        }

        List allPostGoals = project.getPostGoals();
        for ( Iterator it = allPostGoals.iterator(); it.hasNext(); )
        {
            PostGoal postGoal = (PostGoal) it.next();

            List postGoalList = (List) postGoalMappings.get( postGoal.getName() );
            if ( postGoalList == null )
            {
                postGoalList = new LinkedList();
                postGoalMappings.put( postGoal.getName(), postGoalList );
            }

            postGoalList.add( postGoal.getAttain() );
        }
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