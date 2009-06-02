package org.apache.maven.project;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

import java.io.File;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.profiles.ProfileManager;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;

/**
 * @version $Id$
 */
@Component(role = MavenProjectBuilder.class)
public class DefaultMavenProjectBuilder
    implements MavenProjectBuilder
{

    @Requirement
    private ProjectBuilder projectBuilder;

    // ----------------------------------------------------------------------
    // MavenProjectBuilder Implementation
    // ----------------------------------------------------------------------

    public MavenProject build( File pomFile, ProjectBuilderConfiguration configuration )
        throws ProjectBuildingException
    {
        return projectBuilder.build( pomFile, configuration );
    }

    public MavenProject buildFromRepository( Artifact artifact, ProjectBuilderConfiguration configuration )
        throws ProjectBuildingException
    {
        return projectBuilder.build( artifact, configuration );
    }

    // This is used by the SITE plugin.
    public MavenProject build( File project, ArtifactRepository localRepository, ProfileManager profileManager )
        throws ProjectBuildingException
    {
        ProjectBuilderConfiguration configuration = new DefaultProjectBuilderConfiguration()
            .setLocalRepository( localRepository );

        return build( project, configuration );
    }

    public MavenProject buildFromRepository( Artifact artifact, List<ArtifactRepository> remoteRepositories, ArtifactRepository localRepository, boolean force )
        throws ProjectBuildingException
    {
        return buildFromRepository( artifact, remoteRepositories, localRepository );        
    }

    public MavenProject buildFromRepository( Artifact artifact, List<ArtifactRepository> remoteRepositories, ArtifactRepository localRepository )
        throws ProjectBuildingException
    {
        ProjectBuilderConfiguration configuration = new DefaultProjectBuilderConfiguration()
            .setLocalRepository( localRepository )
            .setRemoteRepositories( remoteRepositories );
        configuration.setProcessPlugins( false );
        configuration.setLenientValidation( true );
        
        return buildFromRepository( artifact, configuration );
    }

    /**
     * This is used for pom-less execution like running archetype:generate.
     * 
     * I am taking out the profile handling and the interpolation of the base directory until we
     * spec this out properly.
     */
    public MavenProject buildStandaloneSuperProject( ProjectBuilderConfiguration config )
        throws ProjectBuildingException
    {
        return projectBuilder.buildStandaloneSuperProject( config );
    }

    public MavenProjectBuildingResult buildProjectWithDependencies( File pomFile, ProjectBuilderConfiguration configuration )
        throws ProjectBuildingException
    {
        return projectBuilder.buildProjectWithDependencies( pomFile, configuration );
    }

}