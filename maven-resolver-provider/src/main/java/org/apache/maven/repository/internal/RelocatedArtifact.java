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
import java.util.Map;
import java.util.Objects;

import org.eclipse.aether.artifact.AbstractArtifact;
import org.eclipse.aether.artifact.Artifact;

/**
 * @author Benjamin Bentmann
 */
public final class RelocatedArtifact extends AbstractArtifact {

    private final Artifact artifact;

    private final String groupId;

    private final String artifactId;

    private final String version;

    private final String message;

    RelocatedArtifact(Artifact artifact, String groupId, String artifactId, String version, String message) {
        this.artifact = Objects.requireNonNull(artifact, "artifact cannot be null");
        // TODO Use StringUtils here
        this.groupId = (groupId != null && groupId.length() > 0) ? groupId : null;
        this.artifactId = (artifactId != null && artifactId.length() > 0) ? artifactId : null;
        this.version = (version != null && version.length() > 0) ? version : null;
        this.message = (message != null && message.length() > 0) ? message : null;
    }

    @Override
    public String getGroupId() {
        if (groupId != null) {
            return groupId;
        } else {
            return artifact.getGroupId();
        }
    }

    @Override
    public String getArtifactId() {
        if (artifactId != null) {
            return artifactId;
        } else {
            return artifact.getArtifactId();
        }
    }

    @Override
    public String getVersion() {
        if (version != null) {
            return version;
        } else {
            return artifact.getVersion();
        }
    }

    // Revise these three methods when MRESOLVER-233 is delivered
    @Override
    public Artifact setVersion(String version) {
        String current = getVersion();
        if (current.equals(version) || (version == null && current.length() <= 0)) {
            return this;
        }
        return new RelocatedArtifact(artifact, groupId, artifactId, version, message);
    }

    @Override
    public Artifact setFile(File file) {
        File current = getFile();
        if (Objects.equals(current, file)) {
            return this;
        }
        return new RelocatedArtifact(artifact.setFile(file), groupId, artifactId, version, message);
    }

    @Override
    public Artifact setProperties(Map<String, String> properties) {
        Map<String, String> current = getProperties();
        if (current.equals(properties) || (properties == null && current.isEmpty())) {
            return this;
        }
        return new RelocatedArtifact(artifact.setProperties(properties), groupId, artifactId, version, message);
    }

    @Override
    public String getClassifier() {
        return artifact.getClassifier();
    }

    @Override
    public String getExtension() {
        return artifact.getExtension();
    }

    @Override
    public File getFile() {
        return artifact.getFile();
    }

    @Override
    public String getProperty(String key, String defaultValue) {
        return artifact.getProperty(key, defaultValue);
    }

    @Override
    public Map<String, String> getProperties() {
        return artifact.getProperties();
    }

    public String getMessage() {
        return message;
    }
}
