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

/**
 * metadata graph edge - combination of version, scope and depth define
 * an edge in the graph
 *
 * @author <a href="oleg@codehaus.org">Oleg Gusakov</a>
 *
 */

public class MetadataGraphEdge
{
    String            version;
    ArtifactScopeEnum scope;
    int               depth = -1;
    int               pomOrder = -1;
    boolean           resolved = true;
    String            artifactUri;

    /**
     * capturing where this link came from
     * and where it is linked to.
     *
     *   In the first implementation only source used for explanatory function
     */
    MetadataGraphVertex  source;
    MetadataGraphVertex  target;

    //----------------------------------------------------------------------------
    public MetadataGraphEdge( final String version, final boolean resolved, final ArtifactScopeEnum scope, final String artifactUri, final int depth,
                              final int pomOrder )
    {
        super();
        this.version = version;
        this.scope = scope;
        this.artifactUri = artifactUri;
        this.depth = depth;
        this.resolved = resolved;
        this.pomOrder = pomOrder;
    }
    //----------------------------------------------------------------------------
    /**
     * helper for equals
     */
    private static boolean objectsEqual( final Object o1, final Object o2 )
    {
        if ( o1 == null && o2 == null )
        {
            return true;
        }
        if ( o1 == null || o2 == null )
        {
            return false; // as they are not both null
        }
        return o1.equals( o2 );
    }

    //----------------------------------------------------------------------------
    /**
     * used to eliminate exact duplicates in the edge list
     */
    @Override
    @SuppressWarnings( "checkstyle:equalshashcode" )
    public boolean equals( final Object o )
    {
        if ( o instanceof MetadataGraphEdge )
        {
            final MetadataGraphEdge e = (MetadataGraphEdge) o;

            return objectsEqual( version, e.version )
                && ArtifactScopeEnum.checkScope( scope ).getScope().
                    equals( ArtifactScopeEnum.checkScope( e.scope ).getScope() )
                && depth == e.depth;
        }
        return false;
    }

    //----------------------------------------------------------------------------
    public String getVersion()
    {
        return version;
    }

    public void setVersion( final String version )
    {
        this.version = version;
    }

    public ArtifactScopeEnum getScope()
    {
        return scope;
    }

    public void setScope( final ArtifactScopeEnum scope )
    {
        this.scope = scope;
    }

    public int getDepth()
    {
        return depth;
    }

    public void setDepth( final int depth )
    {
        this.depth = depth;
    }

    public boolean isResolved()
    {
        return resolved;
    }

    public void setResolved( final boolean resolved )
    {
        this.resolved = resolved;
    }

    public int getPomOrder()
    {
        return pomOrder;
    }

    public void setPomOrder( final int pomOrder )
    {
        this.pomOrder = pomOrder;
    }

    public String getArtifactUri()
    {
        return artifactUri;
    }

    public void setArtifactUri( final String artifactUri )
    {
        this.artifactUri = artifactUri;
    }

    public MetadataGraphVertex getSource()
    {
        return source;
    }

    public void setSource( final MetadataGraphVertex source )
    {
        this.source = source;
    }

    public MetadataGraphVertex getTarget()
    {
        return target;
    }

    public void setTarget( final MetadataGraphVertex target )
    {
        this.target = target;
    }

    @Override
    public String toString()
    {
        return "[ " + "FROM:("
            + ( source == null ? "no source" : ( source.md == null ? "no source MD" : source.md.toString() ) ) + ") "
            + "TO:(" + ( target == null ? "no target" : ( target.md == null ? "no target MD" : target.md.toString() ) )
            + ") " + "version=" + version + ", scope=" + ( scope == null ? "null" : scope.getScope() ) + ", depth="
            + depth + "]";
    }
    //----------------------------------------------------------------------------
    //----------------------------------------------------------------------------
}
