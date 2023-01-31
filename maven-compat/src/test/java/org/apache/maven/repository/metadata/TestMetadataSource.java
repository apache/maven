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
package org.apache.maven.repository.metadata;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.repository.legacy.metadata.ArtifactMetadataRetrievalException;
import org.apache.maven.repository.legacy.metadata.ArtifactMetadataSource;
import org.apache.maven.repository.legacy.metadata.MetadataResolutionRequest;
import org.apache.maven.repository.legacy.metadata.ResolutionGroup;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;

@Component(role = ArtifactMetadataSource.class)
public class TestMetadataSource implements ArtifactMetadataSource {
    @Requirement
    private ArtifactFactory factory;

    public ResolutionGroup retrieve(
            Artifact artifact, ArtifactRepository localRepository, List<ArtifactRepository> remoteRepositories)
            throws ArtifactMetadataRetrievalException {
        Set<Artifact> dependencies = new HashSet<>();

        if ("g".equals(artifact.getArtifactId())) {
            Artifact a = null;
            try {
                a = factory.createBuildArtifact("org.apache.maven", "h", "1.0", "jar");
                dependencies.add(a);
            } catch (Exception e) {
                throw new ArtifactMetadataRetrievalException("Error retrieving metadata", e, a);
            }
        }

        if ("i".equals(artifact.getArtifactId())) {
            Artifact a = null;
            try {
                a = factory.createBuildArtifact("org.apache.maven", "j", "1.0-SNAPSHOT", "jar");
                dependencies.add(a);
            } catch (Exception e) {
                throw new ArtifactMetadataRetrievalException("Error retrieving metadata", e, a);
            }
        }

        return new ResolutionGroup(artifact, dependencies, remoteRepositories);
    }

    public List<ArtifactVersion> retrieveAvailableVersions(
            Artifact artifact, ArtifactRepository localRepository, List<ArtifactRepository> remoteRepositories)
            throws ArtifactMetadataRetrievalException {
        throw new UnsupportedOperationException("Cannot get available versions in this test case");
    }

    public List<ArtifactVersion> retrieveAvailableVersionsFromDeploymentRepository(
            Artifact artifact, ArtifactRepository localRepository, ArtifactRepository remoteRepository)
            throws ArtifactMetadataRetrievalException {
        throw new UnsupportedOperationException("Cannot get available versions in this test case");
    }

    public ResolutionGroup retrieve(MetadataResolutionRequest request) throws ArtifactMetadataRetrievalException {
        return retrieve(request.getArtifact(), request.getLocalRepository(), request.getRemoteRepositories());
    }
}
