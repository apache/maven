package org.apache.maven.project;

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

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Extension;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.ReportPlugin;
import org.codehaus.plexus.util.dag.CycleDetectedException;
import org.codehaus.plexus.util.dag.DAG;
import org.codehaus.plexus.util.dag.TopologicalSorter;
import org.codehaus.plexus.util.dag.Vertex;

/**
 * Sort projects by dependencies.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id$
 */
public class ProjectSorter
{
    private final DAG dag;
    
    private final Map projectMap;

    private final List<MavenProject> sortedProjects;

    private MavenProject topLevelProject;

    /**
     * Sort a list of projects.
     * <ul>
     * <li>collect all the vertices for the projects that we want to build.</li>
     * <li>iterate through the deps of each project and if that dep is within
     * the set of projects we want to build then add an edge, otherwise throw
     * the edge away because that dependency is not within the set of projects
     * we are trying to build. we assume a closed set.</li>
     * <li>do a topo sort on the graph that remains.</li>
     * </ul>
     * @throws DuplicateProjectException if any projects are duplicated by id
     * @throws MissingProjectException 
     */
    public ProjectSorter( List projects )
        throws CycleDetectedException, DuplicateProjectException, MissingProjectException
    {
        this( projects, null, null, false, false );
    }
    
    public ProjectSorter( List projects, List selectedProjectNames, String resumeFrom, boolean make, boolean makeDependents )
        throws CycleDetectedException, DuplicateProjectException, MissingProjectException
    {
        dag = new DAG();

        projectMap = new HashMap();

        for ( Iterator i = projects.iterator(); i.hasNext(); )
        {
            MavenProject project = (MavenProject) i.next();

            String id = ArtifactUtils.versionlessKey( project.getGroupId(), project.getArtifactId() );

            if ( dag.getVertex( id ) != null )
            {
                throw new DuplicateProjectException( "Project '" + id + "' is duplicated in the reactor" );
            }

            dag.addVertex( id );

            projectMap.put( id, project );
        }

        for ( Iterator i = projects.iterator(); i.hasNext(); )
        {
            MavenProject project = (MavenProject) i.next();

            String id = ArtifactUtils.versionlessKey( project.getGroupId(), project.getArtifactId() );

            for ( Iterator j = project.getDependencies().iterator(); j.hasNext(); )
            {
                Dependency dependency = (Dependency) j.next();

                String dependencyId = ArtifactUtils
                    .versionlessKey( dependency.getGroupId(), dependency.getArtifactId() );

                if ( dag.getVertex( dependencyId ) != null )
                {
                    project.addProjectReference( (MavenProject) projectMap.get( dependencyId ) );

                    dag.addEdge( id, dependencyId );
                }
            }

            MavenProject parent = project.getParent();
            if ( parent != null )
            {
                String parentId = ArtifactUtils.versionlessKey( parent.getGroupId(), parent.getArtifactId() );
                if ( dag.getVertex( parentId ) != null )
                {
                    // Parent is added as an edge, but must not cause a cycle - so we remove any other edges it has in conflict
                    if ( dag.hasEdge( parentId, id ) )
                    {
                        dag.removeEdge( parentId, id );
                    }
                    dag.addEdge( id, parentId );
                }
            }

            List buildPlugins = project.getBuildPlugins();
            if ( buildPlugins != null )
            {
                for ( Iterator j = buildPlugins.iterator(); j.hasNext(); )
                {
                    Plugin plugin = (Plugin) j.next();
                    String pluginId = ArtifactUtils.versionlessKey( plugin.getGroupId(), plugin.getArtifactId() );
                    if ( dag.getVertex( pluginId ) != null && !pluginId.equals( id ) )
                    {
                        addEdgeWithParentCheck( projectMap, pluginId, project, id );
                    }

                    if ( !pluginId.equals( id ) ) {
                        for ( Iterator k = plugin.getDependencies().iterator(); k.hasNext(); )
                        {
                          Dependency dependency = (Dependency) k.next();

                          String dependencyId = ArtifactUtils
                              .versionlessKey( dependency.getGroupId(), dependency.getArtifactId() );

                          if ( dag.getVertex( dependencyId ) != null )
                          {
                              // If the plugin in which this dependency is listed is actually used here,
                              // it will probably be stuck using an older version of this project that
                              // already exists in the local repository. If not yet installed, it is
                              // likely that we'll get an ArtifactNotFoundException when this plugin
                              // is executed to build this project.
                              //
                              // If the plugin is NOT actually used here, it may actually belong in the
                              // pluginManagement section.
                              if ( !id.equals( dependencyId ) )
                              {
                                  project.addProjectReference( (MavenProject) projectMap.get( dependencyId ) );

                                  addEdgeWithParentCheck( projectMap, dependencyId, project, id );
                                  
                                  // TODO: Shouldn't we add an edge between the plugin and its dependency?
                                  // Note that doing this may result in cycles...run 
                                  // ProjectSorterTest.testPluginDependenciesInfluenceSorting_DeclarationInParent() 
                                  // for more information, if you change this:
                                  
                                  // dag.addEdge( pluginId, dependencyId );
                              }
                          }
                       }
                    }
                }
            }

            List reportPlugins = project.getReportPlugins();
            if ( reportPlugins != null )
            {
                for ( Iterator j = reportPlugins.iterator(); j.hasNext(); )
                {
                    ReportPlugin plugin = (ReportPlugin) j.next();
                    String pluginId = ArtifactUtils.versionlessKey( plugin.getGroupId(), plugin.getArtifactId() );
                    if ( dag.getVertex( pluginId ) != null && !pluginId.equals( id ) )
                    {
                        addEdgeWithParentCheck( projectMap, pluginId, project, id );
                    }
                }
            }

            for ( Iterator j = project.getBuildExtensions().iterator(); j.hasNext(); )
            {
                Extension extension = (Extension) j.next();
                String extensionId = ArtifactUtils.versionlessKey( extension.getGroupId(), extension.getArtifactId() );
                if ( dag.getVertex( extensionId ) != null )
                {
                    addEdgeWithParentCheck( projectMap, extensionId, project, id );
                }
            }
        }

        List sortedProjects = new ArrayList();

        for ( Iterator i = TopologicalSorter.sort( dag ).iterator(); i.hasNext(); )
        {
            String id = (String) i.next();

            sortedProjects.add( projectMap.get( id ) );
        }
        
        // TODO: !![jc; 28-jul-2005] check this; if we're using '-r' and there are aggregator tasks, this will result in weirdness.
        for ( Iterator i = sortedProjects.iterator(); i.hasNext() && topLevelProject == null; )
        {
            MavenProject project = (MavenProject) i.next();
            if ( project.isExecutionRoot() )
            {
                topLevelProject = project;
            }
        }
        
        sortedProjects = applyMakeFilter( sortedProjects, dag, projectMap, topLevelProject, selectedProjectNames, make, makeDependents );
        
        resumeFrom( resumeFrom, sortedProjects, projectMap, topLevelProject );

        this.sortedProjects = Collections.unmodifiableList( sortedProjects );
    }

