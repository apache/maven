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
package org.apache.maven.artifact.resolver;

import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;

/**
 * @author Jason van Zyl
 */
public class ArtifactResolutionException extends AbstractArtifactResolutionException {
    @SuppressWarnings("checkstyle:parameternumber")
    public ArtifactResolutionException(
            String message,
            String groupId,
            String artifactId,
            String version,
            String type,
            String classifier,
            List<ArtifactRepository> remoteRepositories,
            List<String> path,
            Throwable t) {
        super(message, groupId, artifactId, version, type, classifier, remoteRepositories, path, t);
    }

    public ArtifactResolutionException(
            String message,
            String groupId,
            String artifactId,
            String version,
            String type,
            String classifier,
            Throwable t) {
        super(message, groupId, artifactId, version, type, classifier, null, null, t);
    }

    public ArtifactResolutionException(String message, Artifact artifact) {
        super(message, artifact);
    }

    public ArtifactResolutionException(String message, Artifact artifact, List<ArtifactRepository> remoteRepositories) {
        super(message, artifact, remoteRepositories);
    }

    public ArtifactResolutionException(String message, Artifact artifact, Throwable cause) {
        super(message, artifact, null, cause);
    }

    public ArtifactResolutionException(
            String message, Artifact artifact, List<ArtifactRepository> remoteRepositories, Throwable cause) {
        super(message, artifact, remoteRepositories, cause);
    }
}
