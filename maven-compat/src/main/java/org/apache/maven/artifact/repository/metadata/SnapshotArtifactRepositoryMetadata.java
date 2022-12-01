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
package org.apache.maven.artifact.repository.metadata;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;

/**
 * Metadata for the artifact version directory of the repository.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * TODO split instantiation (versioning, plugin mappings) from definition
 */
public class SnapshotArtifactRepositoryMetadata extends AbstractRepositoryMetadata {
    private Artifact artifact;

    public SnapshotArtifactRepositoryMetadata(Artifact artifact) {
        super(createMetadata(artifact, null));
        this.artifact = artifact;
    }

    public SnapshotArtifactRepositoryMetadata(Artifact artifact, Snapshot snapshot) {
        super(createMetadata(artifact, createVersioning(snapshot)));
        this.artifact = artifact;
    }

    public boolean storedInGroupDirectory() {
        return false;
    }

    public boolean storedInArtifactVersionDirectory() {
        return true;
    }

    public String getGroupId() {
        return artifact.getGroupId();
    }

    public String getArtifactId() {
        return artifact.getArtifactId();
    }

    public String getBaseVersion() {
        return artifact.getBaseVersion();
    }

    public Object getKey() {
        return "snapshot " + artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + artifact.getBaseVersion();
    }

    public boolean isSnapshot() {
        return artifact.isSnapshot();
    }

    public int getNature() {
        return isSnapshot() ? SNAPSHOT : RELEASE;
    }

    public ArtifactRepository getRepository() {
        return artifact.getRepository();
    }

    public void setRepository(ArtifactRepository remoteRepository) {
        artifact.setRepository(remoteRepository);
    }
}
