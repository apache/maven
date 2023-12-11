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

import org.apache.maven.api.ArtifactCoordinate;
import org.apache.maven.api.Service;
import org.apache.maven.api.Session;
import org.apache.maven.api.annotations.Consumer;
import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Nonnull;

/**
 * Resolves artifact meta/pseudo versions.
 */
@Experimental
@Consumer
public interface VersionResolver extends Service {

    /**
     * Resolves an artifact's meta version (if any) to a concrete version. For example, resolves "1.0-SNAPSHOT"
     * to "1.0-20090208.132618-23" or "RELEASE"/"LATEST" to "2.0".
     *
     * @param session The repository session, must not be {@code null}.
     * @param artifactCoordinate The artifact coordinate for which the version needs to be resolved, must not be {@code null}
     * @return The version result, never {@code null}.
     * @throws VersionResolverException If the metaversion could not be resolved.
     */
    @Nonnull
    default VersionResolverResult resolve(@Nonnull Session session, @Nonnull ArtifactCoordinate artifactCoordinate)
            throws VersionResolverException {
        return resolve(VersionResolverRequest.build(session, artifactCoordinate));
    }

    /**
     * Resolves an artifact's meta version (if any) to a concrete version. For example, resolves "1.0-SNAPSHOT"
     * to "1.0-20090208.132618-23" or "RELEASE"/"LATEST" to "2.0".
     *
     * @param request The version request, must not be {@code null}.
     * @return The version result, never {@code null}.
     * @throws VersionResolverException If the metaversion could not be resolved.
     */
    @Nonnull
    VersionResolverResult resolve(@Nonnull VersionResolverRequest request) throws VersionResolverException;
}
