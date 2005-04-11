package org.apache.maven.artifact.repository.layout;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerNotFoundException;
import org.apache.maven.artifact.metadata.ArtifactMetadata;

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

/**
 * @author jdcasey
 */
public class DefaultRepositoryLayout
    implements ArtifactRepositoryLayout
{

    private ArtifactHandlerManager artifactHandlerManager;

    public String pathOf( Artifact artifact )
        throws ArtifactPathFormatException
    {
        ArtifactHandler artifactHandler = null;
        try
        {
            // TODO: this is a poor excuse to have this method throwing an exception. Validate the artifact first, perhaps associate the handler with it
            artifactHandler = artifactHandlerManager.getArtifactHandler( artifact.getType() );
        }
        catch ( ArtifactHandlerNotFoundException e )
        {
            throw new ArtifactPathFormatException( "Cannot find ArtifactHandler for artifact: \'" + artifact.getId() +
                                                   "\'.", e );
        }

        StringBuffer path = new StringBuffer();

        path.append( artifact.getGroupId().replace( '.', '/' ) ).append( '/' );
        path.append( artifact.getArtifactId() ).append( '/' );
        path.append( artifact.getBaseVersion() ).append( '/' );
        path.append( artifact.getArtifactId() ).append( '-' ).append( artifact.getVersion() );

        if ( artifact.hasClassifier() )
        {
            path.append( '-' ).append( artifact.getClassifier() );
        }

        if ( artifactHandler.extension() != null && artifactHandler.extension().length() > 0 )
        {
            path.append( '.' ).append( artifactHandler.extension() );
        }

        return path.toString();
    }

    public String pathOfMetadata( ArtifactMetadata metadata )
        throws ArtifactPathFormatException
    {
        Artifact artifact = metadata.getArtifact();

        StringBuffer path = new StringBuffer();

        path.append( artifact.getGroupId().replace( '.', '/' ) ).append( '/' );
        path.append( artifact.getArtifactId() ).append( '/' );
        path.append( artifact.getBaseVersion() ).append( '/' );
        path.append( metadata.getFilename() );

        return path.toString();
    }

}