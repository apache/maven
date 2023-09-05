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

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.deployment.DeployRequest;
import org.eclipse.aether.impl.MetadataGenerator;
import org.eclipse.aether.installation.InstallRequest;
import org.eclipse.aether.metadata.Metadata;
import org.eclipse.aether.util.ConfigUtils;

/**
 * Maven GA level metadata generator.
 *
 * Version metadata contains list of existing baseVersions within this GA.
 */
class VersionsMetadataGenerator implements MetadataGenerator {

    private final Map<Object, VersionsMetadata> versions;

    private final Map<Object, VersionsMetadata> processedVersions;

    private final Date timestamp;

    VersionsMetadataGenerator(RepositorySystemSession session, InstallRequest request) {
        this(session, request.getMetadata());
    }

    VersionsMetadataGenerator(RepositorySystemSession session, DeployRequest request) {
        this(session, request.getMetadata());
    }

    private VersionsMetadataGenerator(RepositorySystemSession session, Collection<? extends Metadata> metadatas) {
        versions = new LinkedHashMap<>();
        processedVersions = new LinkedHashMap<>();
        timestamp = (Date) ConfigUtils.getObject(session, new Date(), "maven.startTime");

        /*
         * NOTE: This should be considered a quirk to support interop with Maven's legacy ArtifactDeployer which
         * processes one artifact at a time and hence cannot associate the artifacts from the same project to use the
         * same version index. Allowing the caller to pass in metadata from a previous deployment allows to re-establish
         * the association between the artifacts of the same project.
         */
        for (Iterator<? extends Metadata> it = metadatas.iterator(); it.hasNext(); ) {
            Metadata metadata = it.next();
            if (metadata instanceof VersionsMetadata) {
                it.remove();
                VersionsMetadata versionsMetadata = (VersionsMetadata) metadata;
                processedVersions.put(versionsMetadata.getKey(), versionsMetadata);
            }
        }
    }

    @Override
    public Collection<? extends Metadata> prepare(Collection<? extends Artifact> artifacts) {
        return Collections.emptyList();
    }

    @Override
    public Artifact transformArtifact(Artifact artifact) {
        return artifact;
    }

    @Override
    public Collection<? extends Metadata> finish(Collection<? extends Artifact> artifacts) {
        for (Artifact artifact : artifacts) {
            Object key = VersionsMetadata.getKey(artifact);
            if (processedVersions.get(key) == null) {
                VersionsMetadata versionsMetadata = versions.get(key);
                if (versionsMetadata == null) {
                    versionsMetadata = new VersionsMetadata(artifact, timestamp);
                    versions.put(key, versionsMetadata);
                }
            }
        }

        return versions.values();
    }
}
