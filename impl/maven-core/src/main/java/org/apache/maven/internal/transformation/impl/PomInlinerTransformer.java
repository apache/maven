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

import javax.inject.Named;
import javax.inject.Singleton;
import javax.xml.stream.XMLStreamException;

import java.io.IOException;
import java.nio.file.Path;

import org.apache.maven.api.feature.Features;
import org.apache.maven.api.services.ModelBuilderException;
import org.apache.maven.internal.transformation.PomArtifactTransformer;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.deployment.DeployRequest;
import org.eclipse.aether.installation.InstallRequest;

/**
 * Inliner POM transformer.
 *
 * @since TBD
 */
@Singleton
@Named
class PomInlinerTransformer implements PomArtifactTransformer {
    @Override
    public InstallRequest remapInstallArtifacts(RepositorySystemSession session, InstallRequest request) {
        return request;
    }

    @Override
    public DeployRequest remapDeployArtifacts(RepositorySystemSession session, DeployRequest request) {
        return request;
    }

    @Override
    public void injectTransformedArtifacts(RepositorySystemSession session, MavenProject currentProject)
            throws IOException {
        if (!Features.consumerPom(session.getConfigProperties())) {
            // TODO
        }
    }

    @Override
    public void transform(MavenProject project, RepositorySystemSession session, Path src, Path tgt)
            throws ModelBuilderException, XMLStreamException, IOException {
        // TODO
    }
}
