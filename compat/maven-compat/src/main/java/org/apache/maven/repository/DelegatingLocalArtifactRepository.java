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
package org.apache.maven.repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.repository.MavenArtifactRepository;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;

/**
 * Delegating local artifact repository chains the reactor, IDE workspace
 * and user local repository.
 */
@Deprecated
public class DelegatingLocalArtifactRepository extends MavenArtifactRepository {
    private LocalArtifactRepository buildReactor;

    private LocalArtifactRepository ideWorkspace;

    private ArtifactRepository userLocalArtifactRepository;

    public DelegatingLocalArtifactRepository(ArtifactRepository artifactRepository) {
        this.userLocalArtifactRepository = artifactRepository;
    }

    public void setBuildReactor(LocalArtifactRepository localRepository) {
        this.buildReactor = localRepository;
    }

    public void setIdeWorkspace(LocalArtifactRepository localRepository) {
        this.ideWorkspace = localRepository;
    }

    /**
     * @deprecated instead use {@link #getIdeWorkspace()}
     */
    @Deprecated
    public LocalArtifactRepository getIdeWorspace() {
        return ideWorkspace;
    }

    public LocalArtifactRepository getIdeWorkspace() {
        return getIdeWorspace();
    }

    @Override
    public Artifact find(Artifact artifact) {
        if (!artifact.isRelease() && buildReactor != null) {
            artifact = buildReactor.find(artifact);
        }

        if (!artifact.isResolved() && ideWorkspace != null) {
            artifact = ideWorkspace.find(artifact);
        }

        if (!artifact.isResolved()) {
            artifact = userLocalArtifactRepository.find(artifact);
        }

        return artifact;
    }

    @Override
    public List<String> findVersions(Artifact artifact) {
        Collection<String> versions = new LinkedHashSet<>();

        if (buildReactor != null) {
            versions.addAll(buildReactor.findVersions(artifact));
        }

        if (ideWorkspace != null) {
            versions.addAll(ideWorkspace.findVersions(artifact));
        }

        versions.addAll(userLocalArtifactRepository.findVersions(artifact));

        return Collections.unmodifiableList(new ArrayList<>(versions));
    }

    public String pathOfLocalRepositoryMetadata(ArtifactMetadata metadata, ArtifactRepository repository) {
        return userLocalArtifactRepository.pathOfLocalRepositoryMetadata(metadata, repository);
    }

    public String getId() {
        return userLocalArtifactRepository.getId();
    }

    @Override
    public String pathOf(Artifact artifact) {
        return userLocalArtifactRepository.pathOf(artifact);
    }

    @Override
    public String getBasedir() {
        return (userLocalArtifactRepository != null) ? userLocalArtifactRepository.getBasedir() : null;
    }

    @Override
    public ArtifactRepositoryLayout getLayout() {
        return userLocalArtifactRepository.getLayout();
    }

    @Override
    public ArtifactRepositoryPolicy getReleases() {
        return userLocalArtifactRepository.getReleases();
    }

    @Override
    public ArtifactRepositoryPolicy getSnapshots() {
        return userLocalArtifactRepository.getSnapshots();
    }

    @Override
    public String getKey() {
        return userLocalArtifactRepository.getKey();
    }

    @Override
    public String getUrl() {
        return userLocalArtifactRepository.getUrl();
    }

    @Override
    public int hashCode() {
        int hash = 17;
        hash = hash * 31 + (buildReactor == null ? 0 : buildReactor.hashCode());
        hash = hash * 31 + (ideWorkspace == null ? 0 : ideWorkspace.hashCode());
        hash = hash * 31 + (userLocalArtifactRepository == null ? 0 : userLocalArtifactRepository.hashCode());

        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }

        DelegatingLocalArtifactRepository other = (DelegatingLocalArtifactRepository) obj;

        return eq(buildReactor, other.buildReactor)
                && eq(ideWorkspace, other.ideWorkspace)
                && eq(userLocalArtifactRepository, other.userLocalArtifactRepository);
    }
}
