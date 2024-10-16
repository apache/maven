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
package org.apache.maven.artifact;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.OverConstrainedVersionException;
import org.apache.maven.artifact.versioning.VersionRange;

/**
 */
public class DefaultArtifact implements Artifact {
    private String groupId;

    private String artifactId;

    private String baseVersion;

    private final String type;

    private final String classifier;

    private volatile String scope;

    private volatile File file;

    private ArtifactRepository repository;

    private String downloadUrl;

    private ArtifactFilter dependencyFilter;

    private ArtifactHandler artifactHandler;

    private List<String> dependencyTrail;

    private volatile String version;

    private VersionRange versionRange;

    private volatile boolean resolved;

    private boolean release;

    private List<ArtifactVersion> availableVersions;

    private Map<Object, ArtifactMetadata> metadataMap;

    private boolean optional;

    public DefaultArtifact(
            String groupId,
            String artifactId,
            String version,
            String scope,
            String type,
            String classifier,
            ArtifactHandler artifactHandler) {
        this(
                groupId,
                artifactId,
                VersionRange.createFromVersion(version),
                scope,
                type,
                classifier,
                artifactHandler,
                false);
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    public DefaultArtifact(
            String groupId,
            String artifactId,
            String version,
            String scope,
            String type,
            String classifier,
            ArtifactHandler artifactHandler,
            boolean optional) {
        this(
                groupId,
                artifactId,
                VersionRange.createFromVersion(version),
                scope,
                type,
                classifier,
                artifactHandler,
                optional);
    }

    public DefaultArtifact(
            String groupId,
            String artifactId,
            VersionRange versionRange,
            String scope,
            String type,
            String classifier,
            ArtifactHandler artifactHandler) {
        this(groupId, artifactId, versionRange, scope, type, classifier, artifactHandler, false);
    }

    @SuppressWarnings("checkstyle:parameternumber")
    public DefaultArtifact(
            String groupId,
            String artifactId,
            VersionRange versionRange,
            String scope,
            String type,
            String classifier,
            ArtifactHandler artifactHandler,
            boolean optional) {
        this.groupId = groupId;

        this.artifactId = artifactId;

        this.versionRange = versionRange;

        selectVersionFromNewRangeIfAvailable();

        this.artifactHandler = artifactHandler;

        this.scope = scope;

        this.type = type;

        if (classifier == null) {
            classifier = artifactHandler.getClassifier();
        }

        this.classifier = classifier;

        this.optional = optional;

        validateIdentity();
    }

    private void validateIdentity() {
        if (empty(groupId)) {
            throw new InvalidArtifactRTException(
                    groupId, artifactId, getVersion(), type, "The groupId cannot be empty.");
        }

        if (empty(artifactId)) {
            throw new InvalidArtifactRTException(
                    groupId, artifactId, getVersion(), type, "The artifactId cannot be empty.");
        }

        if (empty(type)) {
            throw new InvalidArtifactRTException(groupId, artifactId, getVersion(), type, "The type cannot be empty.");
        }

        if ((empty(version)) && (versionRange == null)) {
            throw new InvalidArtifactRTException(
                    groupId, artifactId, getVersion(), type, "The version cannot be empty.");
        }
    }

    public static boolean empty(String value) {
        return (value == null) || (value.trim().length() < 1);
    }

    @Override
    public String getClassifier() {
        return classifier;
    }

    @Override
    public boolean hasClassifier() {
        return classifier != null && !classifier.isEmpty();
    }

    @Override
    public String getScope() {
        return scope;
    }

    @Override
    public String getGroupId() {
        return groupId;
    }

    @Override
    public String getArtifactId() {
        return artifactId;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public void setVersion(String version) {
        this.version = version;
        setBaseVersionInternal(version);
        versionRange = null;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public void setFile(File file) {
        this.file = file;
    }

    @Override
    public File getFile() {
        return file;
    }

    @Override
    public ArtifactRepository getRepository() {
        return repository;
    }

    @Override
    public void setRepository(ArtifactRepository repository) {
        this.repository = repository;
    }

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    @Override
    public String getId() {
        return getDependencyConflictId() + ":" + getBaseVersion();
    }

    @Override
    public String getDependencyConflictId() {
        StringBuilder sb = new StringBuilder(128);
        sb.append(getGroupId());
        sb.append(':');
        appendArtifactTypeClassifierString(sb);
        return sb.toString();
    }

    private void appendArtifactTypeClassifierString(StringBuilder sb) {
        sb.append(getArtifactId());
        sb.append(':');
        sb.append(getType());
        if (hasClassifier()) {
            sb.append(':');
            sb.append(getClassifier());
        }
    }

    @Override
    public void addMetadata(ArtifactMetadata metadata) {
        if (metadataMap == null) {
            metadataMap = new HashMap<>();
        }

        ArtifactMetadata m = metadataMap.get(metadata.getKey());
        if (m != null) {
            m.merge(metadata);
        } else {
            metadataMap.put(metadata.getKey(), metadata);
        }
    }

    @Override
    public Collection<ArtifactMetadata> getMetadataList() {
        if (metadataMap == null) {
            return Collections.emptyList();
        }

        return Collections.unmodifiableCollection(metadataMap.values());
    }

    // ----------------------------------------------------------------------
    // Object overrides
    // ----------------------------------------------------------------------

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (getGroupId() != null) {
            sb.append(getGroupId());
            sb.append(':');
        }
        appendArtifactTypeClassifierString(sb);
        sb.append(':');
        if (getBaseVersionInternal() != null) {
            sb.append(getBaseVersionInternal());
        } else {
            sb.append(versionRange.toString());
        }
        if (scope != null) {
            sb.append(':');
            sb.append(scope);
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DefaultArtifact that = (DefaultArtifact) o;
        return Objects.equals(groupId, that.groupId)
                && Objects.equals(artifactId, that.artifactId)
                && Objects.equals(type, that.type)
                && Objects.equals(classifier, that.classifier)
                && Objects.equals(version, that.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(groupId, artifactId, type, classifier, version);
    }

    @Override
    public String getBaseVersion() {
        if (baseVersion == null && version != null) {
            setBaseVersionInternal(version);
        }

        return baseVersion;
    }

    protected String getBaseVersionInternal() {
        if ((baseVersion == null) && (version != null)) {
            setBaseVersionInternal(version);
        }

        return baseVersion;
    }

    @Override
    public void setBaseVersion(String baseVersion) {
        setBaseVersionInternal(baseVersion);
    }

    protected void setBaseVersionInternal(String baseVersion) {
        this.baseVersion = ArtifactUtils.toSnapshotVersion(baseVersion);
    }

    @Override
    public int compareTo(Artifact a) {
        int result = groupId.compareTo(a.getGroupId());
        if (result == 0) {
            result = artifactId.compareTo(a.getArtifactId());
            if (result == 0) {
                result = type.compareTo(a.getType());
                if (result == 0) {
                    if (classifier == null) {
                        if (a.getClassifier() != null) {
                            result = 1;
                        }
                    } else {
                        if (a.getClassifier() != null) {
                            result = classifier.compareTo(a.getClassifier());
                        } else {
                            result = -1;
                        }
                    }
                    if (result == 0) {
                        // We don't consider the version range in the comparison, just the resolved version
                        result = new DefaultArtifactVersion(version)
                                .compareTo(new DefaultArtifactVersion(a.getVersion()));
                    }
                }
            }
        }
        return result;
    }

    @Override
    public void updateVersion(String version, ArtifactRepository localRepository) {
        setResolvedVersion(version);
        setFile(new File(localRepository.getBasedir(), localRepository.pathOf(this)));
    }

    @Override
    public String getDownloadUrl() {
        return downloadUrl;
    }

    @Override
    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    @Override
    public ArtifactFilter getDependencyFilter() {
        return dependencyFilter;
    }

    @Override
    public void setDependencyFilter(ArtifactFilter artifactFilter) {
        dependencyFilter = artifactFilter;
    }

    @Override
    public ArtifactHandler getArtifactHandler() {
        return artifactHandler;
    }

    @Override
    public List<String> getDependencyTrail() {
        return dependencyTrail;
    }

    @Override
    public void setDependencyTrail(List<String> dependencyTrail) {
        this.dependencyTrail = dependencyTrail;
    }

    @Override
    public void setScope(String scope) {
        this.scope = scope;
    }

    @Override
    public VersionRange getVersionRange() {
        return versionRange;
    }

    @Override
    public void setVersionRange(VersionRange versionRange) {
        this.versionRange = versionRange;
        selectVersionFromNewRangeIfAvailable();
    }

    private void selectVersionFromNewRangeIfAvailable() {
        if ((versionRange != null) && (versionRange.getRecommendedVersion() != null)) {
            selectVersion(versionRange.getRecommendedVersion().toString());
        } else {
            version = null;
            baseVersion = null;
        }
    }

    @Override
    public void selectVersion(String version) {
        this.version = version;
        setBaseVersionInternal(version);
    }

    @Override
    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    @Override
    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    @Override
    public boolean isSnapshot() {
        return getBaseVersion() != null
                && (getBaseVersion().endsWith(SNAPSHOT_VERSION)
                        || getBaseVersion().equals(LATEST_VERSION));
    }

    @Override
    public void setResolved(boolean resolved) {
        this.resolved = resolved;
    }

    @Override
    public boolean isResolved() {
        return resolved;
    }

    @Override
    public void setResolvedVersion(String version) {
        this.version = version;
        // retain baseVersion
    }

    @Override
    public void setArtifactHandler(ArtifactHandler artifactHandler) {
        this.artifactHandler = artifactHandler;
    }

    @Override
    public void setRelease(boolean release) {
        this.release = release;
    }

    @Override
    public boolean isRelease() {
        return release;
    }

    @Override
    public List<ArtifactVersion> getAvailableVersions() {
        return availableVersions;
    }

    @Override
    public void setAvailableVersions(List<ArtifactVersion> availableVersions) {
        this.availableVersions = availableVersions;
    }

    @Override
    public boolean isOptional() {
        return optional;
    }

    @Override
    public ArtifactVersion getSelectedVersion() throws OverConstrainedVersionException {
        return versionRange.getSelectedVersion(this);
    }

    @Override
    public boolean isSelectedVersionKnown() throws OverConstrainedVersionException {
        return versionRange.isSelectedVersionKnown(this);
    }

    @Override
    public void setOptional(boolean optional) {
        this.optional = optional;
    }
}
