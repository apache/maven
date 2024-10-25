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
package org.apache.maven.repository.legacy.resolver.transform;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.RepositoryRequest;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadataResolutionException;
import org.apache.maven.artifact.repository.metadata.Versioning;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;

/**
 * Describes a version transformation during artifact resolution - "latest" type
 */
@Named("latest")
@Singleton
@Deprecated
public class LatestArtifactTransformation extends AbstractVersionTransformation {

    public void transformForResolve(Artifact artifact, RepositoryRequest request)
            throws ArtifactResolutionException, ArtifactNotFoundException {
        if (Artifact.LATEST_VERSION.equals(artifact.getVersion())) {
            try {
                String version = resolveVersion(artifact, request);
                if (Artifact.LATEST_VERSION.equals(version)) {
                    throw new ArtifactNotFoundException("Unable to determine the latest version", artifact);
                }

                artifact.setBaseVersion(version);
                artifact.updateVersion(version, request.getLocalRepository());
            } catch (RepositoryMetadataResolutionException e) {
                throw new ArtifactResolutionException(e.getMessage(), artifact, e);
            }
        }
    }

    public void transformForInstall(Artifact artifact, ArtifactRepository localRepository) {
        // metadata is added via addPluginArtifactMetadata
    }

    public void transformForDeployment(
            Artifact artifact, ArtifactRepository remoteRepository, ArtifactRepository localRepository) {
        // metadata is added via addPluginArtifactMetadata
    }

    protected String constructVersion(Versioning versioning, String baseVersion) {
        return versioning.getLatest();
    }
}
