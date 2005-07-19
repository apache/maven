package org.apache.maven.plugin.plugin.metadata;

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

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadataManagementException;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadataManager;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.mapping.metadata.PluginMappingMetadata;
import org.apache.maven.project.MavenProject;

import java.io.File;

/**
 * @goal deployMapping
 * @phase deploy
 */
public class PluginMappingDeployMojo
    extends AbstractMojo
{

    /**
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * @parameter expression="${component.org.apache.maven.artifact.repository.metadata.RepositoryMetadataManager}"
     * @required
     * @readonly
     */
    private RepositoryMetadataManager repositoryMetadataManager;

    /**
     * @parameter expression="${project.build.directory}/repository-metadata"
     * @required
     * @readonly
     */
    private String outputDirectory;

    public void execute()
        throws MojoExecutionException
    {
        ArtifactRepository distributionRepository = project.getDistributionManagementArtifactRepository();
        
        RepositoryMetadata metadata = new PluginMappingMetadata( project.getGroupId() );

        File updatedMetadataFile = new File( outputDirectory, metadata.getRepositoryPath() ).getAbsoluteFile();
        
        if ( !updatedMetadataFile.exists() )
        {
            throw new MojoExecutionException( "Cannot find updated " + metadata + " in file: \'" + updatedMetadataFile + "\'. This seems to indicate that the 'package' lifecycle phase didn't succeed." );
        }
        
        metadata.setFile( updatedMetadataFile );
        
        try
        {
            repositoryMetadataManager.deploy( metadata, distributionRepository );
        }
        catch ( RepositoryMetadataManagementException e )
        {
            throw new MojoExecutionException( "Error updating plugin-mapping metadata.", e );
        }
    }

}
