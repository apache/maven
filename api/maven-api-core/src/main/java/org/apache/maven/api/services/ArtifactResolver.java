package org.apache.maven.api.services;

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

import org.apache.maven.api.Session;
import org.apache.maven.api.Artifact;

/**
 * Resolves the artifact, i.e download the file when required and attach it to the artifact
 */
public interface ArtifactResolver extends Service
{

    /**
     * @param request {@link ArtifactResolverRequest}
     * @return {@link ArtifactResolverResult}
     * @throws ArtifactResolverException in case of an error.
     * @throws IllegalArgumentException in case of parameter {@code buildingRequest} is {@code null} or
     *             parameter {@code mavenArtifact} is {@code null} or invalid.
     */
    ArtifactResolverResult resolve( ArtifactResolverRequest request );

    /**
     * @param session {@link Session}
     * @param artifact {@link Artifact}
     * @return {@link ArtifactResolverResult}
     * @throws ArtifactResolverException in case of an error.
     * @throws IllegalArgumentException in case of parameter {@code buildingRequest} is {@code null} or
     *             parameter {@code coordinate} is {@code null} or invalid.
     */
    default ArtifactResolverResult resolve( Session session,
                                            Artifact artifact )
    {
        return resolve( ArtifactResolverRequest.build( session, artifact ) );
    }

}
