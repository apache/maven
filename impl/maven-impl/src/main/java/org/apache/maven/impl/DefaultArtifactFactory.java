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
package org.apache.maven.impl;

import org.apache.maven.api.Artifact;
import org.apache.maven.api.ProducedArtifact;
import org.apache.maven.api.Session;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.di.Named;
import org.apache.maven.api.di.Singleton;
import org.apache.maven.api.services.ArtifactFactory;
import org.eclipse.aether.artifact.ArtifactType;

import static java.util.Objects.requireNonNull;

@Named
@Singleton
public class DefaultArtifactFactory implements ArtifactFactory {
    @Override
    public Artifact create(
            @Nonnull Session session,
            String groupId,
            String artifactId,
            String version,
            String classifier,
            String extension,
            String type) {
        InternalSession internalSession = InternalSession.from(requireNonNull(session, "session"));
        return new DefaultArtifact(
                internalSession,
                createArtifact(internalSession, groupId, artifactId, version, classifier, extension, type));
    }

    @Override
    public ProducedArtifact createProduced(
            @Nonnull Session session,
            String groupId,
            String artifactId,
            String version,
            String classifier,
            String extension,
            String type) {
        InternalSession internalSession = InternalSession.from(requireNonNull(session, "session"));
        return new DefaultProducedArtifact(
                internalSession,
                createArtifact(internalSession, groupId, artifactId, version, classifier, extension, type));
    }

    private org.eclipse.aether.artifact.Artifact createArtifact(
            InternalSession session,
            String groupId,
            String artifactId,
            String version,
            String classifier,
            String extension,
            String typeId) {
        ArtifactType type =
                typeId != null ? session.getSession().getArtifactTypeRegistry().get(typeId) : null;
        String effectiveClassifier =
                classifier != null && !classifier.isEmpty() ? classifier : type != null ? type.getClassifier() : null;
        String effectiveExtension =
                extension != null && !extension.isEmpty() ? extension : type != null ? type.getExtension() : null;
        return new org.eclipse.aether.artifact.DefaultArtifact(
                groupId, artifactId, effectiveClassifier, effectiveExtension, version, type);
    }
}
