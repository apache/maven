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

import org.apache.maven.api.Artifact;
import org.apache.maven.api.Service;
import org.apache.maven.api.Session;
import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Nonnull;

/**
 * Service used to create {@link Artifact} objects.
 *
 * @since 4.0
 */
@Experimental
public interface ArtifactFactory
    extends Service
{

    /**
     * Creates an artifact.
     *
     * @param request the request holding artifact creation parameters
     * @return an {@code Artifact}, never {@code null}
     * @throws IllegalArgumentException if {@code request} is null or {@code request.session} is null or invalid
     */
    @Nonnull
    Artifact create( @Nonnull
    ArtifactFactoryRequest request );

    @Nonnull
    default Artifact create( @Nonnull
    Session session, String groupId, String artifactId, String version, String extension )
    {
        return create( ArtifactFactoryRequest.build( session, groupId, artifactId, version, extension ) );
    }

    @Nonnull
    default Artifact create( @Nonnull
    Session session, String groupId, String artifactId, String version, String classifier, String extension,
                             String type )
    {
        return create( ArtifactFactoryRequest.build( session, groupId, artifactId, version, classifier, extension,
                                                     type ) );
    }
}
