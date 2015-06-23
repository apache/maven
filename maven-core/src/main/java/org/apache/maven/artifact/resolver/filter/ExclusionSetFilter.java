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

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.maven.artifact.Artifact;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 */
public class ExclusionSetFilter
    implements ArtifactFilter
{
    private Set<String> excludes;

    public ExclusionSetFilter( String[] excludes )
    {
        this.excludes = new LinkedHashSet<>( Arrays.asList( excludes ) );
    }

    public ExclusionSetFilter( Set<String> excludes )
    {
        this.excludes = excludes;
    }

    public boolean include( Artifact artifact )
    {
        String id = artifact.getArtifactId();

        if ( excludes.contains( id ) )
        {
            return false;
        }

        id = artifact.getGroupId() + ':' + id;

        return !excludes.contains( id );

    }

    @Override
    public int hashCode()
    {
        int hash = 17;
        hash = hash * 31 + excludes.hashCode();
        return hash;
    }

    @Override
    public boolean equals( Object obj )
    {
        if ( this == obj )
        {
            return true;
        }

        if ( !( obj instanceof ExclusionSetFilter ) )
        {
            return false;
        }

        ExclusionSetFilter other = (ExclusionSetFilter) obj;

        return excludes.equals( other.excludes );
    }
}
