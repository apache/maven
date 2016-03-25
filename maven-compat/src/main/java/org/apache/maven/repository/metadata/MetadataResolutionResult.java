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

import org.apache.maven.artifact.ArtifactScopeEnum;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;

/**
 * This object is tinted with ClasspathTransformation and GraphConflictResolver.
 * Get rid of them after debugging
 *
 * @author <a href="oleg@codehaus.org">Oleg Gusakov</a>
 */
public class MetadataResolutionResult
{
    MetadataTreeNode treeRoot;

    /**
     * these components are are initialized on demand by
     * explicit call of the initTreeProcessing()
     */
    ClasspathTransformation classpathTransformation;
    GraphConflictResolver conflictResolver;

    //----------------------------------------------------------------------------
    public MetadataResolutionResult( )
    {
    }
    //----------------------------------------------------------------------------
    public MetadataResolutionResult( MetadataTreeNode root )
    {
        this.treeRoot = root;
    }
    //----------------------------------------------------------------------------
    public MetadataTreeNode getTree()
    {
        return treeRoot;
    }
    //----------------------------------------------------------------------------
    public void setTree( MetadataTreeNode root )
    {
        this.treeRoot = root;
    }

    public void initTreeProcessing( PlexusContainer plexus )
        throws ComponentLookupException
    {
        classpathTransformation = plexus.lookup( ClasspathTransformation.class );
        conflictResolver = plexus.lookup( GraphConflictResolver.class );
    }
    //----------------------------------------------------------------------------
    public MetadataGraph getGraph()
        throws MetadataResolutionException
    {
        return treeRoot == null ? null : new MetadataGraph( treeRoot );
    }
    //----------------------------------------------------------------------------
    public MetadataGraph getGraph( ArtifactScopeEnum scope )
        throws MetadataResolutionException, GraphConflictResolutionException
    {
        if ( treeRoot == null )
        {
            return null;
        }

        if ( conflictResolver == null )
        {
            return null;
        }

        return conflictResolver.resolveConflicts( getGraph(), scope );
    }
    //----------------------------------------------------------------------------
    public MetadataGraph getGraph( MetadataResolutionRequestTypeEnum requestType )
        throws MetadataResolutionException, GraphConflictResolutionException
    {
        if ( requestType == null )
        {
            return null;
        }

        if ( treeRoot == null )
        {
            return null;
        }

        if ( conflictResolver == null )
        {
            return null;
        }

        if ( requestType.equals( MetadataResolutionRequestTypeEnum.classpathCompile ) )
        {
            return conflictResolver.resolveConflicts( getGraph(), ArtifactScopeEnum.compile );
        }
        else if ( requestType.equals( MetadataResolutionRequestTypeEnum.classpathRuntime ) )
        {
            return conflictResolver.resolveConflicts( getGraph(), ArtifactScopeEnum.runtime );
        }
        else if ( requestType.equals( MetadataResolutionRequestTypeEnum.classpathTest ) )
        {
            return conflictResolver.resolveConflicts( getGraph(), ArtifactScopeEnum.test );
        }
        else if ( requestType.equals( MetadataResolutionRequestTypeEnum.graph ) )
        {
            return getGraph();
        }
        else if ( requestType.equals( MetadataResolutionRequestTypeEnum.versionedGraph ) )
        {
            return new MetadataGraph( getTree(), true, false );
        }
        else if ( requestType.equals( MetadataResolutionRequestTypeEnum.scopedGraph ) )
        {
            return new MetadataGraph( getTree(), true, true );
        }
        return null;
    }
    //----------------------------------------------------------------------------
    public ClasspathContainer getClasspath( ArtifactScopeEnum scope )
        throws MetadataGraphTransformationException, MetadataResolutionException
    {
        if ( classpathTransformation == null )
        {
            return null;
        }

        MetadataGraph dirtyGraph = getGraph();
        if ( dirtyGraph == null )
        {
            return null;
        }

        return classpathTransformation.transform( dirtyGraph, scope, false );
    }

    //----------------------------------------------------------------------------
    public MetadataTreeNode getClasspathTree( ArtifactScopeEnum scope )
        throws MetadataGraphTransformationException, MetadataResolutionException
    {
        ClasspathContainer cpc = getClasspath( scope );
        if ( cpc == null )
        {
            return null;
        }

        return cpc.getClasspathAsTree();
    }
    //----------------------------------------------------------------------------
    //----------------------------------------------------------------------------
}
