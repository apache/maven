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


import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.execution.DuplicateProjectException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.ReportPlugin;
import org.apache.maven.model.Extension;
import org.codehaus.plexus.util.dag.CycleDetectedException;
import org.codehaus.plexus.util.dag.DAG;
import org.codehaus.plexus.util.dag.TopologicalSorter;

import java.util.*;

public class ReactorManager
{
    public static final String FAIL_FAST = "fail-fast";

    public static final String FAIL_AT_END = "fail-at-end";

    public static final String FAIL_NEVER = "fail-never";

    private List blackList = new ArrayList();

    private Map buildFailuresByProject = new HashMap();

    private Map pluginContextsByProjectAndPluginKey = new HashMap();

    private String failureBehavior = FAIL_FAST;

    private final ProjectSorter sorter;

    private Map buildSuccessesByProject = new HashMap();

    public ReactorManager( List projects, String failureBehavior )
        throws CycleDetectedException, DuplicateProjectException
    {
        sorter = new ProjectSorter( projects );

        if ( failureBehavior == null )
        {
            this.failureBehavior = FAIL_FAST;
        }
        else
        {
            this.failureBehavior = failureBehavior;
        }
    }

    public Map getPluginContext( PluginDescriptor plugin, MavenProject project )
    {
        Map pluginContextsByKey = (Map) pluginContextsByProjectAndPluginKey.get( project.getId() );

        if ( pluginContextsByKey == null )
        {
            pluginContextsByKey = new HashMap();

            pluginContextsByProjectAndPluginKey.put( project.getId(), pluginContextsByKey );
        }

        Map pluginContext = (Map) pluginContextsByKey.get( plugin.getPluginLookupKey() );

        if ( pluginContext == null )
        {
            pluginContext = new HashMap();
            pluginContextsByKey.put( plugin.getPluginLookupKey(), pluginContext );
        }

        return pluginContext;
    }

    public String getFailureBehavior()
    {
        return failureBehavior;
    }

    public void blackList( MavenProject project )
    {
        blackList( getProjectKey( project ) );
    }

    private void blackList( String id )
    {
        if ( !blackList.contains( id ) )
        {
            blackList.add( id );

            List dependents = sorter.getDependents( id );

            if ( ( dependents != null ) && !dependents.isEmpty() )
            {
                for ( Iterator it = dependents.iterator(); it.hasNext(); )
                {
                    String dependentId = (String) it.next();

                    if ( !buildSuccessesByProject.containsKey( dependentId ) &&
                        !buildFailuresByProject.containsKey( dependentId ) )
                    {
                        blackList( dependentId );
                    }
                }
            }
        }
    }

    public boolean isBlackListed( MavenProject project )
    {
        return blackList.contains( getProjectKey( project ) );
    }

    private static String getProjectKey( MavenProject project )
    {
        return ArtifactUtils.versionlessKey( project.getGroupId(), project.getArtifactId() );
    }

    public void registerBuildFailure( MavenProject project, Exception error, String task, long time )
    {
        buildFailuresByProject.put( getProjectKey( project ), new BuildFailure( error, task, time ) );
    }

    public boolean hasBuildFailures()
    {
        return !buildFailuresByProject.isEmpty();
    }

    public boolean hasBuildFailure( MavenProject project )
    {
        return buildFailuresByProject.containsKey( getProjectKey( project ) );
    }

    public boolean hasMultipleProjects()
    {
        return sorter.hasMultipleProjects();
    }

    public List getSortedProjects()
    {
        return sorter.getSortedProjects();
    }

    public MavenProject getTopLevelProject()
    {
        return sorter.getTopLevelProject();
    }

    public boolean hasBuildSuccess( MavenProject project )
    {
        return buildSuccessesByProject.containsKey( getProjectKey( project ) );
    }

    public void registerBuildSuccess( MavenProject project, long time )
    {
        buildSuccessesByProject.put( getProjectKey( project ), new BuildSuccess( project, time ) );
    }

    public BuildFailure getBuildFailure( MavenProject project )
    {
        return (BuildFailure) buildFailuresByProject.get( getProjectKey( project ) );
    }

    public BuildSuccess getBuildSuccess( MavenProject project )
    {
        return (BuildSuccess) buildSuccessesByProject.get( getProjectKey( project ) );
    }

    public boolean executedMultipleProjects()
    {
        return buildFailuresByProject.size() + buildSuccessesByProject.size() > 1;
    }

    protected static class ProjectSorter
    {
        private final DAG dag;

        private final List sortedProjects;

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
         */
        // MAVENAPI FIXME: the DAG used is NOT only used to represent the dependency relation,
        // but also for <parent>, <build><plugin>, <reports>. We need multiple DAG's
        // since a DAG can only handle 1 type of relationship properly.
        // Usecase:  This is detected as a cycle:
        // org.apache.maven:maven-plugin-api                -(PARENT)->
        // org.apache.maven:maven                           -(inherited REPORTING)->
        // org.apache.maven.plugins:maven-checkstyle-plugin -(DEPENDENCY)->
        // org.apache.maven:maven-plugin-api
        // In this case, both the verify and the report goals are called
        // in a different lifecycle. Though the compiler-plugin has a valid usecase, although
        // that seems to work fine. We need to take versions and lifecycle into account.
        public ProjectSorter( List projects )
            throws CycleDetectedException, DuplicateProjectException
        {
            dag = new DAG();

            Map projectMap = new HashMap();

            for ( Iterator i = projects.iterator(); i.hasNext(); )
            {
                MavenProject project = (MavenProject) i.next();

                String id = ArtifactUtils.versionlessKey( project.getGroupId(), project.getArtifactId() );

                if ( dag.getVertex( id ) != null )
                {
                    MavenProject conflictingProject = (MavenProject) projectMap.get( id );

                    throw new DuplicateProjectException( id,
                                                         conflictingProject.getFile(),
                                                         project.getFile(),
                                                         "Project '"
                                                         + id
                                                         + "' is duplicated in the reactor" );
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
                        if ( ( dag.getVertex( pluginId ) != null ) && !pluginId.equals( id ) )
                        {
                            addEdgeWithParentCheck( projectMap, pluginId, project, id );
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
                        if ( ( dag.getVertex( pluginId ) != null ) && !pluginId.equals( id ) )
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

            this.sortedProjects = Collections.unmodifiableList( sortedProjects );
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

        // TODO: !![jc; 28-jul-2005] check this; if we're using '-r' and there are aggregator tasks, this will result in weirdness.
        public MavenProject getTopLevelProject()
        {
            if ( topLevelProject == null )
            {
                for ( Iterator i = sortedProjects.iterator(); i.hasNext() && ( topLevelProject == null ); )
                {
                    MavenProject project = (MavenProject) i.next();
                    if ( project.isExecutionRoot() )
                    {
                        topLevelProject = project;
                    }
                }
            }

            return topLevelProject;
        }

        public List getSortedProjects()
        {
            return sortedProjects;
        }

        public boolean hasMultipleProjects()
        {
            return sortedProjects.size() > 1;
        }

        private List getDependents( String id )
        {
            return dag.getParentLabels( id );
        }
    }
}
