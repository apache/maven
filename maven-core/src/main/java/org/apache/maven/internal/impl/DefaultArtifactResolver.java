package org.apache.maven.internal.impl;

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

import org.apache.maven.api.annotations.Nonnull;
import javax.inject.Inject;

import java.util.Objects;

import org.apache.maven.api.Artifact;
import org.apache.maven.api.services.ArtifactResolver;
import org.apache.maven.api.services.ArtifactResolverException;
import org.apache.maven.api.services.ArtifactResolverRequest;
import org.apache.maven.api.services.ArtifactResolverResult;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;

public class DefaultArtifactResolver implements ArtifactResolver
{
    private final RepositorySystem repositorySystem;

    @Inject
    DefaultArtifactResolver( @Nonnull RepositorySystem repositorySystem )
    {
        this.repositorySystem = Objects.requireNonNull( repositorySystem );
    }

    @Override
    public ArtifactResolverResult resolve( ArtifactResolverRequest request )
            throws ArtifactResolverException, IllegalArgumentException
    {
        DefaultSession session = ( DefaultSession ) request.getSession();
        try
        {
            ArtifactRequest req = new ArtifactRequest()
                    .setArtifact( session.toArtifact( request.getArtifact() ) )
                    .setRepositories( session.toRepositories( session.getRemoteRepositories() ) );
            ArtifactResult res = repositorySystem.resolveArtifact( session.getSession(), req );
            return new ArtifactResolverResult()
            {
                @Override
                public Artifact getArtifact()
                {
                    return session.getArtifact( res.getArtifact() );
                }
            };
        }
        catch ( ArtifactResolutionException e )
        {
            throw new ArtifactResolverException( "Unable to resolve artifact", e );
        }
    }

}
