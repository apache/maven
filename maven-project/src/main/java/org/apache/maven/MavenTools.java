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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.InvalidRepositoryException;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.model.DeploymentRepository;
import org.apache.maven.model.Model;
import org.apache.maven.model.Repository;
import org.apache.maven.project.ProjectBuildingException;

import java.io.IOException;
import java.util.List;

/**
 * @author Jason van Zyl
 */
public interface MavenTools
{
    // ----------------------------------------------------------------------------
    // Methods taken from ProjectUtils
    // ----------------------------------------------------------------------------

    List buildArtifactRepositories( List repositories )
        throws InvalidRepositoryException;

    ArtifactRepository buildDeploymentArtifactRepository( DeploymentRepository repo )
        throws InvalidRepositoryException;

    ArtifactRepository buildArtifactRepository( Repository repo )
        throws InvalidRepositoryException;
    
    ArtifactRepository createLocalRepository( String url, String repositoryId )
        throws IOException;   

    ArtifactRepository createRepository( String url, String repositoryId );
    
    ArtifactRepository createRepository( String url, String repositoryId, ArtifactRepositoryPolicy snapshotsPolicy, ArtifactRepositoryPolicy releasesPolicy );
    
    void setGlobalUpdatePolicy( String policy );
    
    void setGlobalChecksumPolicy( String policy );
    
    // Taken from RepositoryHelper
    
    void findModelFromRepository( Artifact artifact, List remoteArtifactRepositories, ArtifactRepository localRepository )
        throws ProjectBuildingException;

    List buildArtifactRepositories( Model model )        
        throws ProjectBuildingException;
    
}
