package org.apache.maven.artifact.resolver;

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

import java.util.List;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 */
public class ArtifactResolutionException
    extends AbstractArtifactResolutionException
{
    public ArtifactResolutionException( String message, String groupId, String artifactId, String version, String type,
                                        List remoteRepositories, List path, Throwable t )
    {
        super( message, groupId, artifactId, version, type, remoteRepositories, path, t );
    }

    public ArtifactResolutionException( String message, Artifact artifact )
    {
        super( message, artifact );
    }

    protected ArtifactResolutionException( String message, Artifact artifact, List remoteRepositories )
    {
        super( message, artifact, remoteRepositories );
    }

    protected ArtifactResolutionException( String message, Artifact artifact, List remoteRepositories, Throwable t )
    {
        super( message, artifact, remoteRepositories, t );
    }

/*
    public ArtifactResolutionException( String message, String groupId, String artifactId, String version, String type,
                                        List remoteRepositories, String downloadUrl, List path, Throwable t )
    {
        super( constructMessage( message, groupId, artifactId, version, type, remoteRepositories, downloadUrl, path ),
               t );

        this.originalMessage = message;
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.type = type;
        this.version = version;
        this.remoteRepositories = remoteRepositories;
        this.path = constructArtifactPath( path );
        this.downloadUrl = downloadUrl;
    }

    public ArtifactResolutionException( String message, String groupId, String artifactId, String version, String type,
                                        List remoteRepositories, String downloadUrl, Throwable t )
    {
        this( message, groupId, artifactId, version, type, remoteRepositories, downloadUrl, null, t );
    }

    public ArtifactResolutionException( String message, String groupId, String artifactId, String version, String type,
                                        List remoteRepositories, String downloadUrl, List path )
    {
        super( constructMessage( message, groupId, artifactId, version, type, remoteRepositories, downloadUrl, path ) );

        this.originalMessage = message;
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.type = type;
        this.version = version;
        this.remoteRepositories = remoteRepositories;
        this.downloadUrl = downloadUrl;
        this.path = constructArtifactPath( path );
    }

    public ArtifactResolutionException( String message, Artifact artifact, List remoteRepositories, Throwable t )
    {
        this( message, artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion(), artifact.getType(),
              remoteRepositories, artifact.getDownloadUrl(), artifact.getDependencyTrail(), t );
    }

    public ArtifactResolutionException( String message, Artifact artifact, List remoteRepositories )
    {
        this( message, artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion(), artifact.getType(),
              remoteRepositories, artifact.getDownloadUrl(), artifact.getDependencyTrail() );
    }

    public ArtifactResolutionException( String message, Artifact artifact )
    {
        this( message, artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion(), artifact.getType(), null,
              artifact.getDownloadUrl(), artifact.getDependencyTrail() );
    }

    public ArtifactResolutionException( String message, Throwable cause )
    {
        super( message, cause );

        this.originalMessage = message;
        this.path = "";
    }

*/
}
