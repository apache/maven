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
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.layout.ArtifactPathFormatException;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.IOException;

public class DefaultArtifactInstaller
    extends AbstractLogEnabled
    implements ArtifactInstaller
{
    private ArtifactHandlerManager artifactHandlerManager;

    public void install( String basedir, Artifact artifact, ArtifactRepository localRepository )
        throws ArtifactInstallationException
    {
        File source = null;

        try
        {
            source = artifactHandlerManager.getArtifactHandler( artifact.getType() ).source( basedir, artifact );
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
            artifact.setPath( artifactHandlerManager.getLocalRepositoryArtifactPath( artifact, localRepository ) );

            if ( !artifact.getFile().getParentFile().exists() )
            {
                artifact.getFile().getParentFile().mkdirs();
            }

            getLogger().info( "Installing " + source.getPath() + " to " + artifact.getPath() );

            FileUtils.copyFile( source, artifact.getFile() );
        }
        catch ( IOException e )
        {
            throw new ArtifactInstallationException( "Error installing artifact: ", e );
        }
        catch ( ArtifactPathFormatException e )
        {
            throw new ArtifactInstallationException( "Error installing artifact: ", e );
        }
    }
}