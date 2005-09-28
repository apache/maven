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

import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
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
    
    public ReactorManager( List projects )
        throws CycleDetectedException
    {
        this.sorter = new ProjectSorter( projects );
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

    public void setFailureBehavior( String failureBehavior )
    {
        if ( FAIL_FAST.equals( failureBehavior ) || FAIL_AT_END.equals( failureBehavior ) ||
            FAIL_NEVER.equals( failureBehavior ) )
        {
            this.failureBehavior = failureBehavior;
        }
        else
        {
            throw new IllegalArgumentException( "Invalid failure behavior (must be one of: \'" + FAIL_FAST + "\', \'" +
                FAIL_AT_END + "\', \'" + FAIL_NEVER + "\')." );
        }
    }

    public String getFailureBehavior()
    {
        return failureBehavior;
    }

    public void blackList( MavenProject project )
    {
        blackList( ArtifactUtils.versionlessKey( project.getGroupId(), project.getArtifactId() ) );
    }

    private void blackList( String id )
    {
        if ( !blackList.contains( id ) )
        {
            blackList.add( id );

            List dependents = sorter.getDependents( id );

            if ( dependents != null && !dependents.isEmpty() )
            {
                for ( Iterator it = dependents.iterator(); it.hasNext(); )
                {
                    String dependentId = (String) it.next();

                    blackList( dependentId );
                }
            }
        }
    }

    public boolean isBlackListed( MavenProject project )
    {
        return blackList.contains( ArtifactUtils.versionlessKey( project.getGroupId(), project.getArtifactId() ) );
    }

    public void registerBuildFailure( MavenProject project, Exception error, String task )
    {
        buildFailuresByProject.put( project.getId(), new BuildFailure( error, task ) );
    }

    public boolean hasBuildFailures()
    {
        return !buildFailuresByProject.isEmpty();
    }

    public boolean hasBuildFailure( MavenProject project )
    {
        return buildFailuresByProject.containsKey( project.getId() );
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

    private static class BuildFailure
    {
        private Exception cause;

        private String task;

        BuildFailure( Exception cause, String task )
        {
            this.cause = cause;
            this.task = task;
        }

        String getTask()
        {
            return task;
        }

        Exception getCause()
        {
            return cause;
        }
    }
}
