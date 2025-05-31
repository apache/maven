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

import javax.xml.stream.XMLStreamException;

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.maven.api.model.Model;
import org.apache.maven.api.services.ModelBuilderException;
import org.apache.maven.internal.transformation.PomArtifactTransformer;
import org.apache.maven.model.v4.MavenStaxReader;
import org.apache.maven.model.v4.MavenStaxWriter;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.deployment.DeployRequest;
import org.eclipse.aether.installation.InstallRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Support class.
 */
abstract class TransformerSupport implements PomArtifactTransformer {
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public InstallRequest remapInstallArtifacts(RepositorySystemSession session, InstallRequest request) {
        return request;
    }

    @Override
    public DeployRequest remapDeployArtifacts(RepositorySystemSession session, DeployRequest request) {
        return request;
    }

    @Override
    public void injectTransformedArtifacts(RepositorySystemSession session, MavenProject project) throws IOException {}

    @Override
    public void transform(MavenProject project, RepositorySystemSession session, Path src, Path tgt)
            throws ModelBuilderException, XMLStreamException, IOException {
        throw new IllegalStateException("This transformer does not use this call.");
    }

    protected static final String NAMESPACE_FORMAT = "http://maven.apache.org/POM/%s";

    protected static final String SCHEMA_LOCATION_FORMAT = "https://maven.apache.org/xsd/maven-%s.xsd";

    protected Model read(Path src) throws IOException, XMLStreamException {
        MavenStaxReader reader = new MavenStaxReader();
        try (InputStream is = Files.newInputStream(src)) {
            return reader.read(is, false, null);
        }
    }

    protected void write(Model model, Path dest) throws IOException, XMLStreamException {
        String version = model.getModelVersion();
        Files.createDirectories(dest.getParent());
        try (Writer w = Files.newBufferedWriter(dest)) {
            MavenStaxWriter writer = new MavenStaxWriter();
            writer.setNamespace(String.format(NAMESPACE_FORMAT, version));
            writer.setSchemaLocation(String.format(SCHEMA_LOCATION_FORMAT, version));
            writer.setAddLocationInformation(false);
            writer.write(w, model);
        }
    }
}
