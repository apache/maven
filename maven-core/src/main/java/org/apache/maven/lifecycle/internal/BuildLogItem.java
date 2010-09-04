package org.apache.maven.lifecycle.internal;

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

import org.apache.maven.project.MavenProject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @since 3.0
 * @author Kristian Rosenvold
 *         NOTE: This class is not part of any public api and can be changed or deleted without prior notice.
 */
public class BuildLogItem
{
    private final ExecutionPlanItem executionPlanItem;

    private final MavenProject project;

    private final long startTime;

    private long endTime;

    private final List<DependencyLogEntry> dependencies =
        Collections.synchronizedList( new ArrayList<DependencyLogEntry>() );

    public BuildLogItem( MavenProject project, ExecutionPlanItem executionPlanItem )
    {
        this.executionPlanItem = executionPlanItem;
        this.project = project;
        startTime = System.currentTimeMillis();

    }


    public MavenProject getProject()
    {
        return project;
    }

    public void setComplete()
    {
        endTime = System.currentTimeMillis();
    }

    public void addWait( MavenProject upstreamProject, ExecutionPlanItem inSchedule, long startWait )
    {
        long now = System.currentTimeMillis();
        dependencies.add( new DependencyLogEntry( upstreamProject, inSchedule, startWait, now, null ) );
    }

    public void addDependency( MavenProject upstreamProject, String message )
    {
        dependencies.add( new DependencyLogEntry( upstreamProject, message ) );
    }

    public String toString( long rootStart )
    {
        StringBuilder result = new StringBuilder();
        result.append( String.format( "%1d  %2d ", startTime - rootStart, endTime - rootStart ) );
        result.append( project.getName() );
        result.append( " " );
        result.append( getMojoExecutionDescription( executionPlanItem ) );
        if ( dependencies.size() > 0 )
        {
            result.append( "\n" );
            for ( DependencyLogEntry waitLogEntry : dependencies )
            {
                result.append( "    " );
                result.append( waitLogEntry.toString() );
                result.append( "\n" );
            }
        }
        return result.toString();
    }


    public Object toGraph( long rootStart )
    {
        StringBuilder result = new StringBuilder();
        if ( dependencies.size() > 0 )
        {
            for ( DependencyLogEntry waitLogEntry : dependencies )
            {
                result.append( "        " );
                result.append( nodeKey( project, executionPlanItem ) );
                result.append( " ->   " );
                result.append( waitLogEntry.toNodeKey() );
                result.append( waitLogEntry.toNodeDescription( rootStart ) );
                result.append( "\n" );
            }
        }
        else
        {
            result.append( "        " );
            result.append( nodeKey( project, executionPlanItem ) );
            result.append( "\n" );
        }
        return result.toString();
    }

    private static String nodeKey( MavenProject mavenProject, ExecutionPlanItem executionPlanItem )
    {
        String key = mavenProject.getArtifactId();
        if ( executionPlanItem != null )
        {
            key += "_" + getMojoExecutionDescription( executionPlanItem );
        }
        return key.replace( ".", "_" ).replace( ":", "_" );
    }

    private static String getMojoExecutionDescription( ExecutionPlanItem executionPlanItem )
    {
        if ( executionPlanItem.getMojoExecution() != null )
        {
            return executionPlanItem.getMojoExecution().getArtifactId() + getLifeCyclePhase( executionPlanItem );
        }
        else
        {
            return "";
        }
    }

    private static String getLifeCyclePhase( ExecutionPlanItem executionPlanItem )
    {
        return executionPlanItem.getLifecyclePhase() != null ? "[" + executionPlanItem.getLifecyclePhase() + "]" : "";
    }


    class DependencyLogEntry
    {
        private final ExecutionPlanItem executionPlanItem;

        private final MavenProject upstreamProject;

        private final Long start;

        private final Long stop;

        private final String message;

        DependencyLogEntry( MavenProject upstreamProject, ExecutionPlanItem executionPlanItem, Long start, Long stop,
                            String message )
        {
            this.upstreamProject = upstreamProject;
            this.executionPlanItem = executionPlanItem;
            this.start = start;
            this.stop = stop;
            this.message = message;
        }

        DependencyLogEntry( MavenProject upstreamProject, String message )
        {
            this( upstreamProject, null, null, null, message );
        }

        public String toString()
        {
            return upstreamProject.getName() + ":" + getExecutionPlanItem() + getElapsed() + getMessage();
        }

        public String toNodeKey()
        {
            return nodeKey( upstreamProject, executionPlanItem );
        }

        public String toNodeDescription( long rootStart )
        {
            return "";
        }


        private String getMessage()
        {
            return message != null ? message : "";
        }

        private String getExecutionPlanItem()
        {
            if ( executionPlanItem != null )
            {
                return getMojoExecutionDescription( executionPlanItem );
            }
            else
            {
                return "";
            }
        }

        private String getElapsed()
        {
            if ( start != null && stop != null )
            {
                long elapsed = stop - start;
                return elapsed > 0 ? ", wait=" + elapsed : "";
            }
            return "";
        }
    }

}
