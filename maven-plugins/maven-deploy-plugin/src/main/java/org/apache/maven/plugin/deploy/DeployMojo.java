package org.apache.maven.plugin.deploy;

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
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.deployer.ArtifactDeployer;
import org.apache.maven.artifact.deployer.ArtifactDeploymentException;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.artifact.ProjectArtifactMetadata;

import java.io.File;

/**
 * @author <a href="mailto:evenisse@apache.org">Emmanuel Venisse</a>
 * @author <a href="mailto:jdcasey@apache.org">John Casey (refactoring only)</a>
 * @version $Id$
 * @goal deploy
 * @description deploys an artifact to remote repository
 */
public class DeployMojo
    extends AbstractMojo
{

    /**
     * @parameter expression="${project.groupId}"
     * @required
     * @readonly
     */
    private String groupId;

    /**
     * @parameter expression="${project.artifactId}"
     * @required
     * @readonly
     */
    private String artifactId;

    /**
     * @parameter expression="${project.version}"
     * @required
     * @readonly
     */
    private String version;

    /**
     * @parameter expression="${project.packaging}"
     * @required
     * @readonly
     */
    private String packaging;

    /**
     * @parameter expression="${project.file.parentFile}"
     * @required
     * @readonly
     */
    private File parentDir;

    /**
     * @parameter expression="${project.build.directory}"
     * @required
     * @readonly
     */
    private String buildDirectory;

    /**
     * @parameter alias="archiveName" expression="${project.build.finalName}"
     * @required
     */
    private String finalName;

    /**
     * @parameter expression="${component.org.apache.maven.artifact.deployer.ArtifactDeployer}"
     * @required
     * @readonly
     */
    private ArtifactDeployer deployer;

    /**
     * @parameter expression="${project.distributionManagementArtifactRepository}"
     * @required
     * @readonly
     */
    private ArtifactRepository deploymentRepository;

    /**
     * @parameter expression="${localRepository}"
     * @required
     * @readonly
     */
    private ArtifactRepository localRepository;

    public void execute()
        throws MojoExecutionException
    {
        if ( deploymentRepository == null )
        {
            String msg = "Deployment failed: repository element was not specified in the pom inside"
                + " distributionManagement element";
            throw new MojoExecutionException( msg );
        }

        // Deploy the POM
        Artifact artifact = new DefaultArtifact( groupId, artifactId, version, packaging );
        boolean isPomArtifact = "pom".equals( packaging );
        File pom = new File( parentDir, "pom.xml" );
        if ( !isPomArtifact )
        {
            ArtifactMetadata metadata = new ProjectArtifactMetadata( artifact, pom );
            artifact.addMetadata( metadata );
        }

        try
        {
            if ( !isPomArtifact )
            {
                deployer.deploy( buildDirectory, finalName, artifact, deploymentRepository, localRepository );
            }
            else
            {
                deployer.deploy( pom, artifact, deploymentRepository, localRepository );
            }
        }
        catch ( ArtifactDeploymentException e )
        {
            // TODO: deployment exception that does not give a trace
            throw new MojoExecutionException( "Error deploying artifact", e );
        }
    }
}