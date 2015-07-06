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
import java.util.List;

/**
 * MetadataGraph node - as it's a directed graph - holds adjacency lists for incident and exident nodes
 *
 * @author Oleg Gusakov
 *
 */
public class MetadataGraphNode
{
    /** node payload */
    MavenArtifactMetadata metadata;

    /** nodes, incident to this (depend on me) */
    List<MetadataGraphNode> inNodes;

    /** nodes, exident to this (I depend on) */
    List<MetadataGraphNode> exNodes;

    public MetadataGraphNode()
    {
        inNodes = new ArrayList<>( 4 );
        exNodes = new ArrayList<>( 8 );
    }

    public MetadataGraphNode( MavenArtifactMetadata metadata )
    {
        this();
        this.metadata = metadata;
    }

    public MetadataGraphNode addIncident( MetadataGraphNode node )
    {
        inNodes.add( node );
        return this;
    }

    public MetadataGraphNode addExident( MetadataGraphNode node )
    {
        exNodes.add( node );
        return this;
    }

    @Override
    public boolean equals( Object obj )
    {
        if ( obj == null )
        {
            return false;
        }

        if ( MetadataGraphNode.class.isAssignableFrom( obj.getClass() ) )
        {
            MetadataGraphNode node2 = (MetadataGraphNode) obj;

            if ( node2.metadata == null )
            {
                return metadata == null;
            }

            return metadata != null && metadata.toString().equals( node2.metadata.toString() );
        }
        else
        {
            return super.equals( obj );
        }
    }

    @Override
    public int hashCode()
    {
        if ( metadata == null )
        {
            return super.hashCode();
        }

        return metadata.toString().hashCode();
    }
}
