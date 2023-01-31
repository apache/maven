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

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;

/**
 *
 *
 * @author Oleg Gusakov
 *
 */
public class MetadataResolutionRequest {
    private MavenArtifactMetadata mad;

    private String scope;

    // Needs to go away
    private Set<Artifact> artifactDependencies;

    private ArtifactRepository localRepository;

    private List<ArtifactRepository> remoteRepositories;

    // This is like a filter but overrides all transitive versions
    private Map managedVersionMap;

    /** result type - flat list; the default */
    private boolean asList = true;

    /** result type - dirty tree */
    private boolean asDirtyTree = false;

    /** result type - resolved tree */
    private boolean asResolvedTree = false;

    /** result type - graph */
    private boolean asGraph = false;

    public MetadataResolutionRequest() {}

    public MetadataResolutionRequest(
            MavenArtifactMetadata md, ArtifactRepository localRepository, List<ArtifactRepository> remoteRepositories) {
        this.mad = md;
        this.localRepository = localRepository;
        this.remoteRepositories = remoteRepositories;
    }

    public MavenArtifactMetadata getArtifactMetadata() {
        return mad;
    }

    public MetadataResolutionRequest setArtifactMetadata(MavenArtifactMetadata md) {
        this.mad = md;

        return this;
    }

    public MetadataResolutionRequest setArtifactDependencies(Set<Artifact> artifactDependencies) {
        this.artifactDependencies = artifactDependencies;

        return this;
    }

    public Set<Artifact> getArtifactDependencies() {
        return artifactDependencies;
    }

    public ArtifactRepository getLocalRepository() {
        return localRepository;
    }

    public MetadataResolutionRequest setLocalRepository(ArtifactRepository localRepository) {
        this.localRepository = localRepository;

        return this;
    }

    /**
     * @deprecated instead use {@link #getRemoteRepositories()}
     */
    @Deprecated
    public List<ArtifactRepository> getRemoteRepostories() {
        return remoteRepositories;
    }

    public List<ArtifactRepository> getRemoteRepositories() {
        return getRemoteRepostories();
    }

    /**
     * @deprecated instead use {@link #setRemoteRepositories(List)}
     */
    @Deprecated
    public MetadataResolutionRequest setRemoteRepostories(List<ArtifactRepository> remoteRepostories) {
        this.remoteRepositories = remoteRepostories;

        return this;
    }

    public MetadataResolutionRequest setRemoteRepositories(List<ArtifactRepository> remoteRepositories) {
        return setRemoteRepostories(remoteRepositories);
    }

    public Map getManagedVersionMap() {
        return managedVersionMap;
    }

    public MetadataResolutionRequest setManagedVersionMap(Map managedVersionMap) {
        this.managedVersionMap = managedVersionMap;

        return this;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder()
                .append("REQUEST: ")
                .append("\n")
                .append("artifact: ")
                .append(mad)
                .append("\n")
                .append(artifactDependencies)
                .append("\n")
                .append("localRepository: ")
                .append(localRepository)
                .append("\n")
                .append("remoteRepositories: ")
                .append(remoteRepositories)
                .append("\n");

        return sb.toString();
    }

    public boolean isAsList() {
        return asList;
    }

    public MetadataResolutionRequest setAsList(boolean asList) {
        this.asList = asList;
        return this;
    }

    public boolean isAsDirtyTree() {
        return asDirtyTree;
    }

    public MetadataResolutionRequest setAsDirtyTree(boolean asDirtyTree) {
        this.asDirtyTree = asDirtyTree;
        return this;
    }

    public boolean isAsResolvedTree() {
        return asResolvedTree;
    }

    public MetadataResolutionRequest setAsResolvedTree(boolean asResolvedTree) {
        this.asResolvedTree = asResolvedTree;
        return this;
    }

    public boolean isAsGraph() {
        return asGraph;
    }

    public MetadataResolutionRequest setAsGraph(boolean asGraph) {
        this.asGraph = asGraph;
        return this;
    }

    public MetadataResolutionRequest setScope(String scope) {
        this.scope = scope;
        return this;
    }

    public String getScope() {
        return scope;
    }
}
