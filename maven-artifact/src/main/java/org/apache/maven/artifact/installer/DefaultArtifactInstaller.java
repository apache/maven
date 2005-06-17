package org.apache.maven.artifact.installer;

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
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerNotFoundException;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.metadata.ArtifactMetadataRetrievalException;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.layout.ArtifactPathFormatException;
import org.apache.maven.artifact.transform.ArtifactTransformation;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

public class DefaultArtifactInstaller
    extends AbstractLogEnabled
    implements ArtifactInstaller
{
    private ArtifactHandlerManager artifactHandlerManager;

    private List artifactTransformations;

    public void install( String basedir, String finalName, Artifact artifact, ArtifactRepository localRepository )
        throws ArtifactInstallationException
    {
        File source = null;

        try
        {
            String extension = artifactHandlerManager.getArtifactHandler( artifact.getType() ).getExtension();
            source = new File( basedir, finalName + "." + extension );
        }
        catch ( ArtifactHandlerNotFoundException e )
        {
            throw new ArtifactInstallationException( "Error installing artifact: ", e );
        }

        install( source, artifact, localRepository );
    }

    public void install( File source, Artifact artifact, ArtifactRepository localRepository )
        throws ArtifactInstallationException
    {
        try
        {
            // TODO: better to have a transform manager, or reuse the handler manager again so we don't have these requirements duplicated all over?
            for ( Iterator i = artifactTransformations.iterator(); i.hasNext(); )
            {
                ArtifactTransformation transform = (ArtifactTransformation) i.next();
                transform.transformForInstall( artifact, localRepository );
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

            // must be after the artifact is installed
            for ( Iterator i = artifact.getMetadataList().iterator(); i.hasNext(); )
            {
                ArtifactMetadata metadata = (ArtifactMetadata) i.next();
                metadata.storeInLocalRepository( localRepository );
            }
        }
        catch ( IOException e )
        {
            throw new ArtifactInstallationException( "Error installing artifact: ", e );
        }
        catch ( ArtifactPathFormatException e )
        {
            throw new ArtifactInstallationException( "Error installing artifact: ", e );
        }
        catch ( ArtifactMetadataRetrievalException e )
        {
            throw new ArtifactInstallationException( "Error installing artifact: ", e );
        }
    }
}