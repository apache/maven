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
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.maven.api.feature.Features;
import org.apache.maven.api.model.Model;
import org.apache.maven.api.services.Interpolator;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.deployment.DeployRequest;
import org.eclipse.aether.installation.InstallRequest;

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
    public InstallRequest remapInstallArtifacts(RepositorySystemSession session, InstallRequest request) {
        return request.setArtifacts(replacePom(session, request.getArtifacts()));
    }

    @Override
    public DeployRequest remapDeployArtifacts(RepositorySystemSession session, DeployRequest request) {
        return request.setArtifacts(replacePom(session, request.getArtifacts()));
    }

    private Collection<Artifact> replacePom(RepositorySystemSession session, Collection<Artifact> artifacts) {
        Set<String> needsInlining = needsInlining(session);
        if (needsInlining.isEmpty()) {
            return artifacts;
        }
        Map<String, String> pomProperties = pomProperties(session);
        ArrayList<Artifact> newArtifacts = new ArrayList<>(artifacts.size());
        for (Artifact artifact : artifacts) {
            if ("pom".equals(artifact.getExtension())
                    && artifact.getClassifier().isEmpty()) {
                try {
                    Path tmpPom = Files.createTempFile("pom-inliner-", ".xml");
                    String originalPom = Files.readString(artifact.getPath());
                    String interpolatedPom = interpolator.interpolate(
                            originalPom,
                            property -> {
                                if (needsInlining.contains(property)) {
                                    // Check configProperties first (CLI -D args take precedence),
                                    // then fall back to POM-defined properties.
                                    Object value = session.getConfigProperties().get(property);
                                    if (value != null) {
                                        return (String) value;
                                    }
                                    return pomProperties.get(property);
                                }
                                return null;
                            },
                            false);
                    if (!Objects.equals(originalPom, interpolatedPom)) {
                        Files.writeString(tmpPom, interpolatedPom);
                        artifact = artifact.setPath(tmpPom);
                    }
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
            newArtifacts.add(artifact);
        }
        return newArtifacts;
    }

    @SuppressWarnings("unchecked")
    private Set<String> needsInlining(RepositorySystemSession session) {
        return (Set<String>) session.getData()
                .computeIfAbsent(
                        PomInlinerTransformer.class.getName() + ".needsInlining", ConcurrentHashMap::newKeySet);
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> pomProperties(RepositorySystemSession session) {
        return (Map<String, String>) session.getData()
                .computeIfAbsent(
                        PomInlinerTransformer.class.getName() + ".pomProperties", ConcurrentHashMap::new);
    }

    @Override
    public void injectTransformedArtifacts(RepositorySystemSession session, MavenProject project) throws IOException {
        if (!Features.consumerPom(session.getConfigProperties())) {
            try {
                Model model = read(project.getFile().toPath());
                String version = model.getVersion();
                if (version == null && model.getParent() != null) {
                    version = model.getParent().getVersion();
                }
                String newVersion;
                if (version != null) {
                    HashSet<String> usedProperties = new HashSet<>();
                    Map<String, String> pomProperties = pomProperties(session);
                    newVersion = interpolator.interpolate(version.trim(), property -> {
                        if (session.getConfigProperties().containsKey(property)) {
                            usedProperties.add(property);
                            return (String) session.getConfigProperties().get(property);
                        }
                        // CI-friendly version properties (revision, sha1, changelist) may be
                        // defined in the POM's <properties> section rather than passed via -D.
                        // In that case, fall back to the project's effective properties so the
                        // installed/deployed POM gets the literal version inlined for consumers.
                        String projectValue = project.getProperties().getProperty(property);
                        if (projectValue != null) {
                            usedProperties.add(property);
                            // Remember this value for replacePom(), which does not have a project ref.
                            pomProperties.put(property, projectValue);
                            return projectValue;
                        }
                        throw new IllegalArgumentException("Cannot inline property " + property);
                    });
                    if (!Objects.equals(version, newVersion)) {
                        needsInlining(session).addAll(usedProperties);
                    }
                }
            } catch (XMLStreamException e) {
                throw new IOException("Problem during inlining POM", e);
            }
        }
    }
}