    // make selected projects and possibly projects they depend on, or projects that depend on them 
    private static List applyMakeFilter( List sortedProjects, DAG dag, Map projectMap, MavenProject topLevelProject, List selectedProjectNames, boolean make, boolean makeDependents ) throws MissingProjectException
    {
        if ( selectedProjectNames == null ) return sortedProjects;
        
        MavenProject[] selectedProjects = new MavenProject[selectedProjectNames.size()];
        for ( int i = 0; i < selectedProjects.length; i++ )
        {
            selectedProjects[i] = findProject( (String) selectedProjectNames.get( i ), projectMap, topLevelProject );
        }
        Set projectsToMake = new HashSet( Arrays.asList( selectedProjects ) );
        for ( int i = 0; i < selectedProjects.length; i++ )
        {
            MavenProject project = selectedProjects[i];
            String id = ArtifactUtils.versionlessKey( project.getGroupId(), project.getArtifactId() );
            Vertex v = dag.getVertex( id );
            if ( make )
            {
                gatherDescendents ( v, projectMap, projectsToMake, new HashSet() );
            }
            if ( makeDependents )
            {
                gatherAncestors ( v, projectMap, projectsToMake, new HashSet() );
            }
        }
        for ( Iterator i = sortedProjects.iterator(); i.hasNext(); )
        {
            MavenProject project = (MavenProject) i.next();
            if ( !projectsToMake.contains( project ) )
            {
                i.remove();
            }
        }
        return sortedProjects;
    }
    
