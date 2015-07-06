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

package org.apache.maven.lifecycle.internal.stub;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.internal.GoalTask;
import org.apache.maven.lifecycle.internal.LifecycleTask;
import org.apache.maven.lifecycle.internal.DefaultLifecycleTaskSegmentCalculator;
import org.apache.maven.lifecycle.internal.TaskSegment;
import org.apache.maven.plugin.InvalidPluginDescriptorException;
import org.apache.maven.plugin.MojoNotFoundException;
import org.apache.maven.plugin.PluginDescriptorParsingException;
import org.apache.maven.plugin.PluginNotFoundException;
import org.apache.maven.plugin.PluginResolutionException;
import org.apache.maven.plugin.prefix.NoPluginFoundForPrefixException;
import org.apache.maven.plugin.version.PluginVersionResolutionException;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Kristian Rosenvold
 */
public class LifecycleTaskSegmentCalculatorStub
    extends DefaultLifecycleTaskSegmentCalculator
{
    public static final String clean = "clean";

    public static final String aggr = "aggr";

    public static final String install = "install";


    public List<TaskSegment> calculateTaskSegments( MavenSession session, List<String> tasks )
        throws PluginNotFoundException, PluginResolutionException, PluginDescriptorParsingException,
        MojoNotFoundException, NoPluginFoundForPrefixException, InvalidPluginDescriptorException,
        PluginVersionResolutionException
    {
        List<TaskSegment> taskSegments = new ArrayList<>( tasks.size() );

        TaskSegment currentSegment = null;

        for ( String task : tasks )
        {
            if ( aggr.equals( task ) )
            {
                boolean aggregating = true;

                if ( currentSegment == null || currentSegment.isAggregating() != aggregating )
                {
                    currentSegment = new TaskSegment( aggregating );
                    taskSegments.add( currentSegment );
                }

                currentSegment.getTasks().add( new GoalTask( task ) );
            }
            else
            {
                // lifecycle phase
                if ( currentSegment == null || currentSegment.isAggregating() )
                {
                    currentSegment = new TaskSegment( false );
                    taskSegments.add( currentSegment );
                }
                currentSegment.getTasks().add( new LifecycleTask( task ) );
            }
        }

        return taskSegments;
    }

    public boolean requiresProject( MavenSession session )
    {
        return true;
    }
}
