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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import org.apache.maven.api.feature.Features;
import org.apache.maven.api.services.ModelBuilderException;
import org.apache.maven.api.services.ModelSource;
import org.apache.maven.api.services.Source;
import org.apache.maven.api.services.Sources;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.artifact.ProjectArtifact;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.deployment.DeployRequest;
import org.eclipse.aether.installation.InstallRequest;
import org.eclipse.sisu.PreDestroy;

/**
 * Consumer POM transformer.
 *
 * @since TBD
 */
@Singleton
@Named
class ConsumerPomArtifactTransformer extends TransformerSupport {
    static final String CONSUMER_POM_CLASSIFIER = "consumer";

    private static final String CONSUMER_POM_FULL_CLASSIFIER = "consumer-full";

    private static final String BUILD_POM_CLASSIFIER = "build";

    private final Set<Path> toDelete = new CopyOnWriteArraySet<>();
    private final Map<Path, byte[]> consumerFullCache = new ConcurrentHashMap<>();
    private final Set<Path> consumerFullPaths = ConcurrentHashMap.newKeySet();

    private final PomBuilder builder;

    @Inject
    ConsumerPomArtifactTransformer(PomBuilder builder) {
        this.builder = builder;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void injectTransformedArtifacts(RepositorySystemSession session, MavenProject project) throws IOException {
        if (project.getFile() == null) {
            // If there is no build POM there is no reason to inject artifacts for the consumer POM.
            return;
        }
        if (Features.consumerPom(session.getConfigProperties())) {
            Path buildDir =
                    project.getBuild() != null ? Paths.get(project.getBuild().getDirectory()) : null;
            if (buildDir != null) {
                Files.createDirectories(buildDir);
            }
            Path consumer = buildDir != null
                    ? Files.createTempFile(buildDir, CONSUMER_POM_CLASSIFIER + "-", ".pom")
                    : Files.createTempFile(CONSUMER_POM_CLASSIFIER + "-", ".pom");
            deferDeleteFile(consumer);

            Path consumerFull = buildDir != null
                    ? Files.createTempFile(buildDir, CONSUMER_POM_FULL_CLASSIFIER + "-", ".pom")
                    : Files.createTempFile(CONSUMER_POM_FULL_CLASSIFIER + "-", ".pom");
            deferDeleteFile(consumerFull);
            consumerFullPaths.add(consumerFull.toAbsolutePath());

            project.addAttachedArtifact(createConsumerPomArtifact(project, consumer, session));
            project.addAttachedArtifact(
                    createConsumerPomArtifact(project, consumerFull, session, CONSUMER_POM_FULL_CLASSIFIER));
        } else if (project.getModel().getDelegate().isRoot()) {
            throw new IllegalStateException(
                    "The use of the root attribute on the model requires the buildconsumer feature to be active");
        }
    }

    TransformedArtifact createConsumerPomArtifact(
            MavenProject project, Path consumer, RepositorySystemSession session) {
        return createConsumerPomArtifact(project, consumer, session, CONSUMER_POM_CLASSIFIER);
    }

    TransformedArtifact createConsumerPomArtifact(
            MavenProject project, Path consumer, RepositorySystemSession session, String classifier) {
        Path actual = project.getFile().toPath();
        Path parent = project.getBaseDirectory();
        ModelSource source = new ModelSource() {
            @Override
            public Path getPath() {
                return actual;
            }

            @Override
            public InputStream openStream() throws IOException {
                return Files.newInputStream(actual);
            }

            @Override
            public String getLocation() {
                return actual.toString();
            }

            @Override
            public Source resolve(String relative) {
                return Sources.buildSource(actual.resolve(relative));
            }

            @Override
            public ModelSource resolve(ModelLocator modelLocator, String relative) {
                String norm = relative.replace('\\', File.separatorChar).replace('/', File.separatorChar);
                Path path = parent.resolve(norm);
                Path relatedPom = modelLocator.locateExistingPom(path);
                if (relatedPom != null) {
                    return Sources.buildSource(relatedPom);
                }
                return null;
            }
        };
        return new TransformedArtifact(
                this, project, consumer, session, new ProjectArtifact(project), () -> source, classifier, "pom");
    }

    @Override
    public void transform(MavenProject project, RepositorySystemSession session, ModelSource src, Path tgt)
            throws ModelBuilderException, XMLStreamException, IOException {
        if (consumerFullPaths.contains(tgt.toAbsolutePath())) {
            // This is the consumer-full artifact — check if we have cached content
            byte[] cached = consumerFullCache.remove(tgt.toAbsolutePath());
            if (cached != null) {
                Files.write(tgt, cached);
            }
            // If no cached content, leave the file empty (will be skipped during deploy)
        } else {
            // This is the main consumer artifact
            PomBuilder.ConsumerPomBuildResult result = builder.buildConsumerPoms(session, project, src);
            write(result.main(), tgt);
            if (result.consumer() != null) {
                // Cache the consumer-full content for later
                Path consumerFullPath = findConsumerFullPath(tgt);
                if (consumerFullPath != null) {
                    java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                    try (java.io.OutputStream os = baos) {
                        new org.apache.maven.model.v4.MavenStaxWriter().write(os, result.consumer());
                    }
                    consumerFullCache.put(consumerFullPath, baos.toByteArray());
                }
            }
        }
    }

    private Path findConsumerFullPath(Path mainPath) {
        // Find the consumer-full path that's in the same directory as the main path
        Path dir = mainPath.getParent();
        for (Path p : consumerFullPaths) {
            if (p.getParent().equals(dir)) {
                return p;
            }
        }
        return null;
    }

    boolean hasConsumerFullContent(Path path) {
        return consumerFullCache.containsKey(path.toAbsolutePath())
                || (Files.exists(path) && path.toFile().length() > 0);
    }

    private void deferDeleteFile(Path generatedFile) {
        toDelete.add(generatedFile.toAbsolutePath());
    }

    @PreDestroy
    private void doDeleteFiles() {
        for (Path file : toDelete) {
            try {
                Files.delete(file);
            } catch (IOException e) {
                // ignore, we did our best...
            }
        }
    }

    @Override
    public InstallRequest remapInstallArtifacts(RepositorySystemSession session, InstallRequest request) {
        if (consumerPomPresent(request.getArtifacts())) {
            request.setArtifacts(replacePom(request.getArtifacts()));
        }
        return request;
    }

    @Override
    public DeployRequest remapDeployArtifacts(RepositorySystemSession session, DeployRequest request) {
        if (consumerPomPresent(request.getArtifacts())) {
            request.setArtifacts(replacePom(request.getArtifacts()));
        }
        return request;
    }

    private boolean consumerPomPresent(Collection<Artifact> artifacts) {
        return artifacts.stream()
                .anyMatch(a -> "pom".equals(a.getExtension()) && CONSUMER_POM_CLASSIFIER.equals(a.getClassifier()));
    }

    private Collection<Artifact> replacePom(Collection<Artifact> artifacts) {
        List<Artifact> consumers = new ArrayList<>();
        List<Artifact> consumerFulls = new ArrayList<>();
        List<Artifact> mains = new ArrayList<>();
        for (Artifact artifact : artifacts) {
            if ("pom".equals(artifact.getExtension()) || artifact.getExtension().startsWith("pom.")) {
                if (CONSUMER_POM_FULL_CLASSIFIER.equals(artifact.getClassifier())) {
                    consumerFulls.add(artifact);
                } else if (CONSUMER_POM_CLASSIFIER.equals(artifact.getClassifier())) {
                    consumers.add(artifact);
                } else if ("".equals(artifact.getClassifier())) {
                    mains.add(artifact);
                }
            }
        }
        if (!mains.isEmpty() && !consumers.isEmpty()) {
            ArrayList<Artifact> result = new ArrayList<>(artifacts);
            // original POM → build classifier
            for (Artifact main : mains) {
                result.remove(main);
                result.add(new DefaultArtifact(
                        main.getGroupId(),
                        main.getArtifactId(),
                        BUILD_POM_CLASSIFIER,
                        main.getExtension(),
                        main.getVersion(),
                        main.getProperties(),
                        main.getPath()));
            }
            // consumer POM → main (no classifier)
            for (Artifact consumer : consumers) {
                result.remove(consumer);
                result.add(new DefaultArtifact(
                        consumer.getGroupId(),
                        consumer.getArtifactId(),
                        "",
                        consumer.getExtension(),
                        consumer.getVersion(),
                        consumer.getProperties(),
                        consumer.getPath()));
            }
            // consumer-full POM → consumer classifier (only if it has content)
            for (Artifact consumerFull : consumerFulls) {
                result.remove(consumerFull);
                if (consumerFull.getPath() != null && hasConsumerFullContent(consumerFull.getPath())) {
                    result.add(new DefaultArtifact(
                            consumerFull.getGroupId(),
                            consumerFull.getArtifactId(),
                            CONSUMER_POM_CLASSIFIER,
                            consumerFull.getExtension(),
                            consumerFull.getVersion(),
                            consumerFull.getProperties(),
                            consumerFull.getPath()));
                }
            }
            artifacts = result;
        }
        return artifacts;
    }
}
