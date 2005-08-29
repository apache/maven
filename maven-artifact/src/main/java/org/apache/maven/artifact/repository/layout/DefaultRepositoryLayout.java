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
import org.apache.maven.artifact.repository.metadata.RepositoryMetadata;

/**
 * @author jdcasey
 */
public class DefaultRepositoryLayout
    implements ArtifactRepositoryLayout
{
    private static final char PATH_SEPARATOR = '/';

    private static final char GROUP_SEPARATOR = '.';

    private static final char ARTIFACT_SEPARATOR = '-';

    public String pathOf( Artifact artifact )
    {
        ArtifactHandler artifactHandler = artifact.getArtifactHandler();

        StringBuffer path = new StringBuffer();

        path.append( formatAsDirectory( artifact.getGroupId() ) ).append( PATH_SEPARATOR );
        path.append( artifact.getArtifactId() ).append( PATH_SEPARATOR );
        path.append( artifact.getBaseVersion() ).append( PATH_SEPARATOR );
        path.append( artifact.getArtifactId() ).append( ARTIFACT_SEPARATOR ).append( artifact.getVersion() );

        if ( artifact.hasClassifier() )
        {
            path.append( ARTIFACT_SEPARATOR ).append( artifact.getClassifier() );
        }

        if ( artifactHandler.getExtension() != null && artifactHandler.getExtension().length() > 0 )
        {
            path.append( GROUP_SEPARATOR ).append( artifactHandler.getExtension() );
        }

        return path.toString();
    }

    public String pathOfArtifactMetadata( ArtifactMetadata metadata )
    {
        StringBuffer path = new StringBuffer();

        path.append( formatAsDirectory( metadata.getGroupId() ) ).append( PATH_SEPARATOR );
        path.append( metadata.getArtifactId() ).append( PATH_SEPARATOR );
        if ( metadata.storedInArtifactDirectory() )
        {
            path.append( metadata.getBaseVersion() ).append( PATH_SEPARATOR );
        }

        path.append( metadata.getFilename() );

        return path.toString();
    }

    public String pathOfRepositoryMetadata( RepositoryMetadata metadata )
    {
        String file = metadata.getRepositoryPath();

        String result;
        int lastSlash = file.lastIndexOf( PATH_SEPARATOR );

        if ( lastSlash > -1 )
        {
            String filePart = file.substring( lastSlash );

            String dirPart = file.substring( 0, lastSlash );

            result = formatAsDirectory( dirPart ) + filePart;
        }
        else
        {
            result = file;
        }
        return result;
    }

    private String formatAsDirectory( String directory )
    {
        return directory.replace( GROUP_SEPARATOR, PATH_SEPARATOR );
    }

}