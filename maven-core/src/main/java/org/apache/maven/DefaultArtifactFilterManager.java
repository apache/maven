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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ExclusionSetFilter;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;

/**
 * @author Jason van Zyl
 * @version $Id$
 * @todo this should probably be a component with some dynamic control of filtering
 */
@Component(role = ArtifactFilterManager.class)
public class DefaultArtifactFilterManager 
    implements ArtifactFilterManager
{

    private static final Set<String> DEFAULT_EXCLUSIONS;

    @Requirement
    private PlexusContainer plexus;

    static
    {
        List<String> artifacts = new ArrayList<String>();

        artifacts.add( "classworlds" );
        artifacts.add( "plexus-classworlds" );
        artifacts.add( "maven-artifact" );
        artifacts.add( "maven-artifact-manager" );
        artifacts.add( "maven-artifact-resolver" );
        artifacts.add( "maven-build-context" );
        artifacts.add( "maven-compat" );
        artifacts.add( "maven-core" );
        artifacts.add( "maven-error-diagnoser" );
        artifacts.add( "maven-error-diagnostics" );
        artifacts.add( "maven-lifecycle" );
        artifacts.add( "maven-model" );
        artifacts.add( "maven-model-builder" );
        artifacts.add( "maven-monitor" );
        artifacts.add( "maven-plugin-api" );
        artifacts.add( "maven-plugin-descriptor" );
        artifacts.add( "maven-plugin-parameter-documenter" );
        artifacts.add( "maven-plugin-registry" );
        artifacts.add( "maven-profile" );
        artifacts.add( "maven-project" );
        artifacts.add( "maven-repository-metadata" );
        artifacts.add( "maven-settings" );
        artifacts.add( "maven-toolchain" );
        artifacts.add( "plexus-component-api" );
        artifacts.add( "plexus-container-default" );
        artifacts.add( "wagon-provider-api" );
        artifacts.add( "wagon-manager" );

        /*
         * NOTE: Don't exclude the wagons or any of their dependencies (apart from the wagon API). This would otherwise
         * provoke linkage errors for wagons contributed by build extensions. We also don't need to exclude the wagons
         * from plugins. Plugins that use wagons directly and declare the corresponding dependency will simply use a
         * wagon from their plugin realm.
         */

        DEFAULT_EXCLUSIONS = new CopyOnWriteArraySet<String>( artifacts);
    }

    protected Set<String> excludedArtifacts = new HashSet<String>( DEFAULT_EXCLUSIONS );

    /**
     * @deprecated Use this class as a component instead, and then use getArtifactFilter().
     */
    public static ArtifactFilter createStandardFilter()
    {
        // TODO: configure this from bootstrap or scan lib
        return new ExclusionSetFilter( DEFAULT_EXCLUSIONS );
    }

    /**
     * Returns the artifact filter for the core + extension artifacts.
     *
     * @see org.apache.maven.ArtifactFilterManager#getArtifactFilter()
     */
    public ArtifactFilter getArtifactFilter()
    {
        Set<String> excludes = new LinkedHashSet<String>( excludedArtifacts );

        for ( ArtifactFilterManagerDelegate delegate : getDelegates() )
        {
            delegate.addExcludes( excludes );
        }

        return new ExclusionSetFilter( excludes );
    }

    /**
     * Returns the artifact filter for the standard core artifacts.
     *
     * @see org.apache.maven.ArtifactFilterManager#getExtensionArtifactFilter()
     */
    public ArtifactFilter getCoreArtifactFilter()
    {
        Set<String> excludes = new LinkedHashSet<String>( DEFAULT_EXCLUSIONS );

        for ( ArtifactFilterManagerDelegate delegate : getDelegates() )
        {
            delegate.addCoreExcludes( excludes );
        }

        return new ExclusionSetFilter( excludes );
    }

    private List<ArtifactFilterManagerDelegate> getDelegates()
    {
        try
        {
            return plexus.lookupList( ArtifactFilterManagerDelegate.class );
        }
        catch ( ComponentLookupException e )
        {
            return new ArrayList<ArtifactFilterManagerDelegate>();
        }
    }

    /* (non-Javadoc)
     * @see org.apache.maven.ArtifactFilterManager#excludeArtifact(java.lang.String)
     */
    public void excludeArtifact( String artifactId )
    {
        excludedArtifacts.add( artifactId );
    }

}
