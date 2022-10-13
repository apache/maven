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
package org.apache.maven.api.services;

import java.util.Collection;
import java.util.Collections;

import org.apache.maven.api.Artifact;
import org.apache.maven.api.Session;
import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Immutable;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.annotations.NotThreadSafe;
import org.apache.maven.api.annotations.Nullable;

import static org.apache.maven.api.services.BaseRequest.nonNull;

/**
 * A request for installing one or more artifacts in the local repository.
 *
 * @since 4.0
 */
@Experimental
@Immutable
public interface ArtifactInstallerRequest
{

    @Nonnull
    Session getSession();

    @Nonnull
    Collection<Artifact> getArtifacts();

    @Nonnull
    static ArtifactInstallerRequestBuilder builder()
    {
        return new ArtifactInstallerRequestBuilder();
    }

    @Nonnull
    static ArtifactInstallerRequest build( Session session, Collection<Artifact> artifacts )
    {
        return builder().session( nonNull( session,
                                           "session cannot be null" ) ).artifacts( nonNull( artifacts,
                                                                                            "artifacts cannot be null" ) ).build();
    }

    @NotThreadSafe
    class ArtifactInstallerRequestBuilder
    {
        Session session;

        Collection<Artifact> artifacts = Collections.emptyList();

        ArtifactInstallerRequestBuilder()
        {
        }

        @Nonnull
        public ArtifactInstallerRequestBuilder session( @Nonnull
        Session session )
        {
            this.session = session;
            return this;
        }

        @Nonnull
        public ArtifactInstallerRequestBuilder artifacts( @Nullable
        Collection<Artifact> artifacts )
        {
            this.artifacts = artifacts != null ? artifacts : Collections.emptyList();
            return this;
        }

        @Nonnull
        public ArtifactInstallerRequest build()
        {
            return new DefaultArtifactInstallerRequest( session, artifacts );
        }

        static class DefaultArtifactInstallerRequest
            extends BaseRequest
            implements ArtifactInstallerRequest
        {

            private final Collection<Artifact> artifacts;

            DefaultArtifactInstallerRequest( @Nonnull
            Session session, @Nonnull
            Collection<Artifact> artifacts )
            {
                super( session );
                this.artifacts = unmodifiable( nonNull( artifacts, "artifacts cannot be null" ) );
            }

            @Nonnull
            @Override
            public Collection<Artifact> getArtifacts()
            {
                return artifacts;
            }
        }
    }

}
