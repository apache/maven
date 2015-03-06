package org.apache.maven.artifact.resolver.filter;

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

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Filter to only retain objects in the given scope or better. This implementation allows the accumulation of multiple
 * scopes and their associated implied scopes, so that the user can filter apply a series of implication rules in a
 * single step. This should be a more efficient implementation of multiple standard {@link ScopeArtifactFilter}
 * instances ORed together.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @author jdcasey
 */
public class CumulativeScopeArtifactFilter
    extends AbstractScopeArtifactFilter
{

    private Set<String> scopes;

    /**
     * Create a new filter with the specified scopes and their implied scopes enabled.
     *
     * @param scopes The scopes to enable, along with all implied scopes, may be {@code null}.
     */
    public CumulativeScopeArtifactFilter( Collection<String> scopes )
    {
        this.scopes = new HashSet<>();

        addScopes( scopes );
    }

    /**
     * Creates a new filter that combines the specified filters.
     *
     * @param filters The filters to combine, may be {@code null}.
     */
    public CumulativeScopeArtifactFilter( CumulativeScopeArtifactFilter... filters )
    {
        this.scopes = new HashSet<>();

        if ( filters != null )
        {
            for ( CumulativeScopeArtifactFilter filter : filters )
            {
                addScopes( filter.getScopes() );
            }
        }
    }

    private void addScopes( Collection<String> scopes )
    {
        if ( scopes != null )
        {
            for ( String scope : scopes )
            {
                addScope( scope );
            }
        }
    }

    private void addScope( String scope )
    {
        this.scopes.add( scope );

        addScopeInternal( scope );
    }

    public Set<String> getScopes()
    {
        return scopes;
    }

    @Override
    public int hashCode()
    {
        int hash = 17;

        hash = hash * 31 + scopes.hashCode();

        return hash;
    }

    @Override
    public boolean equals( Object obj )
    {
        if ( this == obj )
        {
            return true;
        }

        if ( !( obj instanceof CumulativeScopeArtifactFilter ) )
        {
            return false;
        }

        CumulativeScopeArtifactFilter that = (CumulativeScopeArtifactFilter) obj;

        return scopes.equals( that.scopes );
    }

}
