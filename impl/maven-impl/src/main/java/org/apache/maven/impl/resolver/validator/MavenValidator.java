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
package org.apache.maven.impl.resolver.validator;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.metadata.Metadata;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.spi.validator.Validator;

/**
 * Simplest Maven specific validator that is meant to prevent un-interpolated
 * elements enter resolver; if it does, is most likely some bug.
 */
public class MavenValidator implements Validator {
    protected boolean containsPlaceholder(String value) {
        return value != null && value.contains("${");
    }

    @Override
    public void isValidArtifact(Artifact artifact) throws IllegalArgumentException {
        if (containsPlaceholder(artifact.getGroupId())
                || containsPlaceholder(artifact.getArtifactId())
                || containsPlaceholder(artifact.getVersion())
                || containsPlaceholder(artifact.getClassifier())
                || containsPlaceholder(artifact.getExtension())) {
            throw new IllegalArgumentException("Not fully interpolated artifact " + artifact);
        }
    }

    @Override
    public void isValidMetadata(Metadata metadata) throws IllegalArgumentException {
        if (containsPlaceholder(metadata.getGroupId())
                || containsPlaceholder(metadata.getArtifactId())
                || containsPlaceholder(metadata.getVersion())
                || containsPlaceholder(metadata.getType())) {
            throw new IllegalArgumentException("Not fully interpolated metadata " + metadata);
        }
    }

    @Override
    public void isValidDependency(Dependency dependency) throws IllegalArgumentException {
        Artifact artifact = dependency.getArtifact();
        if (containsPlaceholder(artifact.getGroupId())
                || containsPlaceholder(artifact.getArtifactId())
                || containsPlaceholder(artifact.getVersion())
                || containsPlaceholder(artifact.getClassifier())
                || containsPlaceholder(artifact.getExtension())
                || containsPlaceholder(dependency.getScope())
                || dependency.getExclusions().stream()
                        .anyMatch(e -> containsPlaceholder(e.getGroupId())
                                || containsPlaceholder(e.getArtifactId())
                                || containsPlaceholder(e.getClassifier())
                                || containsPlaceholder(e.getExtension()))) {
            throw new IllegalArgumentException("Not fully interpolated dependency " + dependency);
        }
    }

    @Override
    public void isValidLocalRepository(LocalRepository localRepository) throws IllegalArgumentException {
        if (containsPlaceholder(localRepository.getBasePath().toString())
                || containsPlaceholder(localRepository.getContentType())
                || containsPlaceholder(localRepository.getId())) {
            throw new IllegalArgumentException("Not fully interpolated local repository " + localRepository);
        }
    }

    @Override
    public void isValidRemoteRepository(RemoteRepository remoteRepository) throws IllegalArgumentException {
        if (containsPlaceholder(remoteRepository.getUrl())
                || containsPlaceholder(remoteRepository.getContentType())
                || containsPlaceholder(remoteRepository.getId())) {
            throw new IllegalArgumentException("Not fully interpolated remote repository " + remoteRepository);
        }
    }
}
