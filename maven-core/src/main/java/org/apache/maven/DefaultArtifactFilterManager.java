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

import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ExclusionSetFilter;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Jason van Zyl
 * @version $Id$
 * @todo this should probably be a component with some dynamic control of filtering
 */
public class DefaultArtifactFilterManager implements ArtifactFilterManager
{

    private static final Set DEFAULT_EXCLUSIONS;

    static
    {
        Set artifacts = new HashSet();

        artifacts.add( "classworlds" );
        artifacts.add( "plexus-classworlds" );
        artifacts.add( "commons-cli" );
        artifacts.add( "doxia-sink-api" );
        artifacts.add( "jsch" );
        artifacts.add( "maven-artifact" );
        artifacts.add( "maven-artifact-manager" );
        artifacts.add( "maven-build-context" );
        artifacts.add( "maven-core" );
        artifacts.add( "maven-error-diagnoser" );
        artifacts.add( "maven-lifecycle" );
        artifacts.add( "maven-model" );
        artifacts.add( "maven-monitor" );
        artifacts.add( "maven-plugin-api" );
        artifacts.add( "maven-plugin-descriptor" );
        artifacts.add( "maven-plugin-parameter-documenter" );
        artifacts.add( "maven-profile" );
        artifacts.add( "maven-project" );
        artifacts.add( "maven-reporting-api" );
        artifacts.add( "maven-repository-metadata" );
        artifacts.add( "maven-settings" );
        //adding shared/maven-toolchain project here, even though not part of the default
        //distro yet.
        artifacts.add( "maven-toolchain" );
        artifacts.add( "plexus-component-api" );
        artifacts.add( "plexus-container-default" );
        artifacts.add( "plexus-interactivity-api" );
        artifacts.add( "wagon-provider-api" );
        artifacts.add( "wagon-file" );
        artifacts.add( "wagon-http-lightweight" );
        artifacts.add( "wagon-manager" );
        artifacts.add( "wagon-ssh" );
        artifacts.add( "wagon-ssh-external" );

        DEFAULT_EXCLUSIONS = artifacts;
    }

    private Set excludedArtifacts = new HashSet( DEFAULT_EXCLUSIONS );

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
        return new ExclusionSetFilter( excludedArtifacts );
    }

    /**
     * Returns the artifact filter for the standard core artifacts.
     *
     * @see org.apache.maven.ArtifactFilterManager#getExtensionArtifactFilter()
     */
    public ArtifactFilter getCoreArtifactFilter()
    {
        return new ExclusionSetFilter( DEFAULT_EXCLUSIONS );
    }

    /* (non-Javadoc)
     * @see org.apache.maven.ArtifactFilterManager#excludeArtifact(java.lang.String)
     */
    public void excludeArtifact( String artifactId )
    {
        excludedArtifacts.add( artifactId );
    }

}
