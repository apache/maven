package org.apache.maven.artifact.resolver;

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

import java.util.Iterator;
import java.util.List;

import org.apache.maven.artifact.Artifact;

public class MultipleArtifactsNotFoundException
    extends ArtifactResolutionException
{

    public MultipleArtifactsNotFoundException( Artifact originatingArtifact, List artifacts, List remoteRepositories )
    {
        super( constructMessage( artifacts ), originatingArtifact, remoteRepositories );
    }

    private static String constructMessage( List artifacts )
    {
        StringBuffer buffer = new StringBuffer( "Missing:\n" );
        
        buffer.append( "----------\n" );

        int counter = 0;

        for ( Iterator i = artifacts.iterator(); i.hasNext(); )
        {
            Artifact artifact = (Artifact) i.next();
            String message = ( ++counter ) + ") " + artifact.getId();

            buffer.append( constructMissingArtifactMessage( message, "  ", artifact.getGroupId(), artifact
                .getArtifactId(), artifact.getVersion(), artifact.getType(), artifact.getDownloadUrl(), artifact
                .getDependencyTrail() ) );
        }

        buffer.append( "----------\n" );
        
        int size = artifacts.size();

        buffer.append( size ).append( " required artifact" );

        if ( size > 1 )
        {
            buffer.append( "s are" );
        }
        else
        {
            buffer.append( " is" );
        }

        buffer.append( " missing.\n\nfor artifact: " );
        
        return buffer.toString();
    }

}
