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

import java.util.ArrayList;
import java.util.Collection;

import org.apache.maven.artifact.Artifact;

/**
 * Apply multiple filters, accepting an artifact if at least one of the filters accepts it.
 * 
 * @author Benjamin Bentmann
 */
public class OrArtifactFilter
    implements ArtifactFilter
{

    private Collection<ArtifactFilter> filters;

    public OrArtifactFilter()
    {
    }

    public OrArtifactFilter( Collection<ArtifactFilter> filters )
    {
        this.filters = filters;
    }

    public boolean include( Artifact artifact )
    {
        if ( filters != null )
        {
            for ( ArtifactFilter filter : filters )
            {
                if ( filter.include( artifact ) )
                {
                    return true;
                }
            }
        }

        return false;
    }

    public void add( ArtifactFilter artifactFilter )
    {
        if ( filters == null )
        {
            filters = new ArrayList<ArtifactFilter>();
        }

        filters.add( artifactFilter );
    }

}
