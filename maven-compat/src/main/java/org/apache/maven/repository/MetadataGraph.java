package org.apache.maven.repository;

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

import java.util.ArrayList;
import java.util.Collection;

/**
 * This is the main graph data structure used by the RepositorySystem to present tree and graph objects.
 *
 * @author Oleg Gusakov
 *
 */
public class MetadataGraph
{
    /** all graph nodes */
    Collection<MetadataGraphNode> nodes;

    /** entry point for tree-like structures */
    MetadataGraphNode entry;

    public MetadataGraph( MetadataGraphNode entry )
    {
        this();

        this.entry = entry;
    }

    public MetadataGraph()
    {
        nodes = new ArrayList<>( 64 );
    }

    public void addNode( MetadataGraphNode node )
    {
        nodes.add( node );
    }

    /**
     * find a node by the GAV (metadata)
     *
     * @param md
     */
    public MetadataGraphNode findNode( MavenArtifactMetadata md )
    {
        for ( MetadataGraphNode mgn : nodes )
        {
            if ( mgn.metadata.equals( md ) )
            {
                return mgn;
            }
        }

        MetadataGraphNode node = new MetadataGraphNode( md );
        addNode( node );
        return node;
    }

    /**
     * getter
     */
    public MetadataGraphNode getEntry()
    {
        return entry;
    }

    /**
     * getter
     */
    public Collection<MetadataGraphNode> getNodes()
    {
        return nodes;
    }
}
