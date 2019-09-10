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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Exclusion;

import java.util.List;

/**
 * Filter to exclude from a list of artifact patterns.
 */
public class ExclusionArtifactFilter implements ArtifactFilter
{
    private static final String WILDCARD = "*";

    private final List<Exclusion> exclusions;

    public ExclusionArtifactFilter( List<Exclusion> exclusions )
    {
        this.exclusions = exclusions;
    }

    @Override
    public boolean include( Artifact artifact )
    {
        for ( Exclusion exclusion : exclusions )
        {
            if ( WILDCARD.equals( exclusion.getGroupId() ) && WILDCARD.equals( exclusion.getArtifactId() ) )
            {
                return false;
            }
            if ( WILDCARD.equals( exclusion.getGroupId() )
                && exclusion.getArtifactId().equals( artifact.getArtifactId() ) )
            {
                return false;
            }
            if ( WILDCARD.equals( exclusion.getArtifactId() )
                && exclusion.getGroupId().equals( artifact.getGroupId() ) )
            {
                return false;
            }
            if ( exclusion.getGroupId().equals( artifact.getGroupId() )
                && exclusion.getArtifactId().equals( artifact.getArtifactId() ) )
            {
                return false;
            }
        }
        return true;
    }
}
