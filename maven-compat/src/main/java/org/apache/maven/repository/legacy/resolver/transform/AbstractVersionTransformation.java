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

import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.DefaultRepositoryRequest;
import org.apache.maven.artifact.repository.RepositoryRequest;
import org.apache.maven.artifact.repository.metadata.ArtifactRepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadataManager;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadataResolutionException;
import org.apache.maven.artifact.repository.metadata.SnapshotArtifactRepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.Versioning;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.repository.legacy.WagonManager;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.AbstractLogEnabled;

/**
 * Describes a version transformation during artifact resolution.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * TODO try and refactor to remove abstract methods - not particular happy about current design
 */
public abstract class AbstractVersionTransformation extends AbstractLogEnabled implements ArtifactTransformation {
    @Requirement
    protected RepositoryMetadataManager repositoryMetadataManager;

    @Requirement
    protected WagonManager wagonManager;

    public void transformForResolve(
            Artifact artifact, List<ArtifactRepository> remoteRepositories, ArtifactRepository localRepository)
            throws ArtifactResolutionException, ArtifactNotFoundException {
        RepositoryRequest request = new DefaultRepositoryRequest();
        request.setLocalRepository(localRepository);
        request.setRemoteRepositories(remoteRepositories);
        transformForResolve(artifact, request);
    }

    protected String resolveVersion(
            Artifact artifact, ArtifactRepository localRepository, List<ArtifactRepository> remoteRepositories)
            throws RepositoryMetadataResolutionException {
        RepositoryRequest request = new DefaultRepositoryRequest();
        request.setLocalRepository(localRepository);
        request.setRemoteRepositories(remoteRepositories);
        return resolveVersion(artifact, request);
    }

    protected String resolveVersion(Artifact artifact, RepositoryRequest request)
            throws RepositoryMetadataResolutionException {
        RepositoryMetadata metadata;
        // Don't use snapshot metadata for LATEST (which isSnapshot returns true for)
        if (!artifact.isSnapshot() || Artifact.LATEST_VERSION.equals(artifact.getBaseVersion())) {
            metadata = new ArtifactRepositoryMetadata(artifact);
        } else {
            metadata = new SnapshotArtifactRepositoryMetadata(artifact);
        }

        repositoryMetadataManager.resolve(metadata, request);

        artifact.addMetadata(metadata);

        Metadata repoMetadata = metadata.getMetadata();
        String version = null;
        if (repoMetadata != null && repoMetadata.getVersioning() != null) {
            version = constructVersion(repoMetadata.getVersioning(), artifact.getBaseVersion());
        }

        if (version == null) {
            // use the local copy, or if it doesn't exist - go to the remote repo for it
            version = artifact.getBaseVersion();
        }

        // TODO also do this logging for other metadata?
        // TODO figure out way to avoid duplicated message
        if (getLogger().isDebugEnabled()) {
            if (!version.equals(artifact.getBaseVersion())) {
                String message = artifact.getArtifactId() + ": resolved to version " + version;
                if (artifact.getRepository() != null) {
                    message += " from repository " + artifact.getRepository().getId();
                } else {
                    message += " from local repository";
                }
                getLogger().debug(message);
            } else {
                // Locally installed file is newer, don't use the resolved version
                getLogger().debug(artifact.getArtifactId() + ": using locally installed snapshot");
            }
        }
        return version;
    }

    protected abstract String constructVersion(Versioning versioning, String baseVersion);
}
