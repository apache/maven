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
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ExclusionSetFilter;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;

/**
 * @author Jason van Zyl
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
        Set<String> artifacts = new HashSet<String>();

        artifacts.add( "classworlds:classworlds" );
        artifacts.add( "org.codehaus.plexus:plexus-classworlds" );
        artifacts.add( "org.codehaus.plexus:plexus-component-api" );
        artifacts.add( "org.codehaus.plexus:plexus-container-default" );
        artifacts.add( "plexus:plexus-container-default" );
        artifacts.add( "org.sonatype.spice:spice-inject-plexus" );
        artifacts.add( "org.sonatype.sisu:sisu-inject-plexus" );
        artifacts.add( "org.apache.maven:maven-artifact" );
        artifacts.add( "org.apache.maven:maven-aether-provider" );
        artifacts.add( "org.apache.maven:maven-artifact-manager" );
        artifacts.add( "org.apache.maven:maven-compat" );
        artifacts.add( "org.apache.maven:maven-core" );
        artifacts.add( "org.apache.maven:maven-error-diagnostics" );
        artifacts.add( "org.apache.maven:maven-lifecycle" );
        artifacts.add( "org.apache.maven:maven-model" );
        artifacts.add( "org.apache.maven:maven-model-builder" );
        artifacts.add( "org.apache.maven:maven-monitor" );
        artifacts.add( "org.apache.maven:maven-plugin-api" );
        artifacts.add( "org.apache.maven:maven-plugin-descriptor" );
        artifacts.add( "org.apache.maven:maven-plugin-parameter-documenter" );
        artifacts.add( "org.apache.maven:maven-plugin-registry" );
        artifacts.add( "org.apache.maven:maven-profile" );
        artifacts.add( "org.apache.maven:maven-project" );
        artifacts.add( "org.apache.maven:maven-repository-metadata" );
        artifacts.add( "org.apache.maven:maven-settings" );
        artifacts.add( "org.apache.maven:maven-settings-builder" );
        artifacts.add( "org.apache.maven:maven-toolchain" );
        artifacts.add( "org.apache.maven.wagon:wagon-provider-api" );
        artifacts.add( "org.sonatype.aether:aether-api" );
        artifacts.add( "org.sonatype.aether:aether-spi" );
        artifacts.add( "org.sonatype.aether:aether-impl" );

        /*
         * NOTE: Don't exclude the wagons or any of their dependencies (apart from the wagon API). This would otherwise
         * provoke linkage errors for wagons contributed by build extensions. We also don't need to exclude the wagons
         * from plugins. Plugins that use wagons directly and declare the corresponding dependency will simply use a
         * wagon from their plugin realm.
         */

        DEFAULT_EXCLUSIONS = Collections.unmodifiableSet( artifacts);
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
     * @see org.apache.maven.ArtifactFilterManager#getExtensionDependencyFilter()
     */
    public ArtifactFilter getCoreArtifactFilter()
    {
        return new ExclusionSetFilter( getCoreArtifactExcludes() );
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

    public Set<String> getCoreArtifactExcludes()
    {
        Set<String> excludes = new LinkedHashSet<String>( DEFAULT_EXCLUSIONS );

        for ( ArtifactFilterManagerDelegate delegate : getDelegates() )
        {
            delegate.addCoreExcludes( excludes );
        }

        return excludes;
    }

}
