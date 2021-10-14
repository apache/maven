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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.apache.maven.artifact.ArtifactScopeEnum;

/**
 * maven dependency metadata graph
 *
 * @author <a href="oleg@codehaus.org">Oleg Gusakov</a>
 *
 */
public class MetadataGraph
{
    public static final int DEFAULT_VERTICES = 32;
    public static final int DEFAULT_EDGES    = 64;

    // flags to indicate the granularity of vertices
    private boolean versionedVertices = false;
    private boolean scopedVertices    = false;
    /**
    * the entry point we started building the graph from
    */
    MetadataGraphVertex entry;

    // graph vertices
    TreeSet<MetadataGraphVertex> vertices;

    /**
     * incident and excident edges per node
     */
    Map<MetadataGraphVertex, List<MetadataGraphEdge>> incidentEdges;
    Map<MetadataGraphVertex, List<MetadataGraphEdge>> excidentEdges;

    /**
     *  null in dirty graph, actual
     *  scope for conflict-resolved graph
     */
    ArtifactScopeEnum scope;

    //------------------------------------------------------------------------
    /**
     * init graph
     */
    public MetadataGraph( final int nVertices )
    {
        init( nVertices, 2 * nVertices );
    }
    public MetadataGraph( final int nVertices, final int nEdges )
    {
        init( nVertices, nEdges );
    }
    //------------------------------------------------------------------------
    /**
     * construct a single vertex
     */
    public MetadataGraph( final MetadataGraphVertex entry )
        throws MetadataResolutionException
    {
        checkVertex( entry );
        checkVertices( 1 );

        entry.setCompareVersion( versionedVertices );
        entry.setCompareScope( scopedVertices );

        vertices.add( entry );
        this.entry = entry;
    }
    //------------------------------------------------------------------------
    /**
     * construct graph from a "dirty" tree
     */
    public MetadataGraph( final MetadataTreeNode tree )
        throws MetadataResolutionException
    {
        this( tree, false, false );
    }
    //------------------------------------------------------------------------
    /**
     * construct graph from a "dirty" tree
     *
     * @param tree "dirty" tree root
     * @param versionedVertices true if graph nodes should be versioned (different versions -&gt; different nodes)
     * @param scopedVertices true if graph nodes should be versioned and scoped
     * (different versions and/or scopes -&gt; different nodes)
     *
     */
    public MetadataGraph( final MetadataTreeNode tree, final boolean versionedVertices, final boolean scopedVertices )
        throws MetadataResolutionException
    {
        if ( tree == null )
        {
            throw new MetadataResolutionException( "tree is null" );
        }

        setVersionedVertices( versionedVertices );
        setScopedVertices( scopedVertices );

        this.versionedVertices = scopedVertices || versionedVertices;
        this.scopedVertices = scopedVertices;

        final int count = countNodes( tree );

        init( count, count + ( count / 2 ) );

        processTreeNodes( null, tree, 0, 0 );
    }
    //------------------------------------------------------------------------
    private void processTreeNodes( final MetadataGraphVertex parentVertex, final MetadataTreeNode node, final int depth, final int pomOrder )
        throws MetadataResolutionException
    {
        if ( node == null )
        {
            return;
        }

        final MetadataGraphVertex vertex = new MetadataGraphVertex( node.md, versionedVertices, scopedVertices );
        if ( !vertices.contains( vertex ) )
        {
            vertices.add( vertex );
        }

        if ( parentVertex != null ) // then create the edge
        {
            final ArtifactMetadata md = node.getMd();
            final MetadataGraphEdge e =
                new MetadataGraphEdge( md.version, md.resolved, md.artifactScope, md.artifactUri, depth, pomOrder );
            addEdge( parentVertex, vertex, e );
        }
        else
        {
            entry = vertex;
        }

        final MetadataTreeNode[] kids = node.getChildren();
        if ( kids == null || kids.length < 1 )
        {
            return;
        }

        for ( int i = 0; i < kids.length; i++ )
        {
            final MetadataTreeNode n = kids[i];
            processTreeNodes( vertex, n, depth + 1, i );
        }
    }
    //------------------------------------------------------------------------
    public MetadataGraphVertex findVertex( final ArtifactMetadata md )
    {
        if ( md == null || vertices == null || vertices.size() < 1 )
        {
            return null;
        }

        final MetadataGraphVertex v = new MetadataGraphVertex( md );
        v.setCompareVersion( versionedVertices );
        v.setCompareScope( scopedVertices );

        for ( final MetadataGraphVertex gv : vertices )
        {
            if ( gv.equals( v ) )
            {
                return gv;
            }
        }

        return null;
    }
    //------------------------------------------------------------------------
    public MetadataGraphVertex addVertex( final ArtifactMetadata md )
    {
        if ( md == null )
        {
            return null;
        }

        checkVertices();

        MetadataGraphVertex v = findVertex( md );
        if ( v != null )
        {
            return v;
        }

        v = new MetadataGraphVertex( md );

        v.setCompareVersion( versionedVertices );
        v.setCompareScope( scopedVertices );

        vertices.add( v );
        return v;
    }
    //------------------------------------------------------------------------
    /**
     * init graph
     */
    private void init( final int nVertices, final int nEdges )
    {
        int nV = nVertices;
        if ( nVertices < 1 )
        {
            nV = 1;
        }

        checkVertices( nV );

        int nE = nVertices;
        if ( nEdges <= nV )
        {
            nE = 2 * nE;
        }

        checkEdges( nE );
    }

