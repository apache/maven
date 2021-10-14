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

import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;

/**
 * @author Jason van Zyl
 */
public class ArtifactResolutionException
    extends AbstractArtifactResolutionException
{
    @SuppressWarnings( "checkstyle:parameternumber" )
    public ArtifactResolutionException( final String message, final String groupId, final String artifactId,
                                        final String version, final String type,
                                        final String classifier, final List<ArtifactRepository> remoteRepositories,
                                        final List<String> path, final Throwable t )
    {
        super( message, groupId, artifactId, version, type, classifier, remoteRepositories, path, t );
    }

    public ArtifactResolutionException( final String message, final String groupId, final String artifactId,
                                        final String version, final String type,
                                        final String classifier, final Throwable t )
    {
        super( message, groupId, artifactId, version, type, classifier, null, null, t );
    }

    public ArtifactResolutionException( final String message, final Artifact artifact )
    {
        super( message, artifact );
    }

    public ArtifactResolutionException( final String message, final Artifact artifact,
                                        final List<ArtifactRepository> remoteRepositories )
    {
        super( message, artifact, remoteRepositories );
    }

    public ArtifactResolutionException( final String message, final Artifact artifact, final Throwable cause )
    {
        super( message, artifact, null, cause );
    }

    public ArtifactResolutionException( final String message, final Artifact artifact,
                                        final List<ArtifactRepository> remoteRepositories,
                                        final Throwable cause )
    {
        super( message, artifact, remoteRepositories, cause );
    }

}
