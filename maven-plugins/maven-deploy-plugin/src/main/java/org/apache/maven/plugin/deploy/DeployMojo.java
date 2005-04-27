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
import org.apache.maven.plugin.AbstractPlugin;
import org.apache.maven.plugin.PluginExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.artifact.ProjectArtifactMetadata;

import java.io.File;

/**
 * @author <a href="mailto:evenisse@apache.org">Emmanuel Venisse</a>
 * @version $Id$
 * @goal deploy
 * @description deploys an artifact to remote repository
 * @parameter name="project"
 * type="org.apache.maven.project.MavenProject"
 * required="true"
 * validator=""
 * expression="${project}"
 * description=""
 * @parameter name="deployer"
 * type="org.apache.maven.artifact.deployer.ArtifactDeployer"
 * required="true"
 * validator=""
 * expression="${component.org.apache.maven.artifact.deployer.ArtifactDeployer}"
 * description=""
 * @parameter name="deploymentRepository"
 * type="org.apache.maven.artifact.repository.ArtifactRepository"
 * required="true"
 * validator=""
 * expression="${project.distributionManagementArtifactRepository}"
 * description=""
 * @parameter name="localRepository"
 * type="org.apache.maven.artifact.repository.ArtifactRepository"
 * required="true"
 * validator=""
 * expression="${localRepository}"
 * description=""
 */
public class DeployMojo
    extends AbstractPlugin
{
    private MavenProject project;

    private ArtifactDeployer deployer;

    private ArtifactRepository deploymentRepository;

    private ArtifactRepository localRepository;

    public void execute()
        throws PluginExecutionException
    {
        if ( deploymentRepository == null )
        {
            String msg = "Deployment failed: repository element was not specified in the pom inside" +
                " distributionManagement element";
            throw new PluginExecutionException( msg );
        }

        // Deploy the POM
        Artifact artifact = new DefaultArtifact( project.getGroupId(), project.getArtifactId(), project.getVersion(),
                                                 project.getPackaging() );
        boolean isPomArtifact = "pom".equals( project.getPackaging() );
        File pom = new File( project.getFile().getParentFile(), "pom.xml" );
        if ( !isPomArtifact )
        {
            ArtifactMetadata metadata = new ProjectArtifactMetadata( artifact, pom );
            artifact.addMetadata( metadata );
        }

        try
        {
            if ( !isPomArtifact )
            {
                deployer.deploy( project.getBuild().getDirectory(), project.getBuild().getFinalName(), artifact,
                                 deploymentRepository, localRepository );
            }
            else
            {
                deployer.deploy( pom, artifact, deploymentRepository, localRepository );
            }
        }
        catch ( ArtifactDeploymentException e )
        {
            // TODO: deployment exception that does not give a trace
            throw new PluginExecutionException( "Error deploying artifact", e );
        }
    }
}