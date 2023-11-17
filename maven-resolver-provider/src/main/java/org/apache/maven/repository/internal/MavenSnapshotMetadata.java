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
package org.apache.maven.repository.internal;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

import org.apache.maven.artifact.repository.metadata.Metadata;
import org.eclipse.aether.artifact.Artifact;

/**
 * @author Hervé Boutemy
 */
abstract class MavenSnapshotMetadata extends MavenMetadata {
    static final String SNAPSHOT = "SNAPSHOT";

    protected final Collection<Artifact> artifacts = new ArrayList<>();

    protected final boolean legacyFormat;

    protected MavenSnapshotMetadata(Metadata metadata, File file, boolean legacyFormat, Date timestamp) {
        super(metadata, file, timestamp);
        this.legacyFormat = legacyFormat;
    }

    protected static Metadata createRepositoryMetadata(Artifact artifact, boolean legacyFormat) {
        Metadata metadata = new Metadata();
        if (!legacyFormat) {
            metadata.setModelVersion("1.1.0");
        }
        metadata.setGroupId(artifact.getGroupId());
        metadata.setArtifactId(artifact.getArtifactId());
        metadata.setVersion(artifact.getBaseVersion());

        return metadata;
    }

    public void bind(Artifact artifact) {
        artifacts.add(artifact);
    }

    public Object getKey() {
        return getGroupId() + ':' + getArtifactId() + ':' + getVersion();
    }

    public static Object getKey(Artifact artifact) {
        return artifact.getGroupId() + ':' + artifact.getArtifactId() + ':' + artifact.getBaseVersion();
    }

    protected String getKey(String classifier, String extension) {
        return classifier + ':' + extension;
    }

    @Override
    public String getGroupId() {
        return metadata.getGroupId();
    }

    @Override
    public String getArtifactId() {
        return metadata.getArtifactId();
    }

    @Override
    public String getVersion() {
        return metadata.getVersion();
    }

    @Override
    public Nature getNature() {
        return Nature.SNAPSHOT;
    }
}
