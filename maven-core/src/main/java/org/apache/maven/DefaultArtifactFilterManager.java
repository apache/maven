package org.apache.maven;

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

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ExclusionSetFilter;
import org.apache.maven.extension.internal.CoreExportsProvider;

/**
 * @author Jason van Zyl
 */
@Named
@Singleton
@SuppressWarnings( "deprecation" )
public class DefaultArtifactFilterManager
    implements ArtifactFilterManager
{

    // this is a live injected collection
    protected final List<ArtifactFilterManagerDelegate> delegates;

    protected Set<String> excludedArtifacts;

    private final Set<String> coreArtifacts;

    @Inject
    public DefaultArtifactFilterManager( List<ArtifactFilterManagerDelegate> delegates,
                                         CoreExportsProvider coreExports )
    {
        this.delegates = delegates;
        this.coreArtifacts = coreExports.get().getExportedArtifacts();
    }

    private synchronized Set<String> getExcludedArtifacts()
    {
        if ( excludedArtifacts == null )
        {
            excludedArtifacts = new LinkedHashSet<>( coreArtifacts );
        }
        return excludedArtifacts;
    }

    /**
     * Returns the artifact filter for the core + extension artifacts.
     *
     * @see org.apache.maven.ArtifactFilterManager#getArtifactFilter()
     */
    public ArtifactFilter getArtifactFilter()
    {
        Set<String> excludes = new LinkedHashSet<>( getExcludedArtifacts() );

        for ( ArtifactFilterManagerDelegate delegate : delegates )
        {
            delegate.addExcludes( excludes );
        }

        return new ExclusionSetFilter( excludes );
    }

    /**
     * Returns the artifact filter for the standard core artifacts.
     *
     * @see org.apache.maven.ArtifactFilterManager#getCoreArtifactFilter()
     */
    public ArtifactFilter getCoreArtifactFilter()
    {
        return new ExclusionSetFilter( getCoreArtifactExcludes() );
    }

    public void excludeArtifact( String artifactId )
    {
        getExcludedArtifacts().add( artifactId );
    }

    public Set<String> getCoreArtifactExcludes()
    {
        Set<String> excludes = new LinkedHashSet<>( coreArtifacts );

        for ( ArtifactFilterManagerDelegate delegate : delegates )
        {
            delegate.addCoreExcludes( excludes );
        }

        return excludes;
    }

}
