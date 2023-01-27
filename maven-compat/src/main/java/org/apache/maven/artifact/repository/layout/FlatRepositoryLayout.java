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
package org.apache.maven.artifact.repository.layout;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.codehaus.plexus.component.annotations.Component;

/**
 * FlatRepositoryLayout
 */
@Component(role = ArtifactRepositoryLayout.class, hint = "flat")
public class FlatRepositoryLayout implements ArtifactRepositoryLayout {

    private static final char ARTIFACT_SEPARATOR = '-';

    private static final char GROUP_SEPARATOR = '.';

    public String getId() {
        return "flat";
    }

    public String pathOf(Artifact artifact) {
        ArtifactHandler artifactHandler = artifact.getArtifactHandler();

        StringBuilder path = new StringBuilder(128);

        path.append(artifact.getArtifactId()).append(ARTIFACT_SEPARATOR).append(artifact.getVersion());

        if (artifact.hasClassifier()) {
            path.append(ARTIFACT_SEPARATOR).append(artifact.getClassifier());
        }

        if (artifactHandler.getExtension() != null
                && artifactHandler.getExtension().length() > 0) {
            path.append(GROUP_SEPARATOR).append(artifactHandler.getExtension());
        }

        return path.toString();
    }

    public String pathOfLocalRepositoryMetadata(ArtifactMetadata metadata, ArtifactRepository repository) {
        return pathOfRepositoryMetadata(metadata.getLocalFilename(repository));
    }

    private String pathOfRepositoryMetadata(String filename) {
        StringBuilder path = new StringBuilder(128);

        path.append(filename);

        return path.toString();
    }

    public String pathOfRemoteRepositoryMetadata(ArtifactMetadata metadata) {
        return pathOfRepositoryMetadata(metadata.getRemoteFilename());
    }

    @Override
    public String toString() {
        return getId();
    }
}
