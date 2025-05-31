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
package org.apache.maven.repository.legacy.metadata;

import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.DefaultRepositoryRequest;
import org.apache.maven.artifact.repository.RepositoryRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;

/**
 * Forms a request to retrieve artifact metadata.
 *
 */
@Deprecated
public class DefaultMetadataResolutionRequest implements MetadataResolutionRequest {

    private Artifact artifact;

    private boolean resolveManagedVersions;

    private RepositoryRequest repositoryRequest;

    public DefaultMetadataResolutionRequest() {
        repositoryRequest = new DefaultRepositoryRequest();
    }

    public DefaultMetadataResolutionRequest(RepositoryRequest repositoryRequest) {
        this.repositoryRequest = new DefaultRepositoryRequest(repositoryRequest);
    }

    public DefaultMetadataResolutionRequest(ArtifactResolutionRequest resolutionRequest) {
        this.repositoryRequest = new DefaultRepositoryRequest(resolutionRequest);
    }

    @Override
    public Artifact getArtifact() {
        return artifact;
    }

    @Override
    public DefaultMetadataResolutionRequest setArtifact(Artifact artifact) {
        this.artifact = artifact;

        return this;
    }

    @Override
    public ArtifactRepository getLocalRepository() {
        return repositoryRequest.getLocalRepository();
    }

    @Override
    public DefaultMetadataResolutionRequest setLocalRepository(ArtifactRepository localRepository) {
        repositoryRequest.setLocalRepository(localRepository);

        return this;
    }

    @Override
    public List<ArtifactRepository> getRemoteRepositories() {
        return repositoryRequest.getRemoteRepositories();
    }

    @Override
    public DefaultMetadataResolutionRequest setRemoteRepositories(List<ArtifactRepository> remoteRepositories) {
        repositoryRequest.setRemoteRepositories(remoteRepositories);

        return this;
    }

    @Override
    public boolean isResolveManagedVersions() {
        return resolveManagedVersions;
    }

    @Override
    public DefaultMetadataResolutionRequest setResolveManagedVersions(boolean resolveManagedVersions) {
        this.resolveManagedVersions = resolveManagedVersions;

        return this;
    }

    @Override
    public boolean isOffline() {
        return repositoryRequest.isOffline();
    }

    @Override
    public DefaultMetadataResolutionRequest setOffline(boolean offline) {
        repositoryRequest.setOffline(offline);

        return this;
    }

    @Override
    public boolean isForceUpdate() {
        return repositoryRequest.isForceUpdate();
    }

    @Override
    public DefaultMetadataResolutionRequest setForceUpdate(boolean forceUpdate) {
        repositoryRequest.setForceUpdate(forceUpdate);

        return this;
    }
}
