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

import java.io.File;
import java.io.IOException;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadataInstallationException;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadataManager;
import org.apache.maven.repository.DefaultLocalRepositoryMaintainerEvent;
import org.apache.maven.repository.LocalRepositoryMaintainer;
import org.apache.maven.repository.LocalRepositoryMaintainerEvent;
import org.apache.maven.repository.legacy.resolver.transform.ArtifactTransformationManager;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.util.FileUtils;

/**
 * @author Jason van Zyl
 */
@Component(role=ArtifactInstaller.class)
public class DefaultArtifactInstaller
    extends AbstractLogEnabled
    implements ArtifactInstaller
{
    @Requirement
    private ArtifactTransformationManager transformationManager;

    @Requirement
    private RepositoryMetadataManager repositoryMetadataManager;

    @Requirement( optional = true )
    private LocalRepositoryMaintainer localRepositoryMaintainer;

    /** @deprecated we want to use the artifact method only, and ensure artifact.file is set correctly. */
    @Deprecated
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
        try
        {
            transformationManager.transformForInstall( artifact, localRepository );

            String localPath = localRepository.pathOf( artifact );

            // TODO: use a file: wagon and the wagon manager?
            File destination = new File( localRepository.getBasedir(), localPath );
            if ( !destination.getParentFile().exists() )
            {
                destination.getParentFile().mkdirs();
            }

            getLogger().info( "Installing " + source.getPath() + " to " + destination );

            FileUtils.copyFileIfModified( source, destination );

            // must be after the artifact is installed
            for ( ArtifactMetadata metadata : artifact.getMetadataList() )
            {
                repositoryMetadataManager.install( metadata, localRepository );
            }
            // TODO: would like to flush this, but the plugin metadata is added in advance, not as an install/deploy
            // transformation
            // This would avoid the need to merge and clear out the state during deployment
            // artifact.getMetadataList().clear();

            if ( localRepositoryMaintainer != null )
            {
                LocalRepositoryMaintainerEvent event =
                    new DefaultLocalRepositoryMaintainerEvent( localRepository, artifact, destination );
                localRepositoryMaintainer.artifactInstalled( event );
            }
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