    private static void resumeFrom( String resumeFrom, List sortedProjects, Map projectMap, MavenProject topLevelProject ) throws MissingProjectException
    {
        if ( resumeFrom == null ) return;
        MavenProject resumeFromProject = findProject( resumeFrom, projectMap, topLevelProject );
        for ( Iterator i = sortedProjects.iterator(); i.hasNext(); )
        {
            MavenProject project = (MavenProject) i.next();
            if ( resumeFromProject.equals( project ) ) break;
            i.remove();
        }
        if ( sortedProjects.isEmpty() )
        {
            throw new MissingProjectException( "Couldn't resume, project was not scheduled to run: " + resumeFrom );
        }
    }
    
    private static MavenProject findProject( String projectName, Map projectMap, MavenProject topLevelProject ) throws MissingProjectException
    {
        MavenProject project = (MavenProject) projectMap.get( projectName );
        if ( project != null ) return project;
        // in that case, it must be a file path
        File baseDir;
        if ( topLevelProject == null ) {
            baseDir = new File( System.getProperty( "user.dir" ) );
        } else {
            baseDir = topLevelProject.getBasedir();
            // or should this be .getFile().getParentFile() ?
        }
        
        File projectDir = new File( baseDir, projectName );
        if ( !projectDir.exists() ) {
            throw new MissingProjectException( "Couldn't find specified project dir: " + projectDir.getAbsolutePath() );
        }
        if ( !projectDir.isDirectory() ) {
            throw new MissingProjectException( "Couldn't find specified project dir (not a directory): " + projectDir.getAbsolutePath() );
        }
        
        for ( Iterator i = projectMap.values().iterator(); i.hasNext(); )
        {
            project = (MavenProject) i.next();
            if ( projectDir.equals( project.getFile().getParentFile() ) ) return project;
        }
        
        throw new MissingProjectException( "Couldn't find specified project in module list: " + projectDir.getAbsolutePath() );
    }
    
    private static void gatherDescendents ( Vertex v, Map projectMap, Set out, Set visited )
    {
        if ( visited.contains( v ) ) return;
        visited.add( v );
        out.add( projectMap.get( v.getLabel() ) );
        for ( Iterator i = v.getChildren().iterator(); i.hasNext(); )
        {
            Vertex child = (Vertex) i.next();
            gatherDescendents( child, projectMap, out, visited );
        }
    }
    
    private static void gatherAncestors ( Vertex v, Map projectMap, Set out, Set visited )
    {
        if ( visited.contains( v ) ) return;
        visited.add( v );
        out.add( projectMap.get( v.getLabel() ) );
        for ( Iterator i = v.getParents().iterator(); i.hasNext(); )
        {
            Vertex parent = (Vertex) i.next();
            gatherAncestors( parent, projectMap, out, visited );
        }
    }
    
    private void addEdgeWithParentCheck( Map projectMap, String projectRefId, MavenProject project, String id )
        throws CycleDetectedException
    {
        MavenProject extProject = (MavenProject) projectMap.get( projectRefId );
        
        if ( extProject == null )
        {
            return;
        }

        project.addProjectReference( extProject );

        MavenProject extParent = extProject.getParent();
        if ( extParent != null )
        {
            String parentId = ArtifactUtils.versionlessKey( extParent.getGroupId(), extParent.getArtifactId() );
            // Don't add edge from parent to extension if a reverse edge already exists
            if ( !dag.hasEdge( projectRefId, id ) || !parentId.equals( id ) )
            {
                dag.addEdge( id, projectRefId );
            }
        }
    }

    public MavenProject getTopLevelProject()
    {
        return topLevelProject;
    }

    public List<MavenProject> getSortedProjects()
    {
        return sortedProjects;
    }

    public boolean hasMultipleProjects()
    {
        return sortedProjects.size() > 1;
    }

    public List getDependents( String id )
    {
        return dag.getParentLabels( id );
    }
    
    public DAG getDAG()
    {
        return dag;
    }
    
    public Map getProjectMap()
    {
        return projectMap;
    }
}
