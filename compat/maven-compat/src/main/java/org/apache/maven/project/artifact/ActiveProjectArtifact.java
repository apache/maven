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
package org.apache.maven.project.artifact;

import java.io.File;
import java.util.Collection;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.OverConstrainedVersionException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.project.MavenProject;

/**
 * Wraps an active project instance to be able to receive updates from its artifact without affecting the original
 * attributes of this artifact.
 *
 * TODO I think this exposes a design flaw in that the immutable and mutable parts of an artifact are in one class and
 * should be split. ie scope, file, etc depend on the context of use, whereas everything else is immutable.
 */
@Deprecated
public class ActiveProjectArtifact implements Artifact {
    private final Artifact artifact;

    private final MavenProject project;

    public ActiveProjectArtifact(MavenProject project, Artifact artifact) {
        this.artifact = artifact;
        this.project = project;

        artifact.setFile(project.getArtifact().getFile());
        artifact.setResolved(true);
    }

    /** {@inheritDoc} */
    @Override
    public File getFile() {
        // we need to get the latest file for the project, not the artifact that was created at one point in time
        return project.getArtifact().getFile();
    }

    /** {@inheritDoc} */
    @Override
    public String getGroupId() {
        return artifact.getGroupId();
    }

    /** {@inheritDoc} */
    @Override
    public String getArtifactId() {
        return artifact.getArtifactId();
    }

    /** {@inheritDoc} */
    @Override
    public String getVersion() {
        return artifact.getVersion();
    }

    /** {@inheritDoc} */
    @Override
    public void setVersion(String version) {
        artifact.setVersion(version);
    }

    /** {@inheritDoc} */
    @Override
    public String getScope() {
        return artifact.getScope();
    }

    /** {@inheritDoc} */
    @Override
    public String getType() {
        return artifact.getType();
    }

    /** {@inheritDoc} */
    @Override
    public String getClassifier() {
        return artifact.getClassifier();
    }

    /** {@inheritDoc} */
    @Override
    public boolean hasClassifier() {
        return artifact.hasClassifier();
    }

    /** {@inheritDoc} */
    @Override
    public void setFile(File destination) {
        artifact.setFile(destination);
        project.getArtifact().setFile(destination);
    }

    /** {@inheritDoc} */
    @Override
    public String getBaseVersion() {
        return artifact.getBaseVersion();
    }

    /** {@inheritDoc} */
    @Override
    public void setBaseVersion(String baseVersion) {
        artifact.setBaseVersion(baseVersion);
    }

    /** {@inheritDoc} */
    @Override
    public String getId() {
        return artifact.getId();
    }

    /** {@inheritDoc} */
    @Override
    public String getDependencyConflictId() {
        return artifact.getDependencyConflictId();
    }

    /** {@inheritDoc} */
    @Override
    public void addMetadata(ArtifactMetadata metadata) {
        artifact.addMetadata(metadata);
    }

    /** {@inheritDoc} */
    @Override
    public Collection<ArtifactMetadata> getMetadataList() {
        return artifact.getMetadataList();
    }

    /** {@inheritDoc} */
    @Override
    public void setRepository(ArtifactRepository remoteRepository) {
        artifact.setRepository(remoteRepository);
    }

    /** {@inheritDoc} */
    @Override
    public ArtifactRepository getRepository() {
        return artifact.getRepository();
    }

    /** {@inheritDoc} */
    @Override
    public void updateVersion(String version, ArtifactRepository localRepository) {
        artifact.updateVersion(version, localRepository);
    }

    /** {@inheritDoc} */
    @Override
    public String getDownloadUrl() {
        return artifact.getDownloadUrl();
    }

    /** {@inheritDoc} */
    @Override
    public void setDownloadUrl(String downloadUrl) {
        artifact.setDownloadUrl(downloadUrl);
    }

    /** {@inheritDoc} */
    @Override
    public ArtifactFilter getDependencyFilter() {
        return artifact.getDependencyFilter();
    }

    /** {@inheritDoc} */
    @Override
    public void setDependencyFilter(ArtifactFilter artifactFilter) {
        artifact.setDependencyFilter(artifactFilter);
    }