    private void checkVertices()
    {
        checkVertices( DEFAULT_VERTICES );
    }

    private void checkVertices( final int nVertices )
    {
        if ( vertices == null )
        {
            vertices = new TreeSet<>();
        }
    }
    private void checkEdges()
    {
        int count = DEFAULT_EDGES;

        if ( vertices != null )
        {
            count = vertices.size() + vertices.size() / 2;
        }

        checkEdges( count );
    }
    private void checkEdges( final int nEdges )
    {
        if ( incidentEdges == null )
        {
            incidentEdges = new HashMap<>( nEdges );
        }
        if ( excidentEdges == null )
        {
            excidentEdges = new HashMap<>( nEdges );
        }
    }
    //------------------------------------------------------------------------
    private static void checkVertex( final MetadataGraphVertex v )
        throws MetadataResolutionException
    {
        if ( v == null )
        {
            throw new MetadataResolutionException( "null vertex" );
        }
        if ( v.getMd() == null )
        {
            throw new MetadataResolutionException( "vertex without metadata" );
        }
    }
    //------------------------------------------------------------------------
    private static void checkEdge( final MetadataGraphEdge e )
        throws MetadataResolutionException
    {
        if ( e == null )
        {
            throw new MetadataResolutionException( "badly formed edge" );
        }
    }
    //------------------------------------------------------------------------
    public List<MetadataGraphEdge> getEdgesBetween( final MetadataGraphVertex vFrom, final MetadataGraphVertex vTo )
    {
        final List<MetadataGraphEdge> edges = getIncidentEdges( vTo );
        if ( edges == null || edges.isEmpty() )
        {
            return null;
        }

        final List<MetadataGraphEdge> res = new ArrayList<>( edges.size() );

        for ( final MetadataGraphEdge e : edges )
        {
            if ( e.getSource().equals( vFrom ) )
            {
                res.add( e );
            }
        }

        return res;
    }
    //------------------------------------------------------------------------
    public MetadataGraph addEdge( final MetadataGraphVertex vFrom, final MetadataGraphVertex vTo, final MetadataGraphEdge e )
        throws MetadataResolutionException
    {
        checkVertex( vFrom );
        checkVertex( vTo );

        checkVertices();

        checkEdge( e );
        checkEdges();

        e.setSource( vFrom );
        e.setTarget( vTo );

        vFrom.setCompareVersion( versionedVertices );
        vFrom.setCompareScope( scopedVertices );

        final List<MetadataGraphEdge> exList = excidentEdges.computeIfAbsent( vFrom, k -> new ArrayList<>() );

        if ( !exList.contains( e ) )
        {
            exList.add( e );
        }

        final List<MetadataGraphEdge> inList = incidentEdges.computeIfAbsent( vTo, k -> new ArrayList<>() );

        if ( !inList.contains( e ) )
        {
            inList.add( e );
        }

        return this;
    }
    //------------------------------------------------------------------------
    public MetadataGraph removeVertex( final MetadataGraphVertex v )
    {
        if ( vertices != null && v != null )
        {
            vertices.remove( v );
        }

        if ( incidentEdges != null )
        {
            incidentEdges.remove( v );
        }

        if ( excidentEdges != null )
        {
            excidentEdges.remove( v );
        }

        return this;

    }
    //------------------------------------------------------------------------
    private static int countNodes( final MetadataTreeNode tree )
    {
        if ( tree == null )
        {
            return 0;
        }

        int count = 1;
        final MetadataTreeNode[] kids = tree.getChildren();
        if ( kids == null || kids.length < 1 )
        {
            return count;
        }
        for ( final MetadataTreeNode n : kids )
        {
            count += countNodes( n );
        }

        return count;
    }

