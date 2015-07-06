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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;

/**
 * Apply multiple filters.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class AndArtifactFilter
    implements ArtifactFilter
{
    private Set<ArtifactFilter> filters;

    public AndArtifactFilter()
    {
        this.filters = new LinkedHashSet<>();
    }

    public AndArtifactFilter( List<ArtifactFilter> filters )
    {
        this.filters = new LinkedHashSet<>( filters );
    }

    public boolean include( Artifact artifact )
    {
        boolean include = true;
        for ( Iterator<ArtifactFilter> i = filters.iterator(); i.hasNext() && include; )
        {
            ArtifactFilter filter = i.next();
            if ( !filter.include( artifact ) )
            {
                include = false;
            }
        }
        return include;
    }

    public void add( ArtifactFilter artifactFilter )
    {
        filters.add( artifactFilter );
    }

    public List<ArtifactFilter> getFilters()
    {
        return new ArrayList<>( filters );
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

        if ( !( obj instanceof AndArtifactFilter ) )
        {
            return false;
        }

        AndArtifactFilter other = (AndArtifactFilter) obj;

        return filters.equals( other.filters );
    }
}
