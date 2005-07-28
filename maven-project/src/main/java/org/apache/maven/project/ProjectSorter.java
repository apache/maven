package org.apache.maven.project;

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

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Extension;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.ReportPlugin;
import org.codehaus.plexus.util.dag.CycleDetectedException;
import org.codehaus.plexus.util.dag.DAG;
import org.codehaus.plexus.util.dag.TopologicalSorter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Sort projects by dependencies.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id$
 */
public class ProjectSorter
{
    private ProjectSorter()
    {
        // no touchy...
    }

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
     */
    public static List getSortedProjects( List projects )
        throws CycleDetectedException
    {
        DAG dag = new DAG();

        Map projectMap = new HashMap();

        for ( Iterator i = projects.iterator(); i.hasNext(); )
        {
            MavenProject project = (MavenProject) i.next();

            String id = getId( project.getGroupId(), project.getArtifactId() );

            dag.addVertex( id );

            projectMap.put( id, project );
        }

        for ( Iterator i = projects.iterator(); i.hasNext(); )
        {
            MavenProject project = (MavenProject) i.next();

            String id = getId( project.getGroupId(), project.getArtifactId() );

            for ( Iterator j = project.getDependencies().iterator(); j.hasNext(); )
            {
                Dependency dependency = (Dependency) j.next();

                String dependencyId = getId( dependency.getGroupId(), dependency.getArtifactId() );

                if ( dag.getVertex( dependencyId ) != null )
                {
                    project.addProjectReference( (MavenProject) projectMap.get( dependencyId ) );

                    dag.addEdge( id, dependencyId );
                }
            }

            MavenProject parent = project.getParent();
            if ( parent != null )
            {
                String parentId = getId( parent.getGroupId(), parent.getArtifactId() );
                if ( dag.getVertex( parentId ) != null )
                {
                    dag.addEdge( id, parentId );
                }
            }

            List buildPlugins = project.getBuildPlugins();
            if ( buildPlugins != null )
            {
                for ( Iterator j = buildPlugins.iterator(); j.hasNext(); )
                {
                    Plugin plugin = (Plugin) j.next();
                    String pluginId = getId( plugin.getGroupId(), plugin.getArtifactId() );
                    if ( dag.getVertex( pluginId ) != null )
                    {
                        dag.addEdge( id, pluginId );
                    }
                }
            }

            List reportPlugins = project.getReportPlugins();
            if ( reportPlugins != null )
            {
                for ( Iterator j = reportPlugins.iterator(); j.hasNext(); )
                {
                    ReportPlugin plugin = (ReportPlugin) j.next();
                    String pluginId = getId( plugin.getGroupId(), plugin.getArtifactId() );
                    if ( dag.getVertex( pluginId ) != null )
                    {
                        dag.addEdge( id, pluginId );
                    }
                }
            }

            for ( Iterator j = project.getBuildExtensions().iterator(); j.hasNext(); )
            {
                Extension extension = (Extension) j.next();
                String extensionId = getId( extension.getGroupId(), extension.getArtifactId() );
                if ( dag.getVertex( extensionId ) != null )
                {
                    dag.addEdge( id, extensionId );
                }
            }
        }

        List sortedProjects = new ArrayList();

        for ( Iterator i = TopologicalSorter.sort( dag ).iterator(); i.hasNext(); )
        {
            String id = (String) i.next();

            sortedProjects.add( projectMap.get( id ) );
        }

        return sortedProjects;
    }

    private static String getId( String groupId, String artifactId )
    {
        return groupId + ":" + artifactId;
    }
}
