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

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.InvalidArtifactRTException;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.VersionRange;

/**
 *<strong>Warning:</strong> This is an internal utility class that is only public for technical reasons, it is not part
 * of the public API. In particular, this class can be changed or deleted without prior notice. Use
 * {@link org.apache.maven.project.MavenProjectHelper#attachArtifact} instead.
 */
@Deprecated
public class AttachedArtifact extends DefaultArtifact {

    private final Artifact parent;

    public AttachedArtifact(Artifact parent, String type, String classifier, ArtifactHandler artifactHandler) {
        super(
                parent.getGroupId(),
                parent.getArtifactId(),
                parent.getVersionRange(),
                parent.getScope(),
                type,
                classifier,
                artifactHandler,
                parent.isOptional());

        setDependencyTrail(Collections.singletonList(parent.getId()));

        this.parent = parent;

        if (getId().equals(parent.getId())) {
            throw new InvalidArtifactRTException(
                    parent.getGroupId(),
                    parent.getArtifactId(),
                    parent.getVersion(),
                    parent.getType(),
                    "An attached artifact must have a different ID" + " than its corresponding main artifact.");
        }
    }

    public AttachedArtifact(Artifact parent, String type, ArtifactHandler artifactHandler) {
        this(parent, type, null, artifactHandler);
    }

    @Override
    public void setArtifactId(String artifactId) {
        throw new UnsupportedOperationException(
                "Cannot change the artifactId for an attached artifact." + " It is derived from the main artifact.");
    }

    @Override
    public List<ArtifactVersion> getAvailableVersions() {
        return parent.getAvailableVersions();
    }

    @Override
    public void setAvailableVersions(List<ArtifactVersion> availableVersions) {
        throw new UnsupportedOperationException("Cannot change the version information for an attached artifact."
                + " It is derived from the main artifact.");
    }

    @Override
    public String getBaseVersion() {
        return parent.getBaseVersion();
    }

    @Override
    public void setBaseVersion(String baseVersion) {
        throw new UnsupportedOperationException("Cannot change the version information for an attached artifact."
                + " It is derived from the main artifact.");
    }

    @Override
    public String getDownloadUrl() {
        return parent.getDownloadUrl();
    }

    @Override
    public void setDownloadUrl(String downloadUrl) {
        throw new UnsupportedOperationException("Cannot change the download information for an attached artifact."
                + " It is derived from the main artifact.");
    }

    @Override
    public void setGroupId(String groupId) {
        throw new UnsupportedOperationException(
                "Cannot change the groupId for an attached artifact." + " It is derived from the main artifact.");
    }

    @Override
    public ArtifactRepository getRepository() {
        return parent.getRepository();
    }

    @Override
    public void setRepository(ArtifactRepository repository) {
        throw new UnsupportedOperationException("Cannot change the repository information for an attached artifact."
                + " It is derived from the main artifact.");
    }

    @Override
    public String getScope() {
        return parent.getScope();
    }

    @Override
    public void setScope(String scope) {
        throw new UnsupportedOperationException("Cannot change the scoping information for an attached artifact."
                + " It is derived from the main artifact.");
    }

    @Override
    public String getVersion() {
        return parent.getVersion();
    }

    @Override
    public void setVersion(String version) {
        throw new UnsupportedOperationException("Cannot change the version information for an attached artifact."
                + " It is derived from the main artifact.");
    }

    @Override
    public VersionRange getVersionRange() {
        return parent.getVersionRange();
    }

    @Override
    public void setVersionRange(VersionRange range) {
        throw new UnsupportedOperationException("Cannot change the version information for an attached artifact."
                + " It is derived from the main artifact.");
    }

    @Override
    public boolean isRelease() {
        return parent.isRelease();
    }

    @Override
    public void setRelease(boolean release) {
        throw new UnsupportedOperationException("Cannot change the version information for an attached artifact."
                + " It is derived from the main artifact.");
    }

    @Override
    public boolean isSnapshot() {
        return parent.isSnapshot();
    }

    @Override
    public void addMetadata(ArtifactMetadata metadata) {
        // ignore. The parent artifact will handle metadata.
        // we must fail silently here to avoid problems with the artifact transformers.
    }

    @Override
    public Collection<ArtifactMetadata> getMetadataList() {
        return Collections.emptyList();
    }
}
