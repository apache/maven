package org.apache.maven.model.building;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.codehaus.plexus.util.dag.CycleDetectedException;
import org.codehaus.plexus.util.dag.DAG;

/**
 * Builds up the transformer context.
 * After the buildplan is ready, the build()-method returns the immutable context useful during distribution.
 * It must be able to call {@link DefaultModelBuilder#readRawModel(ModelBuildingRequest, DefaultModelProblemCollector)}.
 *
 * @author Robert Scholte
 * @since 4.0.0
 */
class DefaultTransformerContextBuilder implements TransformerContextBuilder
{

    private final Map<String, Set<FileModelSource>> mappedSources = new ConcurrentHashMap<>( 64 );

    private final DAG dag = new DAG();

    DefaultTransformerContextBuilder( )
    {
    }

    boolean addEdge( Path from, Path p, DefaultModelProblemCollector problems )
    {
        try
        {
            synchronized ( dag )
            {
                dag.addEdge( from.toString(), p.toString() );
            }
            return true;
        }
        catch ( CycleDetectedException e )
        {
            problems.add( new DefaultModelProblem(
                    "Cycle detected between models at " + from + " and " + p,
                    ModelProblem.Severity.FATAL,
                    null, null, 0, 0, null,
                    e
            ) );
            return false;
        }
    }

    public FileModelSource getSource( String groupId, String artifactId )
    {
        Set<FileModelSource> sources = mappedSources.get( groupId + ":" + artifactId );
        if ( sources == null )
        {
            return null;
        }
        return sources.stream().reduce( ( a, b ) ->
        {
            throw new IllegalStateException( String.format( "No unique Source for %s:%s: %s and %s",
                    groupId, artifactId,
                    a.getLocation(), b.getLocation() ) );
        } ).orElse( null );
    }

    public void putSource( String groupId, String artifactId, FileModelSource source )
    {
        mappedSources.computeIfAbsent( groupId + ":" + artifactId, k -> new HashSet<>() )
                .add( source );
    }
}
