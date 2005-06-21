package org.apache.maven.artifact.repository.layout;

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
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.metadata.ArtifactMetadata;

/**
 * @author jdcasey
 */
public class DefaultRepositoryLayout
    implements ArtifactRepositoryLayout
{

    public String pathOf( Artifact artifact )
    {
        ArtifactHandler artifactHandler = artifact.getArtifactHandler();

        StringBuffer path = new StringBuffer();

        path.append( artifact.getGroupId().replace( '.', '/' ) ).append( '/' );
        path.append( artifact.getArtifactId() ).append( '/' );
        path.append( artifact.getBaseVersion() ).append( '/' );
        path.append( artifact.getArtifactId() ).append( '-' ).append( artifact.getVersion() );

        if ( artifact.hasClassifier() )
        {
            path.append( '-' ).append( artifact.getClassifier() );
        }

        if ( artifactHandler.getExtension() != null && artifactHandler.getExtension().length() > 0 )
        {
            path.append( '.' ).append( artifactHandler.getExtension() );
        }

        return path.toString();
    }

    public String pathOfMetadata( ArtifactMetadata metadata )
    {
        StringBuffer path = new StringBuffer();

        path.append( metadata.getGroupId().replace( '.', '/' ) ).append( '/' );
        path.append( metadata.getArtifactId() ).append( '/' );
        if ( !metadata.getBaseVersion().equals( "RELEASE" ) )
        {
            path.append( metadata.getBaseVersion() ).append( '/' );
        }

        path.append( metadata.getFilename() );

        return path.toString();
    }

}