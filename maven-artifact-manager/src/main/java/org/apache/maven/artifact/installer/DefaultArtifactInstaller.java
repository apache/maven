package org.apache.maven.artifact.installer;

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
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadataInstallationException;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadataManager;
import org.apache.maven.artifact.transform.ArtifactTransformationManager;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

public class DefaultArtifactInstaller
    extends AbstractLogEnabled
    implements ArtifactInstaller
{
    private ArtifactTransformationManager transformationManager;

    private RepositoryMetadataManager repositoryMetadataManager;

    /**
     * @deprecated we want to use the artifact method only, and ensure artifact.file is set correctly.
     */
    public void install( String basedir, String finalName, Artifact artifact, ArtifactRepository localRepository )
        throws ArtifactInstallationException
    {
        String extension = artifact.getArtifactHandler().getExtension();
        File source = new File( basedir, finalName + "." + extension );

        install( source, artifact, localRepository );
    }

    public void install( File source, Artifact artifact, ArtifactRepository localRepository )
        throws ArtifactInstallationException
    {
        
        // If we're installing the POM, we need to transform it first. The source file supplied for 
        // installation here may be the POM, but that POM may not be set as the file of the supplied
        // artifact. Since the transformation only has access to the artifact and not the supplied
        // source file, we have to use the Artifact.setFile(..) and Artifact.getFile(..) methods
        // to shunt the POM file into the transformation process.
        // Here, we also set a flag indicating that the POM has been shunted through the Artifact,
        // and to expect the transformed version to be available in the Artifact afterwards...
        boolean useArtifactFile = false;
        File oldArtifactFile = artifact.getFile();
        if ( "pom".equals( artifact.getType() ) )
        {
            artifact.setFile( source );
            useArtifactFile = true;
        }
        
        try
        {
            transformationManager.transformForInstall( artifact, localRepository );
            
            // If we used the Artifact shunt to transform a POM source file, we need to install
            // the transformed version, not the supplied version. Therefore, we need to replace
            // the supplied source POM with the one from Artifact.getFile(..).
            if ( useArtifactFile )
            {
                source = artifact.getFile();
                artifact.setFile( oldArtifactFile );
            }

            String localPath = localRepository.pathOf( artifact );

            // TODO: use a file: wagon and the wagon manager?
            File destination = new File( localRepository.getBasedir(), localPath );
            if ( !destination.getParentFile().exists() )
            {
                destination.getParentFile().mkdirs();
            }

            getLogger().info( "Installing " + source.getPath() + " to " + destination );

            FileUtils.copyFile( source, destination );
            
            // Now, we'll set the artifact's file to the one installed in the local repository,
            // to help avoid duplicate copy operations in the deployment step.
            if ( useArtifactFile )
            {
                artifact.setFile( destination );
            }

            // must be after the artifact is installed
            for ( Iterator i = artifact.getMetadataList().iterator(); i.hasNext(); )
            {
                ArtifactMetadata metadata = (ArtifactMetadata) i.next();
                repositoryMetadataManager.install( metadata, localRepository );
            }
            // TODO: would like to flush this, but the plugin metadata is added in advance, not as an install/deploy transformation
            // This would avoid the need to merge and clear out the state during deployment
//            artifact.getMetadataList().clear();
        }
        catch ( IOException e )
        {
            throw new ArtifactInstallationException( "Error installing artifact: " + e.getMessage(), e );
        }
        catch ( RepositoryMetadataInstallationException e )
        {
            throw new ArtifactInstallationException( "Error installing artifact's metadata: " + e.getMessage(), e );
        }
    }
}