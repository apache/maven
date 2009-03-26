/**
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.maven.repository;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.artifact.Artifact;

/**
 * artifact metadata Tree
 * 
 * @author <a href="oleg@codehaus.org">Oleg Gusakov</a>
 */
public class MetadataTreeNode
{
    private static final int DEFAULT_CHILDREN_COUNT = 16;

    /**
     * this node's artifact MD
     */
    private Artifact metadata;

    /**
     * is there a real artifact behind this node, or it's just a helper ?
     */
    private boolean real = true;

    /**
     * parent node
     */
    private MetadataTreeNode parent;

    /**
     * node unique id, used to identify this node in external tree manipulations, such as
     */
    private int id;

    /**
     * actual found versions
     */
    private List<MetadataTreeNode> children;
    
    // ------------------------------------------------------------------------
    public int countNodes()
    {
        return countNodes( this );
    }

    // ------------------------------------------------------------------------
    public static int countNodes( MetadataTreeNode node )
    {
        int res = 1;

        if ( node.children != null && node.children.size() > 0 )
        {
            for ( MetadataTreeNode child : node.children )
            {
                res += countNodes( child );
            }
        }

        return res;
    }

    // ------------------------------------------------------------------------
    public MetadataTreeNode()
    {
    }

    // ------------------------------------------------------------------------
    public MetadataTreeNode( Artifact md, MetadataTreeNode parent )
    {

        this.metadata = md;
        this.parent = parent;
    }

    // ------------------------------------------------------------------------
    /**
     * dependencies are ordered in the POM - they should be added in the POM order
     */
    public MetadataTreeNode addChild( MetadataTreeNode kid )
    {
        if ( kid == null )
        {
            return this;
        }

        if ( children == null )
        {
            children = new ArrayList<MetadataTreeNode>( DEFAULT_CHILDREN_COUNT );
        }

        kid.setParent( this );
        children.add( kid );

        return this;
    }

    @Override
    public String toString()
    {
        return metadata == null ? "no metadata, parent " + ( parent == null ? "null" : parent.toString() ) : metadata.toString()
            + ":d=" + getDepth();
    }

    // ------------------------------------------------------------------------
    public boolean hasChildren()
    {
        return children != null;
    }

    // ------------------------------------------------------------------------
    public Artifact getMetadata()
    {
        return metadata;
    }

    public MetadataTreeNode getParent()
    {
        return parent;
    }

    public int getDepth()
    {
        int depth = 0;

        for ( MetadataTreeNode p = parent; p != null; p = p.parent )
            ++depth;

        return depth;
    }

    public int getMaxDepth( int depth )
    {
        int res = 0;

        if ( !hasChildren() )
            return depth + 1;

        for ( MetadataTreeNode kid : children )
        {
            int kidDepth = kid.getMaxDepth( depth + 1 );
            if ( kidDepth > res )
                res = kidDepth;
        }

        return res;
    }

    public void setParent( MetadataTreeNode parent )
    {
        this.parent = parent;
    }

    public List<MetadataTreeNode> getChildren()
    {
        return children;
    }

    public boolean isReal()
    {
        return real;
    }

    public void setReal( boolean real)
    {
        this.real = real;
    }

    // ------------------------------------------------------------------------
    public static final MetadataTreeNode deepCopy( MetadataTreeNode node )
    {
        MetadataTreeNode res = new MetadataTreeNode( node.getMetadata(), node.getParent() );
        res.setId( node.getId() );

        if ( node.hasChildren() )
            for ( MetadataTreeNode kid : node.children )
            {
                MetadataTreeNode deepKid = deepCopy( kid );
                res.addChild( deepKid );
            }

        return res;
    }

    // ----------------------------------------------------------------
    /**
     * helper method to print the tree into a Writer
     */
    public static final void showNode( MetadataTreeNode n, int level, Writer wr )
        throws IOException
    {
        if( n == null )
        {
            wr.write( "null node" );
            return;
        }
        for ( int i = 0; i < level; i++ )
            wr.write( "  " );

        wr.write( level + " " + n.getMetadata() + "\n" );

        if ( n.hasChildren() )
        {
            for ( MetadataTreeNode kid : n.getChildren() )
                showNode( kid, level + 1, wr );
        }
    }

    // ----------------------------------------------------------------
    /**
     * helper method to print the tree into sysout
     */
    public static final void showNode( MetadataTreeNode n, int level )
        throws IOException
    {
        StringWriter sw = new StringWriter();
        MetadataTreeNode.showNode( n, 0, sw );
        System.out.println( sw.toString() );
    }

    // ------------------------------------------------------------------------
    public int getId()
    {
        return id;
    }

    public void setId( int id )
    {
        this.id = id;
    }

    // ------------------------------------------------------------------------
    public static void reNumber( MetadataTreeNode node, int startNum )
    {
        reNum( node, new Counter( startNum ) );
    }

    // ------------------------------------------------------------------------
    private static void reNum( MetadataTreeNode node, Counter num )
    {
        node.setId( num.next() );

        if ( node.hasChildren() )
            for ( MetadataTreeNode kid : node.getChildren() )
                reNum( kid, num );
    }
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
}

// ------------------------------------------------------------------------
class Counter
{
    int n;

    public Counter( int n )
    {
        this.n = n;
    }

    int next()
    {
        return n++;
    }
}
