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
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.profiles.ProfileManager;
import org.apache.maven.project.artifact.ProjectArtifact;
import org.apache.maven.repository.RepositorySystem;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.util.StringUtils;

/**
 * @version $Id$
 */
@Component(role = MavenProjectBuilder.class)
public class DefaultMavenProjectBuilder
    implements MavenProjectBuilder
{

    @Requirement
    private ProjectBuilder projectBuilder;

    @Requirement
    private RepositorySystem repositorySystem;

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
        MavenProject project = projectBuilder.build( pomFile, configuration );

        Artifact artifact = new ProjectArtifact( project );                     
        
        ArtifactResolutionRequest request = new ArtifactResolutionRequest()
            .setArtifact( artifact )
            .setResolveRoot( false )
            .setResolveTransitively( true )
            .setLocalRepository( configuration.getLocalRepository() )
            .setRemoteRepostories( project.getRemoteArtifactRepositories() )
            .setManagedVersionMap( project.getManagedVersionMap() );

        ArtifactResolutionResult result = repositorySystem.resolve( request );

        if ( result.hasExceptions() )
        {
            Exception e = result.getExceptions().get( 0 );

            throw new ProjectBuildingException( safeVersionlessKey( project.getGroupId(), project.getArtifactId() ), "Unable to build project due to an invalid dependency version: " + e.getMessage(),
                                                pomFile, e );
        }

        project.setArtifacts( result.getArtifacts() );
        
        return new MavenProjectBuildingResult( project, result );
    }

    private static String safeVersionlessKey( String groupId, String artifactId )
    {
        String gid = groupId;

        if ( StringUtils.isEmpty( gid ) )
        {
            gid = "unknown";
        }

        String aid = artifactId;

        if ( StringUtils.isEmpty( aid ) )
        {
            aid = "unknown";
        }

        return ArtifactUtils.versionlessKey( gid, aid );
    }

}