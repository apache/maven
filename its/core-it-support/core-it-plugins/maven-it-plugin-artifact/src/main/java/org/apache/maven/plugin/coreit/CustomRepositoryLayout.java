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
package org.apache.maven.plugin.coreit;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;

/**
 * @author Benjamin Bentmann
 */
public class CustomRepositoryLayout implements ArtifactRepositoryLayout {
    @Override
    public String getId() {
        return "id";
    }

    public String pathOf(Artifact artifact) {
        ArtifactHandler artifactHandler = artifact.getArtifactHandler();

        StringBuilder path = new StringBuilder();

        path.append(artifact.getArtifactId()).append('-').append(artifact.getVersion());

        if (artifact.hasClassifier()) {
            path.append('-').append(artifact.getClassifier());
        }

        if (artifactHandler.getExtension() != null
                && artifactHandler.getExtension().length() > 0) {
            path.append('.').append(artifactHandler.getExtension());
        }

        return path.toString();
    }

    public String pathOfLocalRepositoryMetadata(ArtifactMetadata metadata, ArtifactRepository repository) {
        return metadata.getLocalFilename(repository);
    }

    public String pathOfRemoteRepositoryMetadata(ArtifactMetadata metadata) {
        return metadata.getRemoteFilename();
    }
}