    //------------------------------------------------------------------------
    public MetadataGraphVertex getEntry()
    {
        return entry;
    }

    public void setEntry( final MetadataGraphVertex entry )
    {
        this.entry = entry;
    }

    public TreeSet<MetadataGraphVertex> getVertices()
    {
        return vertices;
    }

    public List<MetadataGraphEdge> getIncidentEdges( final MetadataGraphVertex vertex )
    {
        checkEdges();
        return incidentEdges.get( vertex );
    }

    public List<MetadataGraphEdge> getExcidentEdges( final MetadataGraphVertex vertex )
    {
        checkEdges();
        return excidentEdges.get( vertex );
    }

    public boolean isVersionedVertices()
    {
        return versionedVertices;
    }

    public void setVersionedVertices( final boolean versionedVertices )
    {
        this.versionedVertices = versionedVertices;
    }

    public boolean isScopedVertices()
    {
        return scopedVertices;
    }

    public void setScopedVertices( final boolean scopedVertices )
    {
        this.scopedVertices = scopedVertices;

        // scoped graph is versioned by definition
        if ( scopedVertices )
        {
            versionedVertices = true;
        }
    }

    public ArtifactScopeEnum getScope()
    {
        return scope;
    }

    public void setScope( final ArtifactScopeEnum scope )
    {
        this.scope = scope;
    }

    // ------------------------------------------------------------------------
    public boolean isEmpty()
    {
        return entry == null || vertices == null || vertices.isEmpty();
    }

    //------------------------------------------------------------------------
    public boolean isEmptyEdges()
    {
        return isEmpty() || incidentEdges == null || incidentEdges.isEmpty();
    }
    //------------------------------------------------------------------------
    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder( 512 );
        if ( isEmpty() )
        {
            return "empty";
        }
        for ( final MetadataGraphVertex v : vertices )
        {
            sb.append( "Vertex:  " ).append( v.getMd().toString() ).append( '\n' );
            final List<MetadataGraphEdge> ins = getIncidentEdges( v );
            if ( ins != null )
            {
                for ( final MetadataGraphEdge e : ins )
                {
                    sb.append( "       from :  " ).append( e.toString() ).append( '\n' );
                }
            }
            else
            {
                sb.append( "      no entries\n" );
            }

            final List<MetadataGraphEdge> outs = getExcidentEdges( v );
            if ( outs != null )
            {
                for ( final MetadataGraphEdge e : outs )
                {
                    sb.append( "        to :  " ).append( e.toString() ).append( '\n' );
                }
            }
            else
            {
                sb.append( "      no exit\n" );
            }

            sb.append( "-------------------------------------------------\n" );
        }
        sb.append( "=============================================================\n" );
        return sb.toString();
    }

    //------------------------------------------------------------------------
    //------------------------------------------------------------------------
}
