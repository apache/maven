package org.apache.maven.api;

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

import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Immutable;
import org.apache.maven.api.annotations.Nonnull;

/**
 * An artifact points to a resource such as a jar file or war application.
 *
 * @since 4.0
 */
@Experimental
@Immutable
public interface Artifact
{

    /**
     * Returns a unique identifier for this artifact.
     * The identifier is composed of groupId, artifactId, version, classifier, extension.
     *
     * @return the unique identifier
     */
    default String key()
    {
        return getGroupId()
                + ':' + getArtifactId()
                + ':' + getExtension()
                + ( getClassifier().length() > 0 ? ":" + getClassifier() : "" )
                + ':' + getVersion();
    }

    /**
     * The groupId of the artifact.
     *
     * @return the groupId
     */
    @Nonnull
    String getGroupId();

    /**
     * The artifactId of the artifact.
     *
     * @return the artifactId
     */
    @Nonnull
    String getArtifactId();

    /**
     * The version of the artifact.
     *
     * @return the version
     */
    @Nonnull
    Version getVersion();

    /**
     * The classifier of the artifact.
     *
     * @return the classifier or an empty string if none, never {@code null}
     */
    @Nonnull
    String getClassifier();

    /**
     * The file extension of the artifact.
     *
     * @return the extension
     */
    @Nonnull
    String getExtension();

    /**
     * Determines whether this artifact uses a snapshot version.
     *
     * @return {@code true} if the artifact is a snapshot, {@code false} otherwise
     * @see org.apache.maven.api.Session#isVersionSnapshot(String)
     */
    boolean isSnapshot();

    /**
     * Shortcut for {@code session.createArtifactCoordinate(artifact)}
     *
     * @return an {@link ArtifactCoordinate}
     * @see org.apache.maven.api.Session#createArtifactCoordinate(Artifact)
     */
    @Nonnull
    ArtifactCoordinate toCoordinate();

}
