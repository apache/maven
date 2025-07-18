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
package org.apache.maven.internal.aether;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.internal.transformation.TransformerManager;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.deployment.DeployRequest;
import org.eclipse.aether.installation.InstallRequest;
import org.eclipse.aether.spi.artifact.transformer.ArtifactTransformer;

import static java.util.Objects.requireNonNull;

/**
 * Maven specific transformer.
 */
@Singleton
@Named
final class MavenTransformer implements ArtifactTransformer {
    private final TransformerManager transformerManager;

    @Inject
    MavenTransformer(TransformerManager transformerManager) {
        this.transformerManager = requireNonNull(transformerManager);
    }

    @Override
    public InstallRequest transformInstallArtifacts(RepositorySystemSession session, InstallRequest request) {
        return transformerManager.remapInstallArtifacts(session, request);
    }

    @Override
    public DeployRequest transformDeployArtifacts(RepositorySystemSession session, DeployRequest request) {
        return transformerManager.remapDeployArtifacts(session, request);
    }
}
