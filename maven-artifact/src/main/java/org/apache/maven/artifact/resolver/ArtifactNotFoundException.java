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

import org.apache.maven.artifact.Artifact;

import java.util.List;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 */
public class ArtifactNotFoundException
    extends AbstractArtifactResolutionException
{
    private String downloadUrl;
    
    protected ArtifactNotFoundException( String message, Artifact artifact, List remoteRepositories )
    {
        super( message, artifact, remoteRepositories );
    }
    
    public ArtifactNotFoundException( String message, Artifact artifact )
    {
        this( message, artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion(), artifact.getType(), null,
              artifact.getDownloadUrl(), artifact.getDependencyTrail() );
    }

    protected ArtifactNotFoundException( String message, Artifact artifact, List remoteRepositories, Throwable t )
    {
        this( message, artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion(), artifact.getType(),
              remoteRepositories, artifact.getDownloadUrl(), artifact.getDependencyTrail(), t );
    }

    public ArtifactNotFoundException( String message, String groupId, String artifactId, String version, String type,
                                      List remoteRepositories, String downloadUrl, List path, Throwable t )
    {
        super( constructMissingArtifactMessage( message, "", groupId, artifactId, version, type, downloadUrl, path ), groupId, artifactId,
               version, type, remoteRepositories, null, t );

        this.downloadUrl = downloadUrl;
    }

    private ArtifactNotFoundException( String message, String groupId, String artifactId, String version, String type,
                                       List remoteRepositories, String downloadUrl, List path )
    {
        super( constructMissingArtifactMessage( message, "", groupId, artifactId, version, type, downloadUrl, path ), groupId, artifactId,
               version, type, remoteRepositories, null );

        this.downloadUrl = downloadUrl;
    }

    public String getDownloadUrl()
    {
        return downloadUrl;
    }

}
