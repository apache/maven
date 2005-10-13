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

    public ArtifactResolutionException( String message, String groupId, String artifactId, String version, String type,
                                        Throwable t )
    {
        super( message, groupId, artifactId, version, type, null, null, t );
    }

    public ArtifactResolutionException( String message, Artifact artifact )
    {
        super( message, artifact );
    }

    public ArtifactResolutionException( String message, Artifact artifact, List remoteRepositories )
    {
        super( message, artifact, remoteRepositories );
    }

    public ArtifactResolutionException( String message, Artifact artifact, Throwable t )
    {
        super( message, artifact, null, t );
    }

    protected ArtifactResolutionException( String message, Artifact artifact, List remoteRepositories, Throwable t )
    {
        super( message, artifact, remoteRepositories, t );
    }
}
