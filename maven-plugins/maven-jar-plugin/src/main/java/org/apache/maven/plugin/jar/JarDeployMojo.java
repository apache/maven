package org.apache.maven.plugin.jar;

/*
 * Copyright 2001-2004 The Apache Software Foundation.
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
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.DistributionManagement;
import org.apache.maven.model.Repository;
import org.apache.maven.plugin.AbstractPlugin;
import org.apache.maven.plugin.PluginExecutionRequest;
import org.apache.maven.plugin.PluginExecutionResponse;
import org.apache.maven.project.MavenProject;

/**
 * @goal deploy
 * @description deploys a JAR to remote repository
 * @parameter name="project" type="org.apache.maven.project.MavenProject"
 *            required="true" validator="" expression="#project" description=""
 * @parameter name="deployer"
 *            type="org.apache.maven.artifact.deployer.ArtifactDeployer"
 *            required="true" validator=""
 *            expression="#component.org.apache.maven.artifact.deployer.ArtifactDeployer"
 *            description=""
 */
public class JarDeployMojo
    extends AbstractPlugin
{
    public void execute( PluginExecutionRequest request, PluginExecutionResponse response ) throws Exception
    {
        MavenProject project = (MavenProject) request.getParameter( "project" );

        ArtifactDeployer artifactDeployer = (ArtifactDeployer) request.getParameter( "deployer" );

        //@todo this will be duplicated in case of every mojo which implements
        // deploy goal
        // this should be pushed into the ArtifactDeployer component
        DistributionManagement distributionManagement = project.getDistributionManagement();

        if ( distributionManagement == null )
        {
            String msg = "Deployment failed: distributionManagement element" + " was not specified in the pom";
            throw new Exception( msg );
        }

        Repository repository = distributionManagement.getRepository();

        if ( repository == null )
        {
            String msg = "Deployment failed: repository element" + " was not specified in the pom inside"
                + " distributionManagement element";
            throw new Exception( msg );
        }

        ArtifactRepository deploymentRepository = new ArtifactRepository( repository.getId(), repository.getUrl() );

        Artifact artifact = new DefaultArtifact( project.getGroupId(),
                                                 project.getArtifactId(),
                                                 project.getVersion(),
                                                 project.getType() );

        artifactDeployer.deploy( project.getBuild().getDirectory(), artifact, deploymentRepository );
    }
}