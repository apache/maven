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
import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.impl.MetadataGenerator;
import org.eclipse.aether.installation.InstallRequest;
import org.eclipse.aether.metadata.Metadata;
import org.eclipse.aether.util.ConfigUtils;

/**
 * Maven local GAV level metadata generator.
 * <p>
 * Local snapshot metadata contains non-transformed snapshot version.
 */
class LocalSnapshotMetadataGenerator implements MetadataGenerator {

    private Map<Object, LocalSnapshotMetadata> snapshots;

    private final boolean legacyFormat;

    private final Date timestamp;

    LocalSnapshotMetadataGenerator(RepositorySystemSession session, InstallRequest request) {
        legacyFormat = ConfigUtils.getBoolean(session.getConfigProperties(), false, "maven.metadata.legacy");

        timestamp = (Date) ConfigUtils.getObject(session, new Date(), "maven.startTime");

        snapshots = new LinkedHashMap<>();
    }

    @Override
    public Collection<? extends Metadata> prepare(Collection<? extends Artifact> artifacts) {
        for (Artifact artifact : artifacts) {
            if (artifact.isSnapshot()) {
                Object key = LocalSnapshotMetadata.getKey(artifact);
                LocalSnapshotMetadata snapshotMetadata = snapshots.get(key);
                if (snapshotMetadata == null) {
                    snapshotMetadata = new LocalSnapshotMetadata(artifact, legacyFormat, timestamp);
                    snapshots.put(key, snapshotMetadata);
                }
                snapshotMetadata.bind(artifact);
            }
        }

        return Collections.emptyList();
    }

    @Override
    public Artifact transformArtifact(Artifact artifact) {
        return artifact;
    }

    @Override
    public Collection<? extends Metadata> finish(Collection<? extends Artifact> artifacts) {
        return snapshots.values();
    }
}
