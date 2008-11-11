package org.apache.maven.project.builder;

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
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Provides methods for resolving of artifacts.
 */
public class PomArtifactResolver
{

    /**
     * Local repository used in resolving artifacts
     */
    private ArtifactRepository localRepository;

    /**
     * Remote repositories used in resolving artifacts
     */
    private List<ArtifactRepository> remoteRepositories;

    /**
     * Artifact resolver used to resolve artifacts
     */
    private ArtifactResolver resolver;

    /**
     * Constructor
     *
     * @param localRepository    local repository used in resolving artifacts
     * @param remoteRepositories remote repositories used in resolving artifacts
     * @param resolver           artifact resolver used to resolve artifacts
     */
    public PomArtifactResolver( ArtifactRepository localRepository, List<ArtifactRepository> remoteRepositories,
                                ArtifactResolver resolver )
    {
        this.localRepository = localRepository;
        this.remoteRepositories = remoteRepositories;
        this.resolver = resolver;
    }

    /**
     * Resolves the specified artifact
     *
     * @param artifact the artifact to resolve
     * @throws IOException if there is a problem resolving the artifact
     */
    public void resolve( Artifact artifact )
        throws IOException
    {
        File artifactFile = new File( localRepository.getBasedir(), localRepository.pathOf( artifact ) );
        artifact.setFile( artifactFile );

        try
        {
            resolver.resolve( artifact, remoteRepositories, localRepository );
        }
        catch ( ArtifactResolutionException e )
        {
            throw new IOException( e.getMessage() );
        }
        catch ( ArtifactNotFoundException e )
        {
            throw new IOException( e.getMessage() );
        }
    }
}
