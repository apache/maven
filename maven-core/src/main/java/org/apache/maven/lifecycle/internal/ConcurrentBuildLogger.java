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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles all concurrency-related logging.
 * <p/>
 * The logging/diagnostic needs of a concurrent build are different from a linear build. This
 * delta required to analyze a concurrent build is located here.
 * <p/>
 * NOTE: This class is not part of any public api and can be changed or deleted without prior notice.
 * 
 * @since 3.0
 * @author Kristian Rosenvold
 */
public class ConcurrentBuildLogger
{
    private final long startTime;

    private final Map<MavenProject, Thread> threadMap = new ConcurrentHashMap<MavenProject, Thread>();

    public ConcurrentBuildLogger()
    {
        startTime = System.currentTimeMillis();
    }


    List<BuildLogItem> items = Collections.synchronizedList( new ArrayList<BuildLogItem>() );

    public BuildLogItem createBuildLogItem( MavenProject project, ExecutionPlanItem current )
    {
        threadMap.put( project, Thread.currentThread() );
        BuildLogItem result = new BuildLogItem( project, current );
        items.add( result );
        return result;
    }

    public String toString()
    {
        StringBuilder result = new StringBuilder();
        for ( Map.Entry<MavenProject, Thread> mavenProjectThreadEntry : threadMap.entrySet() )
        {
            result.append( mavenProjectThreadEntry.getKey().getName() );
            result.append( " ran on " );
            result.append( mavenProjectThreadEntry.getValue().getName() );
            result.append( "\n" );
        }

        for ( BuildLogItem builtLogItem : items )
        {
            result.append( builtLogItem.toString( startTime ) );
            result.append( "\n" );
        }
        return result.toString();
    }

    public String toGraph()
    {
        StringBuilder result = new StringBuilder();

        Map<MavenProject, Collection<BuildLogItem>> multiMap = new HashMap<MavenProject, Collection<BuildLogItem>>();
        for ( BuildLogItem builtLogItem : items )
        {
            MavenProject project = builtLogItem.getProject();
            Collection<BuildLogItem> bag = multiMap.get( project );
            if ( bag == null )
            {
                bag = new ArrayList<BuildLogItem>();
                multiMap.put( project, bag );
            }
            bag.add( builtLogItem );
        }

        result.append( "digraph build" );
        result.append( " {\n " );

        for ( MavenProject mavenProject : multiMap.keySet() )
        {
            final Collection<BuildLogItem> builtLogItems = multiMap.get( mavenProject );
            result.append( "   subgraph " );
            result.append( mavenProject.getArtifactId() );
            result.append( "   {\n" );

            for ( BuildLogItem builtLogItem : builtLogItems )
            {
                result.append( builtLogItem.toGraph( startTime ) );
            }

            result.append( "\n   }\n" );
        }

        result.append( "\n}\n " );
        return result.toString();
    }

}
