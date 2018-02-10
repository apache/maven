package org.apache.maven.repository;

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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.repository.ArtifactRepository;

/**
 * UserLocalArtifactRepository
 */
public class UserLocalArtifactRepository
    extends LocalArtifactRepository
{
    private ArtifactRepository localRepository;

    public UserLocalArtifactRepository( ArtifactRepository localRepository )
    {
        this.localRepository = localRepository;
        setLayout( localRepository.getLayout() );
    }

    @Override
    public Artifact find( Artifact artifact )
    {
        File artifactFile = new File( localRepository.getBasedir(), pathOf( artifact ) );

        // We need to set the file here or the resolver will fail with an NPE, not fully equipped to deal
        // with multiple local repository implementations yet.
        artifact.setFile( artifactFile );

        return artifact;
    }

    @Override
    public String getId()
    {
        return localRepository.getId();
    }

    @Override
    public String pathOfLocalRepositoryMetadata( ArtifactMetadata metadata, ArtifactRepository repository )
    {
        return localRepository.pathOfLocalRepositoryMetadata( metadata, repository );
    }

    @Override
    public String pathOf( Artifact artifact )
    {
        return localRepository.pathOf( artifact );
    }

    @Override
    public boolean hasLocalMetadata()
    {
        return true;
    }
}
