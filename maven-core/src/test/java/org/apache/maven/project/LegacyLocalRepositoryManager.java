package org.apache.maven.project;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.File;

import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.impl.internal.SimpleLocalRepositoryManager;
import org.sonatype.aether.repository.LocalArtifactRequest;
import org.sonatype.aether.repository.LocalArtifactResult;

/**
 * @author Benjamin Bentmann
 */
public class LegacyLocalRepositoryManager
    extends SimpleLocalRepositoryManager
{

    public LegacyLocalRepositoryManager( File basedir )
    {
        super( basedir );
    }

    public String getPathForLocalArtifact( Artifact artifact )
    {
        StringBuilder path = new StringBuilder( 128 );

        path.append( artifact.getGroupId() ).append( '/' );

        path.append( artifact.getExtension() ).append( 's' ).append( '/' );

        path.append( artifact.getArtifactId() ).append( '-' ).append( artifact.getVersion() );

        if ( artifact.getClassifier().length() > 0 )
        {
            path.append( '-' ).append( artifact.getClassifier() );
        }

        path.append( '.' ).append( artifact.getExtension() );

        return path.toString();
    }

    public LocalArtifactResult find( RepositorySystemSession session, LocalArtifactRequest request )
    {
 	    String path = getPathForLocalArtifact( request.getArtifact() );
 	    File file = new File( getRepository().getBasedir(), path );
        LocalArtifactResult result = new LocalArtifactResult( request );
 	    if ( file.isFile() )
        {
 	        result.setFile( file );
 	        result.setAvailable( true );
 	    }
 	    return result;
 	}

}
