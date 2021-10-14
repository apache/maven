package org.apache.maven.repository.metadata;

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

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import org.apache.maven.artifact.ArtifactScopeEnum;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;

/**
 * Default conflict resolver.Implements closer newer first policy by default, but could be configured via plexus
 *
 * @author <a href="mailto:oleg@codehaus.org">Oleg Gusakov</a>
 */
@Component( role = GraphConflictResolver.class )
public class DefaultGraphConflictResolver
    implements GraphConflictResolver
{
    /**
     * artifact, closer to the entry point, is selected
     */
    @Requirement( role = GraphConflictResolutionPolicy.class )
    protected GraphConflictResolutionPolicy policy;

    // -------------------------------------------------------------------------------------
    // -------------------------------------------------------------------------------------
    public MetadataGraph resolveConflicts( final MetadataGraph graph, final ArtifactScopeEnum scope )
        throws GraphConflictResolutionException
    {
        if ( policy == null )
        {
            throw new GraphConflictResolutionException( "no GraphConflictResolutionPolicy injected" );
        }

        if ( graph == null )
        {
            return null;
        }

        final MetadataGraphVertex entry = graph.getEntry();
        if ( entry == null )
        {
            return null;
        }

        if ( graph.isEmpty() )
        {
            throw new GraphConflictResolutionException( "graph with an entry, but not vertices do not exist" );
        }

        if ( graph.isEmptyEdges() )
        {
            return null; // no edges - nothing to worry about
        }

        final TreeSet<MetadataGraphVertex> vertices = graph.getVertices();

        try
        {
            // edge case - single vertex graph
            if ( vertices.size() == 1 )
            {
                return new MetadataGraph( entry );
            }

            final ArtifactScopeEnum requestedScope = ArtifactScopeEnum.checkScope( scope );

            final MetadataGraph res = new MetadataGraph( vertices.size() );
            res.setVersionedVertices( false );
            res.setScopedVertices( false );

            final MetadataGraphVertex resEntry = res.addVertex( entry.getMd() );
            res.setEntry( resEntry );

            res.setScope( requestedScope );

            for ( final MetadataGraphVertex v : vertices )
            {
                final List<MetadataGraphEdge> ins = graph.getIncidentEdges( v );
                final MetadataGraphEdge edge = cleanEdges( v, ins, requestedScope );

                if ( edge == null )
                { // no edges - don't need this vertex any more
                    if ( entry.equals( v ) )
                    { // unless it's an entry point.
                        // currently processing the entry point - it should not have any entry incident edges
                        res.getEntry().getMd().setWhy( "This is a graph entry point. No links." );
                    }
                    else
                    {
                        // System.out.println("--->"+v.getMd().toDomainString()
                        // +" has been terminated on this entry set\n-------------------\n"
                        // +ins
                        // +"\n-------------------\n"
                        // );
                    }
                }
                else
                {
                    // System.out.println("+++>"+v.getMd().toDomainString()+" still has "+edge.toString() );
                    // fill in domain md with actual version data
                    final ArtifactMetadata md = v.getMd();
                    final ArtifactMetadata newMd =
                        new ArtifactMetadata( md.getGroupId(), md.getArtifactId(), edge.getVersion(), md.getType(),
                                              md.getScopeAsEnum(), md.getClassifier(), edge.getArtifactUri(),
                                              edge.getSource() == null ? "" : edge.getSource().getMd().toString(),
                                              edge.isResolved(), edge.getTarget() == null ? null
                                                              : edge.getTarget().getMd().getError() );
                    final MetadataGraphVertex newV = res.addVertex( newMd );
                    final MetadataGraphVertex sourceV = res.addVertex( edge.getSource().getMd() );

                    res.addEdge( sourceV, newV, edge );
                }
            }
            // System.err.println("Original graph("+graph.getVertices().size()+"):\n"+graph.toString());
            // System.err.println("Cleaned("+requestedScope+") graph("+res.getVertices().size()+"):\n"+res.toString());
            // System.err.println("Linked("+requestedScope+")
            // subgraph("+linkedRes.getVertices().size()+"):\n"+linkedRes.toString());
            return findLinkedSubgraph( res );
        }
        catch ( final MetadataResolutionException e )
        {
            throw new GraphConflictResolutionException( e );
        }
    }

    // -------------------------------------------------------------------------------------
    private MetadataGraph findLinkedSubgraph( final MetadataGraph g )
    {
        if ( g.getVertices().size() == 1 )
        {
            return g;
        }

        final List<MetadataGraphVertex> visited = new ArrayList<>( g.getVertices().size() );
        visit( g.getEntry(), visited, g );

        final List<MetadataGraphVertex> dropList = new ArrayList<>( g.getVertices().size() );

        // collect drop list
        for ( final MetadataGraphVertex v : g.getVertices() )
        {
            if ( !visited.contains( v ) )
            {
                dropList.add( v );
            }
        }

        if ( dropList.size() < 1 )
        {
            return g;
        }

        // now - drop vertices
        final TreeSet<MetadataGraphVertex> vertices = g.getVertices();
        for ( final MetadataGraphVertex v : dropList )
        {
            vertices.remove( v );
        }

        return g;
    }

    // -------------------------------------------------------------------------------------
    private void visit( final MetadataGraphVertex from, final List<MetadataGraphVertex> visited, final MetadataGraph graph )
    {
        if ( visited.contains( from ) )
        {
            return;
        }

        visited.add( from );

        final List<MetadataGraphEdge> exitList = graph.getExcidentEdges( from );
        // String s = "|---> "+from.getMd().toString()+" - "+(exitList == null ? -1 : exitList.size()) + " exit links";
        if ( exitList != null && exitList.size() > 0 )
        {
            for ( final MetadataGraphEdge e : graph.getExcidentEdges( from ) )
            {
                visit( e.getTarget(), visited, graph );
            }
        }
    }

    // -------------------------------------------------------------------------------------
    private MetadataGraphEdge cleanEdges( final MetadataGraphVertex v, final List<MetadataGraphEdge> edges,
                                          final ArtifactScopeEnum scope )
    {
        if ( edges == null || edges.isEmpty() )
        {
            return null;
        }

        if ( edges.size() == 1 )
        {
            final MetadataGraphEdge e = edges.get( 0 );
            if ( scope.encloses( e.getScope() ) )
            {
                return e;
            }

            return null;
        }

        MetadataGraphEdge res = null;

        for ( final MetadataGraphEdge e : edges )
        {
            if ( !scope.encloses( e.getScope() ) )
            {
                continue;
            }

            if ( res == null )
            {
                res = e;
            }
            else
            {
                res = policy.apply( e, res );
            }
        }

        return res;
    }
    // -------------------------------------------------------------------------------------
    // -------------------------------------------------------------------------------------
}
