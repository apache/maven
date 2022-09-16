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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.maven.model.Model;
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
    private final DefaultTransformerContext context = new DefaultTransformerContext();

    private final Map<String, Set<FileModelSource>> mappedSources = new ConcurrentHashMap<>( 64 );

    private final DAG dag = new DAG();
    private final DefaultModelBuilder defaultModelBuilder;

    DefaultTransformerContextBuilder( final DefaultModelBuilder defaultModelBuilder )
    {
        this.defaultModelBuilder = defaultModelBuilder;
    }

    /**
     * If an interface could be extracted, DefaultModelProblemCollector should be ModelProblemCollectorExt
     */
    @Override
    public TransformerContext initialize( ModelBuildingRequest request, ModelProblemCollector collector )
    {
        // We must assume the TransformerContext was created using this.newTransformerContextBuilder()
        DefaultModelProblemCollector problems = (DefaultModelProblemCollector) collector;
        return new TransformerContext()
        {
            @Override
            public String getUserProperty( String key )
            {
                return context.userProperties.computeIfAbsent( key,
                        k -> request.getUserProperties().getProperty( key ) );
            }

            @Override
            public Model getRawModel( Path from, String gId, String aId )
            {
                Model model = findRawModel( from, gId, aId );
                if ( model != null )
                {
                    context.modelByGA.put( gId + ":" + aId, model );
                    context.modelByPath.put( model.getPomFile().toPath(), model );
                }
                return model;
            }

            @Override
            public Model getRawModel( Path from, Path path )
            {
                Model model = findRawModel( from, path );
                if ( model != null )
                {
                    String groupId = defaultModelBuilder.getGroupId( model );
                    context.modelByGA.put( groupId + ":" + model.getArtifactId(), model );
                    context.modelByPath.put( path, model );
                }
                return model;
            }

            private Model findRawModel( Path from, String groupId, String artifactId )
            {
                FileModelSource source = getSource( groupId, artifactId );
                if ( source != null )
                {
                    if ( !addEdge( from, source.getFile().toPath(), problems ) )
                    {
                        return null;
                    }
                    try
                    {
                        ModelBuildingRequest gaBuildingRequest = new DefaultModelBuildingRequest( request )
                                .setModelSource( source );
                        return defaultModelBuilder.readRawModel( gaBuildingRequest, problems );
                    }
                    catch ( ModelBuildingException e )
                    {
                        // gathered with problem collector
                    }
                }
                return null;
            }

            private Model findRawModel( Path from, Path p )
            {
                if ( !Files.isRegularFile( p ) )
                {
                    throw new IllegalArgumentException( "Not a regular file: " + p );
                }

                if ( !addEdge( from, p, problems ) )
                {
                    return null;
                }

                DefaultModelBuildingRequest req = new DefaultModelBuildingRequest( request )
                        .setPomFile( p.toFile() )
                        .setModelSource( new FileModelSource( p.toFile() ) );

                try
                {
                    return defaultModelBuilder.readRawModel( req, problems );
                }
                catch ( ModelBuildingException e )
                {
                    // gathered with problem collector
                }
                return null;
            }
        };
    }

    private boolean addEdge( Path from, Path p, DefaultModelProblemCollector problems )
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

    @Override
    public TransformerContext build()
    {
        return context;
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
