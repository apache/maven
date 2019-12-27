package org.apache.maven.plugin.coreit;

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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.deployer.ArtifactDeployer;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryFactory;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import java.io.File;

/**
 * Deploys a user-supplied file to some repository. This mimics part of the Maven Deploy Plugin.
 * 
 * @goal deploy-file
 * @requiresProject false
 * 
 * @author Benjamin Bentmann
 *
 */
public class DeployFileMojo
    extends AbstractMojo
{

    /**
     * The file of the artifact to deploy.
     * 
     * @parameter property="file"
     */
    private File file;

    /**
     * The group id of the artifact.
     * 
     * @parameter property="groupId"
     */
    private String groupId;

    /**
     * The artifact id of the artifact.
     * 
     * @parameter property="artifactId"
     */
    private String artifactId;

    /**
     * The version of the artifact.
     * 
     * @parameter property="version"
     */
    private String version;

    /**
     * The URL of the repository to deploy to.
     * 
     * @parameter property="repositoryUrl"
     */
    private String repositoryUrl;

    /**
     * The ID of the repository to deploy to.
     * 
     * @parameter property="repositoryId"
     */
    private String repositoryId;

    /**
     * The repository factory.
     * 
     * @component
     */
    private ArtifactRepositoryFactory repositoryFactory;

    /**
     * The repository layout.
     * 
     * @component roleHint="default"
     */
    private ArtifactRepositoryLayout repositoryLayout;

    /**
     * The artifact factory.
     * 
     * @component
     */
    private ArtifactFactory artifactFactory;

    /**
     * The artifact deployer.
     * 
     * @component
     */
    private ArtifactDeployer deployer;

    /**
     * The local repository.
     * 
     * @parameter default-value="${localRepository}"
     * @readonly
     * @required
     */
    private ArtifactRepository localRepository;

    /**
     * Runs this mojo.
     * 
     * @throws MojoExecutionException If any artifact could not be deployed.
     */
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        getLog().info( "[MAVEN-CORE-IT-LOG] Deploying artifacts" );

        try
        {
            ArtifactRepository repository =
                repositoryFactory.createDeploymentArtifactRepository( repositoryId, repositoryUrl, repositoryLayout,
                                                                      true );

            Artifact artifact = artifactFactory.createArtifact( groupId, artifactId, version, null, "jar" );

            deployer.deploy( file, artifact, repository, localRepository );
        }
        catch ( Exception e )
        {
            throw new MojoExecutionException( "Failed to deploy artifacts: " + e.getMessage(), e );
        }
    }

}
