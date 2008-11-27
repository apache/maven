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
import org.apache.maven.artifact.metadata.ArtifactMetadataRetrievalException;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadataResolutionException;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.wagon.TransferFailedException;

import java.io.IOException;
import java.util.List;

/**
 * @author Jason van Zyl
 * @version $Id$
 */
public class ArtifactResolutionException
    extends AbstractArtifactResolutionException
{
    public ArtifactResolutionException( String message,
                                        String groupId,
                                        String artifactId,
                                        String version,
                                        String type,
                                        String classifier,
                                        List<ArtifactRepository> remoteRepositories,
                                        List path,
                                        Throwable t )
    {
        super( message, groupId, artifactId, version, type, classifier, remoteRepositories, path, t );
    }

    public ArtifactResolutionException( String message,
                                        String groupId,
                                        String artifactId,
                                        String version,
                                        String type,
                                        String classifier,
                                        Throwable t )
    {
        super( message, groupId, artifactId, version, type, classifier, null, null, t );
    }

    public ArtifactResolutionException( String message,
                                        Artifact artifact )
    {
        super( message, artifact );
    }

    public ArtifactResolutionException( String message,
                                        Artifact artifact,
                                        List<ArtifactRepository> remoteRepositories )
    {
        super( message, artifact, remoteRepositories );
    }

    public ArtifactResolutionException( String message,
                                        Artifact artifact,
                                        ArtifactMetadataRetrievalException cause )
    {
        super( message, artifact, null, cause );
    }

    @Deprecated
    public ArtifactResolutionException( String message, Artifact artifact, Throwable cause )
    {
        super( message, artifact, null, cause );
    }

    protected ArtifactResolutionException( String message,
                                           Artifact artifact,
                                           List<ArtifactRepository> remoteRepositories,
                                           ArtifactMetadataRetrievalException cause )
    {
        super( message, artifact, remoteRepositories, cause );
    }

    @Deprecated
    protected ArtifactResolutionException( String message, Artifact artifact,
                                           List<ArtifactRepository> remoteRepositories, Throwable cause )
    {
        super( message, artifact, remoteRepositories, cause );
    }

    protected ArtifactResolutionException( String message,
                                           Artifact artifact,
                                           List<ArtifactRepository> remoteRepositories,
                                           TransferFailedException cause )
    {
        super( message, artifact, remoteRepositories, cause );
    }

    protected ArtifactResolutionException( String message,
                                           Artifact artifact,
                                           List<ArtifactRepository> remoteRepositories,
                                           IOException cause )
    {
        super( message, artifact, remoteRepositories, cause );
    }

    public ArtifactResolutionException( String message,
                                        Artifact artifact,
                                        RepositoryMetadataResolutionException cause )
    {
        super( message, artifact, null, cause );
    }
}
