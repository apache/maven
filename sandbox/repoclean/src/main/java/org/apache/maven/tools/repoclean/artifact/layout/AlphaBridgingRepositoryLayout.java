package org.apache.maven.tools.repoclean.artifact.layout;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.repository.layout.ArtifactPathFormatException;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;

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

public class AlphaBridgingRepositoryLayout extends DefaultRepositoryLayout
{

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
