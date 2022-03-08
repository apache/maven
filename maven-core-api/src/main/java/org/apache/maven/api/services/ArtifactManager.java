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
     * Returns the path of the file previously associated to this artifact
     * or <code>Optional.empty()</code> if no path has been associated.
     */
    @Nonnull
    Optional<Path> getPath( @Nonnull Artifact artifact );

    /**
     * Associates the given file path to the artifact.
     */
    void setPath( @Nonnull Artifact artifact, Path path );

    /**
     * TODO: investigate removing the Metadata api completely
     */
    @Nonnull
    Collection<Metadata> getAttachedMetadatas( @Nonnull Artifact artifact );

    /**
     * TODO: investigate removing the Metadata api completely
     */
    void attachMetadata( @Nonnull Artifact artifact, @Nonnull Metadata metadata );

    /**
     * Checks whether a given artifact version is considered a <code>SNAPSHOT</code> or not.
     */
    boolean isSnapshot( String version );

}
