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
 * @author jdcasey
 */
@Component(role = ArtifactRepositoryLayout.class, hint = "default")
public class DefaultRepositoryLayout implements ArtifactRepositoryLayout {
    private static final char PATH_SEPARATOR = '/';

    private static final char GROUP_SEPARATOR = '.';

    private static final char ARTIFACT_SEPARATOR = '-';

    public String getId() {
        return "default";
    }

    public String pathOf(Artifact artifact) {
        ArtifactHandler artifactHandler = artifact.getArtifactHandler();

        StringBuilder path = new StringBuilder(128);

        path.append(formatAsDirectory(artifact.getGroupId())).append(PATH_SEPARATOR);
        path.append(artifact.getArtifactId()).append(PATH_SEPARATOR);
        path.append(artifact.getBaseVersion()).append(PATH_SEPARATOR);
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
        return pathOfRepositoryMetadata(metadata, metadata.getLocalFilename(repository));
    }

    private String pathOfRepositoryMetadata(ArtifactMetadata metadata, String filename) {
        StringBuilder path = new StringBuilder(128);

        path.append(formatAsDirectory(metadata.getGroupId())).append(PATH_SEPARATOR);
        if (!metadata.storedInGroupDirectory()) {
            path.append(metadata.getArtifactId()).append(PATH_SEPARATOR);

            if (metadata.storedInArtifactVersionDirectory()) {
                path.append(metadata.getBaseVersion()).append(PATH_SEPARATOR);
            }
        }

        path.append(filename);

        return path.toString();
    }

    public String pathOfRemoteRepositoryMetadata(ArtifactMetadata metadata) {
        return pathOfRepositoryMetadata(metadata, metadata.getRemoteFilename());
    }

    private String formatAsDirectory(String directory) {
        return directory.replace(GROUP_SEPARATOR, PATH_SEPARATOR);
    }

    @Override
    public String toString() {
        return getId();
    }
}
