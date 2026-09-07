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
package org.apache.maven.api;

import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Immutable;
import org.apache.maven.api.annotations.Nonnull;

/**
 * Metadata involved in a repository event.
 *
 * @since 4.1.0
 */
@Experimental
@Immutable
public interface RepositoryMetadata {

    /**
     * Describes which artifact versions a metadata item applies to.
     */
    enum Nature {
        /** Metadata that applies only to release versions. */
        RELEASE,

        /** Metadata that applies only to snapshot versions. */
        SNAPSHOT,

        /** Metadata that applies to both release and snapshot versions. */
        RELEASE_OR_SNAPSHOT,
    }

    /**
     * {@return the group identifier, or an empty string if the metadata applies to the entire repository}
     */
    @Nonnull
    String groupId();

    /**
     * {@return the artifact identifier, or an empty string if the metadata applies at group level}
     */
    @Nonnull
    String artifactId();

    /**
     * {@return the version, or an empty string if the metadata applies at artifact level}
     */
    @Nonnull
    String version();

    /**
     * {@return the metadata filename, such as {@code maven-metadata.xml}}
     */
    @Nonnull
    String type();

    /**
     * {@return the artifact version nature to which the metadata applies}
     */
    @Nonnull
    Nature nature();

    /**
     * {@return the local path of the metadata file if it has been resolved}
     */
    @Nonnull
    Optional<Path> path();

    /**
     * {@return the read-only properties associated with the metadata}
     */
    @Nonnull
    Map<String, String> properties();
}
