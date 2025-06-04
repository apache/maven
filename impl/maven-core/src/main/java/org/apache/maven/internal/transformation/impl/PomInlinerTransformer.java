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
import javax.xml.stream.XMLStreamException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import org.apache.maven.api.feature.Features;
import org.apache.maven.api.model.Model;
import org.apache.maven.api.services.Interpolator;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystemSession;

import static java.util.Objects.requireNonNull;

/**
 * Inliner POM transformer. The goal of this transformer is to fix Maven 3 issue about emitting (installing, deploying)
 * unusable POMs when using CI Friendly Versions.
 *
 * @since TBD
 */
@Singleton
@Named
class PomInlinerTransformer extends TransformerSupport {
    private final Interpolator interpolator;

    @Inject
    PomInlinerTransformer(Interpolator interpolator) {
        this.interpolator = requireNonNull(interpolator);
    }

    @Override
    public void injectTransformedArtifacts(RepositorySystemSession session, MavenProject project) throws IOException {
        if (!Features.consumerPom(session.getConfigProperties())) {
            try {
                Model model = read(project.getFile().toPath());
                boolean parentVersion = false;
                String version = model.getVersion();
                if (version == null && model.getParent() != null) {
                    parentVersion = true;
                    version = model.getParent().getVersion();
                }
                String newVersion;
                if (version != null) {
                    newVersion = interpolator.interpolate(version.trim(), property -> {
                        if (!session.getConfigProperties().containsKey(property)) {
                            throw new IllegalArgumentException("Cannot inline property " + property);
                        }
                        return (String) session.getConfigProperties().get(property);
                    });
                    if (!Objects.equals(version, newVersion)) {
                        if (parentVersion) {
                            model = model.withParent(model.getParent().withVersion(newVersion));
                        } else {
                            model = model.withVersion(newVersion);
                        }
                        Path tmpPom = Files.createTempFile(
                                project.getArtifactId() + "-" + project.getVersion() + "-", ".xml");
                        write(model, tmpPom);
                        project.setFile(tmpPom.toFile());
                    }
                }
            } catch (XMLStreamException e) {
                throw new IOException("Problem during inlining POM", e);
            }
        }
    }
}
