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
package org.apache.maven.internal.transform;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.Collection;
import org.apache.maven.feature.Features;
import org.codehaus.plexus.util.StringUtils;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.transform.ArtifactTransformer;
import org.eclipse.aether.transform.ArtifactTransformerManager;
import org.eclipse.aether.transform.Identity;

/**
 * Maven specific artifact transformer manager.
 *
 * @since TBD
 */
public final class MavenArtifactTransformerManager implements ArtifactTransformerManager {
    private final RepositorySystemSession session;

    public MavenArtifactTransformerManager(RepositorySystemSession session) {
        this.session = requireNonNull(session);
    }

    @Override
    public Collection<ArtifactTransformer> getTransformersForArtifact(Artifact artifact) {
        ArrayList<ArtifactTransformer> result = new ArrayList<>();

        // Consumer POM: if feature enabled and artifact applies
        if (Features.buildConsumer(session.getUserProperties()).isActive()
                && "pom".equals(artifact.getExtension())
                && StringUtils.isBlank(artifact.getClassifier())) {
            result.add(ConsumerModelArtifactTransformer.INSTANCE);
        } else {
            result.add(Identity.TRANSFORMER);
        }
        return result;
    }
}
