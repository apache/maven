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

import java.util.Objects;

/**
 * Filter to only retain objects in the given artifactScope or better.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class ScopeArtifactFilter
    extends AbstractScopeArtifactFilter
{

    private final String scope;

    public ScopeArtifactFilter( String scope )
    {
        this.scope = scope;

        addScopeInternal( scope );
    }

    public String getScope()
    {
        return scope;
    }

    @Override
    public int hashCode()
    {
        int hash = 17;

        hash = hash * 31 + ( scope != null ? scope.hashCode() : 0 );

        return hash;
    }

    @Override
    public boolean equals( Object obj )
    {
        if ( this == obj )
        {
            return true;
        }

        if ( !( obj instanceof ScopeArtifactFilter ) )
        {
            return false;
        }

        ScopeArtifactFilter other = (ScopeArtifactFilter) obj;

        return Objects.equals( scope, other.scope );
    }

}
