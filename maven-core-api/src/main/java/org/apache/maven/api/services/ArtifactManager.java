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
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import javax.annotation.Nonnull;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Optional;

import org.apache.maven.api.Artifact;
import org.apache.maven.api.Metadata;

public interface ArtifactManager extends Service
{

    /**
     * Returns the path to the resolved file in the local repository
     * if the artifact has been resolved.
     *
     * @return the path of the resolved artifact
     */
    @Nonnull
    Optional<Path> getPath( Artifact artifact );

    @Nonnull
    Collection<Metadata> getAttachedMetadatas( Artifact artifact );

    void attachMetadata( @Nonnull Artifact artifact, @Nonnull Metadata metadata );

    void setPath( Artifact artifact, Path path );
}
