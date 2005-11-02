package org.apache.maven.plugins.mavenone;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.deployer.ArtifactDeployer;
import org.apache.maven.artifact.deployer.ArtifactDeploymentException;
import org.apache.maven.artifact.installer.ArtifactInstallationException;
import org.apache.maven.artifact.installer.ArtifactInstaller;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryFactory;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.IOUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

/**
 * Install the artifact in a maven one local repository
 *
 * @goal deploy-maven-one-repository
 * @phase deploy
 */
public class MavenOneRepositoryDeployMojo
    extends AbstractMojo
{
    /**
     * @parameter expression="${project.packaging}"
     * @required
     * @readonly
     */
    protected String packaging;

    /**
     * @parameter expression="${project.file}"
     * @required
     * @readonly
     */
    private File pomFile;

    /**
     * @parameter expression="${project.artifact}"
     * @required
     * @readonly
     */
    private Artifact artifact;

    /**
     * @component
     */
    protected ArtifactDeployer deployer;

    /**
     * @component
     */
    protected ArtifactRepositoryFactory factory;

    /**
     * @parameter expression="${remoteRepositoryId}" default-value="mavenOneRemoteRepository"
     * @required
     */
    protected String remoteRepositoryId;

    /**
     * @parameter expression="${remoteRepositoryUrl}"
     * @required
     */
    protected String remoteRepositoryUrl;

    /**
     * @component roleHint="legacy"
     */
    private ArtifactRepositoryLayout legacyLayout;

    /**
     * @parameter expression="${localRepository}"
     * @required
     * @readonly
     */
    private ArtifactRepository localRepository;

    public void execute()
        throws MojoExecutionException
    {
        try
        {
            ArtifactRepository deploymentRepository = factory.createDeploymentArtifactRepository( remoteRepositoryId,
                                                                                                  remoteRepositoryUrl,
                                                                                                  legacyLayout, false );

            boolean isPomArtifact = "pom".equals( packaging );

            if ( isPomArtifact )
            {
                deployer.deploy( pomFile, artifact, deploymentRepository, localRepository );
            }
            else
            {
                File file = artifact.getFile();
                if ( file == null )
                {
                    throw new MojoExecutionException(
                        "The packaging for this project did not assign a file to the build artifact" );
                }
                deployer.deploy( file, artifact, deploymentRepository, localRepository );
            }

        }
        catch ( ArtifactDeploymentException e )
        {
            throw new MojoExecutionException( e.getMessage(), e );
        }
    }
}
