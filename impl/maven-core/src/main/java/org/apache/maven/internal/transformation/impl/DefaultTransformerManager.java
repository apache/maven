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
package org.apache.maven.internal.transformation.impl;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import java.io.IOException;
import java.util.Map;

import org.apache.maven.internal.transformation.PomArtifactTransformer;
import org.apache.maven.internal.transformation.TransformerManager;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.deployment.DeployRequest;
import org.eclipse.aether.installation.InstallRequest;

import static java.util.Objects.requireNonNull;

@Singleton
@Named
public class DefaultTransformerManager implements TransformerManager {
    private final Map<String, PomArtifactTransformer> transformers;

    @Inject
    public DefaultTransformerManager(Map<String, PomArtifactTransformer> transformers) {
        this.transformers = requireNonNull(transformers);
    }

    @Override
    public InstallRequest remapInstallArtifacts(RepositorySystemSession session, InstallRequest request) {
        for (PomArtifactTransformer transformer : transformers.values()) {
            request = transformer.remapInstallArtifacts(session, request);
        }
        return request;
    }

    @Override
    public DeployRequest remapDeployArtifacts(RepositorySystemSession session, DeployRequest request) {
        for (PomArtifactTransformer transformer : transformers.values()) {
            request = transformer.remapDeployArtifacts(session, request);
        }
        return request;
    }

    @Override
    public void injectTransformedArtifacts(RepositorySystemSession repositorySession, MavenProject currentProject)
            throws IOException {
        for (PomArtifactTransformer transformer : transformers.values()) {
            transformer.injectTransformedArtifacts(repositorySession, currentProject);
        }
    }
}
