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

import java.util.Set;
import java.util.HashSet;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 * @todo this should probably be a component with some dynamic control of filtering
 */
public class MavenArtifactFilterManager
{
    public static ArtifactFilter createStandardFilter()
    {
        // TODO: configure this from bootstrap or scan lib
        Set artifacts = new HashSet();

        artifacts.add( "classworlds" );
        artifacts.add( "commons-cli" );
        artifacts.add( "doxia-sink-api" );
        artifacts.add( "jsch" );
        artifacts.add( "maven-artifact" );
        artifacts.add( "maven-artifact-manager" );
        artifacts.add( "maven-core" );
        artifacts.add( "maven-error-diagnoser" );
        artifacts.add( "maven-model" );
        artifacts.add( "maven-monitor" );
        artifacts.add( "maven-plugin-api" );
        artifacts.add( "maven-plugin-descriptor" );
        artifacts.add( "maven-plugin-parameter-documenter" );
        artifacts.add( "maven-plugin-registry" );
        artifacts.add( "maven-profile" );
        artifacts.add( "maven-project" );
        artifacts.add( "maven-reporting-api" );
        artifacts.add( "maven-repository-metadata" );
        artifacts.add( "maven-settings" );
        artifacts.add( "plexus-container-default" );
        artifacts.add( "plexus-interactivity-api" );
        //artifacts.add( "plexus-utils" );
        artifacts.add( "wagon-provider-api" );
        artifacts.add( "wagon-file" );
        artifacts.add( "wagon-http-lightweight" );
        artifacts.add( "wagon-ssh" );
        artifacts.add( "wagon-ssh-external" );

        return new ExclusionSetFilter( artifacts );
    }
}
