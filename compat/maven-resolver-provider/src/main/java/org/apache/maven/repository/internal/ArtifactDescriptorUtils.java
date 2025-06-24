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

import org.apache.maven.model.Repository;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RepositoryPolicy;

/**
 * <strong>Warning:</strong> This is an internal utility class that is only public for technical reasons, it is not part
 * of the public API. In particular, this class can be changed or deleted without prior notice.
 *
 * @deprecated since 4.0.0, use {@code maven-api-impl} jar instead
 */
@Deprecated(since = "4.0.0")
public class ArtifactDescriptorUtils {

    public static Artifact toPomArtifact(Artifact artifact) {
        Artifact pomArtifact = artifact;

        if (!pomArtifact.getClassifier().isEmpty() || !"pom".equals(pomArtifact.getExtension())) {
            pomArtifact =
                    new DefaultArtifact(artifact.getGroupId(), artifact.getArtifactId(), "pom", artifact.getVersion());
        }

        return pomArtifact;
    }

    /**
     * Creates POM artifact out of passed in artifact by dropping classifier (if exists) and rewriting extension to
     * "pom". Unconditionally, unlike {@link #toPomArtifact(Artifact)} that does this only for artifacts without
     * classifiers.
     *
     * @since 4.0.0
     */
    public static Artifact toPomArtifactUnconditionally(Artifact artifact) {
        return new DefaultArtifact(artifact.getGroupId(), artifact.getArtifactId(), "pom", artifact.getVersion());
    }

    public static RemoteRepository toRemoteRepository(Repository repository) {
        RemoteRepository.Builder builder =
                new RemoteRepository.Builder(repository.getId(), repository.getLayout(), repository.getUrl());
        builder.setSnapshotPolicy(toRepositoryPolicy(repository.getSnapshots()));
        builder.setReleasePolicy(toRepositoryPolicy(repository.getReleases()));
        return builder.build();
    }

    public static RepositoryPolicy toRepositoryPolicy(org.apache.maven.model.RepositoryPolicy policy) {
        boolean enabled = true;
        String checksums = toRepositoryChecksumPolicy(RepositoryPolicy.CHECKSUM_POLICY_WARN); // the default
        String updates = RepositoryPolicy.UPDATE_POLICY_DAILY;

        if (policy != null) {
            enabled = policy.isEnabled();
            if (policy.getUpdatePolicy() != null) {
                updates = policy.getUpdatePolicy();
            }
            if (policy.getChecksumPolicy() != null) {
                checksums = policy.getChecksumPolicy();
            }
        }

        return new RepositoryPolicy(enabled, updates, checksums);
    }

    public static String toRepositoryChecksumPolicy(final String artifactRepositoryPolicy) {
        return switch (artifactRepositoryPolicy) {
            case RepositoryPolicy.CHECKSUM_POLICY_FAIL -> RepositoryPolicy.CHECKSUM_POLICY_FAIL;
            case RepositoryPolicy.CHECKSUM_POLICY_IGNORE -> RepositoryPolicy.CHECKSUM_POLICY_IGNORE;
            case RepositoryPolicy.CHECKSUM_POLICY_WARN -> RepositoryPolicy.CHECKSUM_POLICY_WARN;
            default -> throw new IllegalArgumentException(
                    "unknown repository checksum policy: " + artifactRepositoryPolicy);
        };
    }
}
