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
package org.apache.maven.internal.transformation;

import javax.xml.stream.XMLStreamException;

import java.io.IOException;
import java.nio.file.Path;

import org.apache.maven.api.services.ModelBuilderException;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.deployment.DeployRequest;
import org.eclipse.aether.installation.InstallRequest;

/**
 * Maven POM transformer.
 * TODO: rename this interface to "Transformer" as it can do much more than just transform POM.
 * @since TBD
 */
public interface PomArtifactTransformer {
    InstallRequest remapInstallArtifacts(RepositorySystemSession session, InstallRequest request);

    DeployRequest remapDeployArtifacts(RepositorySystemSession session, DeployRequest request);

    void injectTransformedArtifacts(RepositorySystemSession session, MavenProject currentProject) throws IOException;

    void transform(MavenProject project, RepositorySystemSession session, Path src, Path tgt)
            throws ModelBuilderException, XMLStreamException, IOException;
}
