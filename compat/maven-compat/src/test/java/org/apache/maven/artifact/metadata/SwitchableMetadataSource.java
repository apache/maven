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
package org.apache.maven.artifact.metadata;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.repository.legacy.metadata.MetadataResolutionRequest;
import org.eclipse.sisu.Priority;

@Singleton
@Priority(10)
@Named
@Deprecated
public class SwitchableMetadataSource implements ArtifactMetadataSource {
    private ArtifactMetadataSource delegate;

    @Inject
    public SwitchableMetadataSource(@Named("test") ArtifactMetadataSource delegate) {
        this.delegate = delegate;
    }

    public void setDelegate(ArtifactMetadataSource delegate) {
        this.delegate = delegate;
    }

    @Override
    public ResolutionGroup retrieve(MetadataResolutionRequest request) throws ArtifactMetadataRetrievalException {
        return delegate.retrieve(request);
    }

    @Override
    public ResolutionGroup retrieve(
            Artifact artifact, ArtifactRepository localRepository, List<ArtifactRepository> remoteRepositories)
            throws ArtifactMetadataRetrievalException {
        return delegate.retrieve(artifact, localRepository, remoteRepositories);
    }

    @Override
    public List<ArtifactVersion> retrieveAvailableVersions(MetadataResolutionRequest request)
            throws ArtifactMetadataRetrievalException {
        return delegate.retrieveAvailableVersions(request);
    }

    @Override
    public List<ArtifactVersion> retrieveAvailableVersions(
            Artifact artifact, ArtifactRepository localRepository, List<ArtifactRepository> remoteRepositories)
            throws ArtifactMetadataRetrievalException {
        return delegate.retrieveAvailableVersions(artifact, localRepository, remoteRepositories);
    }

    @Override
    public List<ArtifactVersion> retrieveAvailableVersionsFromDeploymentRepository(
            Artifact artifact, ArtifactRepository localRepository, ArtifactRepository remoteRepository)
            throws ArtifactMetadataRetrievalException {
        return delegate.retrieveAvailableVersionsFromDeploymentRepository(artifact, localRepository, remoteRepository);
    }
}
