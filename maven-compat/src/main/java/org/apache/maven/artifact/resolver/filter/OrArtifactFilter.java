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
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.maven.artifact.Artifact;

/**
 * Apply multiple filters, accepting an artifact if at least one of the filters accepts it.
 *
 * @author Benjamin Bentmann
 */
public class OrArtifactFilter
    implements ArtifactFilter
{

    private Set<ArtifactFilter> filters;

    public OrArtifactFilter()
    {
        this.filters = new LinkedHashSet<>();
    }

    public OrArtifactFilter( Collection<ArtifactFilter> filters )
    {
        this.filters = new LinkedHashSet<>( filters );
    }

    public boolean include( Artifact artifact )
    {
        for ( ArtifactFilter filter : filters )
        {
            if ( filter.include( artifact ) )
            {
                return true;
            }
        }

        return false;
    }

    public void add( ArtifactFilter artifactFilter )
    {
        filters.add( artifactFilter );
    }

    @Override
    public int hashCode()
    {
        int hash = 17;
        hash = hash * 31 + filters.hashCode();
        return hash;
    }

    @Override
    public boolean equals( Object obj )
    {
        if ( this == obj )
        {
            return true;
        }

        if ( !( obj instanceof OrArtifactFilter ) )
        {
            return false;
        }

        OrArtifactFilter other = (OrArtifactFilter) obj;

        return filters.equals( other.filters );
    }

}
