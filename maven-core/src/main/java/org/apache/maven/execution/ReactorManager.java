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
import org.apache.maven.project.DuplicateProjectException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectSorter;
import org.codehaus.plexus.util.dag.CycleDetectedException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
}