    /** {@inheritDoc} */
    @Override
    public ArtifactHandler getArtifactHandler() {
        return artifact.getArtifactHandler();
    }

    /** {@inheritDoc} */
    @Override
    public List<String> getDependencyTrail() {
        return artifact.getDependencyTrail();
    }

    /** {@inheritDoc} */
    @Override
    public void setDependencyTrail(List<String> dependencyTrail) {
        artifact.setDependencyTrail(dependencyTrail);
    }

    /** {@inheritDoc} */
    @Override
    public void setScope(String scope) {
        artifact.setScope(scope);
    }

    /** {@inheritDoc} */
    @Override
    public VersionRange getVersionRange() {
        return artifact.getVersionRange();
    }

    /** {@inheritDoc} */
    @Override
    public void setVersionRange(VersionRange newRange) {
        artifact.setVersionRange(newRange);
    }

    /** {@inheritDoc} */
    @Override
    public void selectVersion(String version) {
        artifact.selectVersion(version);
    }

    /** {@inheritDoc} */
    @Override
    public void setGroupId(String groupId) {
        artifact.setGroupId(groupId);
    }

    /** {@inheritDoc} */
    @Override
    public void setArtifactId(String artifactId) {
        artifact.setArtifactId(artifactId);
    }

    /** {@inheritDoc} */
    @Override
    public boolean isSnapshot() {
        return artifact.isSnapshot();
    }

    /** {@inheritDoc} */
    @Override
    public int compareTo(Artifact a) {
        return artifact.compareTo(a);
    }

    /** {@inheritDoc} */
    @Override
    public void setResolved(boolean resolved) {
        artifact.setResolved(resolved);
    }

    /** {@inheritDoc} */
    @Override
    public boolean isResolved() {
        return artifact.isResolved();
    }

    /** {@inheritDoc} */
    @Override
    public void setResolvedVersion(String version) {
        artifact.setResolvedVersion(version);
    }

    /** {@inheritDoc} */
    @Override
    public void setArtifactHandler(ArtifactHandler handler) {
        artifact.setArtifactHandler(handler);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "active project artifact[artifact: " + artifact + ", project: " + project + "]";
    }

    /** {@inheritDoc} */
    @Override
    public boolean isRelease() {
        return artifact.isRelease();
    }

    /** {@inheritDoc} */
    @Override
    public void setRelease(boolean release) {
        artifact.setRelease(release);
    }

    /** {@inheritDoc} */
    @Override
    public List<ArtifactVersion> getAvailableVersions() {
        return artifact.getAvailableVersions();
    }

    /** {@inheritDoc} */
    @Override
    public void setAvailableVersions(List<ArtifactVersion> versions) {
        artifact.setAvailableVersions(versions);
    }

    /** {@inheritDoc} */
    @Override
    public boolean isOptional() {
        return artifact.isOptional();
    }

    /** {@inheritDoc} */
    @Override
    public ArtifactVersion getSelectedVersion() throws OverConstrainedVersionException {
        return artifact.getSelectedVersion();
    }

    /** {@inheritDoc} */
    @Override
    public boolean isSelectedVersionKnown() throws OverConstrainedVersionException {
        return artifact.isSelectedVersionKnown();
    }

    /** {@inheritDoc} */
    @Override
    public void setOptional(boolean optional) {
        artifact.setOptional(optional);
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        int result = 17;

        result = 37 * result + getGroupId().hashCode();
        result = 37 * result + getArtifactId().hashCode();
        result = 37 * result + getType().hashCode();
        if (getVersion() != null) {
            result = 37 * result + getVersion().hashCode();
        }
        result = 37 * result + (getClassifier() != null ? getClassifier().hashCode() : 0);

        return result;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }

        if (o instanceof Artifact a) {
            if (!a.getGroupId().equals(getGroupId())) {
                return false;
            } else if (!a.getArtifactId().equals(getArtifactId())) {
                return false;
            } else if (!a.getVersion().equals(getVersion())) {
                return false;
            } else if (!a.getType().equals(getType())) {
                return false;
            } else {
                return a.getClassifier() == null
                        ? getClassifier() == null
                        : a.getClassifier().equals(getClassifier());
            }
        } else {
            return false;
        }
    }
}
