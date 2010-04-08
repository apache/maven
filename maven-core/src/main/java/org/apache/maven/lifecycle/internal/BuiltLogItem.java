/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.maven.lifecycle.internal;

import org.apache.maven.project.MavenProject;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Kristian Rosenvold
 *         NOTE: This class is not part of any public api and can be changed or deleted without prior notice.
 */
public class BuiltLogItem
{
    private final ExecutionPlanItem executionPlanItem;

    private final MavenProject project;

    private final long startTime;

    private long endTime;

    private final List<WaitLogEntry> waits = new ArrayList<WaitLogEntry>();

    public BuiltLogItem( MavenProject project, ExecutionPlanItem executionPlanItem )
    {
        this.executionPlanItem = executionPlanItem;
        this.project = project;
        startTime = System.currentTimeMillis();

    }

    public void setComplete()
    {
        endTime = System.currentTimeMillis();
    }

    public void addWait( MavenProject upstreamProject, ExecutionPlanItem inSchedule, long startWait )
    {
        long now = System.currentTimeMillis();
        if ( ( now - startWait ) > 1 )
        {
            waits.add( new WaitLogEntry( upstreamProject, inSchedule, startWait, now ) );
        }
    }

    public String toString( long rootStart )
    {
        StringBuilder result = new StringBuilder();
        result.append( String.format( "%1d  %2d ", startTime - rootStart, endTime - rootStart ) );
        result.append( project.getName() );
        result.append( " " );
        result.append( executionPlanItem.getMojoExecution().getArtifactId() );
        for ( WaitLogEntry waitLogEntry : waits )
        {
            result.append( waitLogEntry.toString() );
        }
        return result.toString();
    }

    class WaitLogEntry
    {
        private final ExecutionPlanItem executionPlanItem;

        private final MavenProject upstreamProject;

        private final long start;

        private final long stop;

        WaitLogEntry( MavenProject upstreamProject, ExecutionPlanItem executionPlanItem, long start, long stop )
        {
            this.upstreamProject = upstreamProject;
            this.executionPlanItem = executionPlanItem;
            this.start = start;
            this.stop = stop;
        }

        public String toString()
        {
            return upstreamProject.getName() + " " + executionPlanItem.getMojoExecution().getArtifactId() + ", wait=" +
                ( stop - start );
        }
    }
}
