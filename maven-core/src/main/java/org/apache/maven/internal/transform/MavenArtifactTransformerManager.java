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

import java.util.ArrayList;
import java.util.Collection;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.apache.maven.feature.Features;
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
@Singleton
@Named
public class MavenArtifactTransformerManager implements ArtifactTransformerManager {
    private final ConsumerModelArtifactTransformer consumerModelArtifactTransformer;

    @Inject
    public MavenArtifactTransformerManager(ConsumerModelArtifactTransformer consumerModelArtifactTransformer) {
        this.consumerModelArtifactTransformer = consumerModelArtifactTransformer;
    }

    @Override
    public Collection<ArtifactTransformer> getTransformersForArtifact(
            RepositorySystemSession session, Artifact artifact) {
        ArrayList<ArtifactTransformer> result = new ArrayList<>();
        if (Features.buildConsumer(session.getUserProperties()).isActive()) {
            result.add(consumerModelArtifactTransformer);
        } else {
            result.add(Identity.TRANSFORMER);
        }
        // further chain, like sign? or have some generic chain and just pass (to resolver mgr to supplement it?
        return result;
    }
}
