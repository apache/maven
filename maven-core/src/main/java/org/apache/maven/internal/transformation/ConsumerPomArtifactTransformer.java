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

import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.xml.stream.XMLStreamException;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

import org.apache.maven.api.Repository;
import org.apache.maven.api.feature.Features;
import org.apache.maven.api.model.DistributionManagement;
import org.apache.maven.api.model.Model;
import org.apache.maven.api.model.ModelBase;
import org.apache.maven.api.model.Profile;
import org.apache.maven.model.building.DefaultModelBuildingRequest;
import org.apache.maven.model.building.ModelBuilder;
import org.apache.maven.model.building.ModelBuildingException;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.building.ModelBuildingResult;
import org.apache.maven.model.v4.MavenModelVersion;
import org.apache.maven.model.v4.MavenStaxWriter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.project.ProjectModelResolver;
import org.apache.maven.project.artifact.ProjectArtifact;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.RequestTrace;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.deployment.DeployRequest;
import org.eclipse.aether.impl.RemoteRepositoryManager;
import org.eclipse.aether.installation.InstallRequest;

/**
 * Consumer POM transformer.
 *
 * @since TBD
 */
@Singleton
@Named("consumer-pom")
public final class ConsumerPomArtifactTransformer {

    private static final String BOM_PACKAGING = "bom";

    public static final String POM_PACKAGING = "pom";

    private static final String CONSUMER_POM_CLASSIFIER = "consumer";

    private static final String BUILD_POM_CLASSIFIER = "build";

    private static final String NAMESPACE_FORMAT = "http://maven.apache.org/POM/%s";

    private static final String SCHEMA_LOCATION_FORMAT = "https://maven.apache.org/xsd/maven-%s.xsd";

    private final Set<Path> toDelete = new CopyOnWriteArraySet<>();

    private final ModelBuilder modelBuilder;
    private final Provider<RepositorySystem> repoSystem;
    private final Provider<RemoteRepositoryManager> repositoryManager;

    @Inject
    ConsumerPomArtifactTransformer(
            ModelBuilder modelBuilder,
            Provider<RepositorySystem> repoSystem,
            Provider<RemoteRepositoryManager> repositoryManager) {
        this.modelBuilder = modelBuilder;
        this.repoSystem = repoSystem;
        this.repositoryManager = repositoryManager;
    }

    public void injectTransformedArtifacts(MavenProject project, RepositorySystemSession session) throws IOException {
        if (project.getFile() == null) {
            // If there is no build POM there is no reason to inject artifacts for the consumer POM.
            return;
        }
        if (Features.buildConsumer(session.getUserProperties())) {
            Path buildDir =
                    project.getBuild() != null ? Paths.get(project.getBuild().getDirectory()) : null;
            if (buildDir != null) {
                Files.createDirectories(buildDir);
            }
            Path consumer = buildDir != null
                    ? Files.createTempFile(buildDir, CONSUMER_POM_CLASSIFIER + "-", ".pom")
                    : Files.createTempFile(CONSUMER_POM_CLASSIFIER + "-", ".pom");
            deferDeleteFile(consumer);

            project.addAttachedArtifact(createConsumerPomArtifact(project, consumer, session));
        } else if (project.getModel().getDelegate().isRoot()) {
            throw new IllegalStateException(
                    "The use of the root attribute on the model requires the buildconsumer feature to be active");
        }
    }

    public ConsumerPomArtifact createConsumerPomArtifact(
            MavenProject project, Path consumer, RepositorySystemSession session) {
        return new ConsumerPomArtifact(project, consumer, session);
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

    public InstallRequest remapInstallArtifacts(RepositorySystemSession session, InstallRequest request) {
        if (Features.buildConsumer(session.getUserProperties()) && consumerPomPresent(request.getArtifacts())) {
            request.setArtifacts(replacePom(request.getArtifacts()));
        }
        return request;
    }

    public DeployRequest remapDeployArtifacts(RepositorySystemSession session, DeployRequest request) {
        if (Features.buildConsumer(session.getUserProperties()) && consumerPomPresent(request.getArtifacts())) {
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
        List<Artifact> mains = new ArrayList<>();
        for (Artifact artifact : artifacts) {
            if ("pom".equals(artifact.getExtension()) || artifact.getExtension().startsWith("pom.")) {
                if (CONSUMER_POM_CLASSIFIER.equals(artifact.getClassifier())) {
                    consumers.add(artifact);
                } else if ("".equals(artifact.getClassifier())) {
                    mains.add(artifact);
                }
            }
        }
        if (!mains.isEmpty() && !consumers.isEmpty()) {
            ArrayList<Artifact> result = new ArrayList<>(artifacts);
            for (Artifact main : mains) {
                result.remove(main);
                result.add(new DefaultArtifact(
                        main.getGroupId(),
                        main.getArtifactId(),
                        BUILD_POM_CLASSIFIER,
                        main.getExtension(),
                        main.getVersion(),
                        main.getProperties(),
                        main.getFile()));
            }
            for (Artifact consumer : consumers) {
                result.remove(consumer);
                result.add(new DefaultArtifact(
                        consumer.getGroupId(),
                        consumer.getArtifactId(),
                        "",
                        consumer.getExtension(),
                        consumer.getVersion(),
                        consumer.getProperties(),
                        consumer.getFile()));
            }
            artifacts = result;
        }
        return artifacts;
    }

    /**
     * Consumer POM is transformed from original POM.
     */
    class ConsumerPomArtifact extends TransformedArtifact {

        private MavenProject project;
        private RepositorySystemSession session;

        ConsumerPomArtifact(MavenProject mavenProject, Path target, RepositorySystemSession session) {
            super(
                    new ProjectArtifact(mavenProject),
                    () -> mavenProject.getFile().toPath(),
                    CONSUMER_POM_CLASSIFIER,
                    "pom",
                    target);
            this.project = mavenProject;
            this.session = session;
        }

        @Override
        public void transform(Path src, Path dest) {
            DefaultModelBuildingRequest request = new DefaultModelBuildingRequest();
            request.setPomFile(src.toFile());
            request.setValidationLevel(ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL);
            request.setLocationTracking(false);
            request.setModelResolver(new ProjectModelResolver(
                    session,
                    new RequestTrace(null),
                    repoSystem.get(),
                    repositoryManager.get(),
                    project.getRemoteProjectRepositories(),
                    ProjectBuildingRequest.RepositoryMerging.POM_DOMINANT,
                    null));
            request.setTransformerContextBuilder(modelBuilder.newTransformerContextBuilder());
            Properties props = new Properties();
            props.putAll(session.getSystemProperties());
            props.putAll(session.getUserProperties());
            request.setSystemProperties(props);
            try {
                ModelBuildingResult result = modelBuilder.build(request);
                transform(src, dest, result.getEffectiveModel().getDelegate());
            } catch (ModelBuildingException e) {
                throw new RuntimeException(e);
            }
        }

        void transform(Path src, Path dest, Model model) {
            String version;

            String packaging = model.getPackaging();
            if (POM_PACKAGING.equals(packaging)) {
                // raw to consumer transform
                model = model.withRoot(false).withModules(null);
                if (model.getParent() != null) {
                    model = model.withParent(model.getParent().withRelativePath(null));
                }

                if (!model.isPreserveModelVersion()) {
                    model = model.withPreserveModelVersion(false);
                    version = new MavenModelVersion().getModelVersion(model);
                    model = model.withModelVersion(version);
                } else {
                    version = model.getModelVersion();
                }
            } else {
                Model.Builder builder = prune(
                        Model.newBuilder(model, true)
                                .preserveModelVersion(false)
                                .root(false)
                                .parent(null)
                                .build(null),
                        model);
                boolean isBom = BOM_PACKAGING.equals(packaging);
                if (isBom) {
                    builder.packaging(POM_PACKAGING);
                }
                builder.profiles(model.getProfiles().stream()
                        .map(p -> prune(Profile.newBuilder(p, true), p).build())
                        .collect(Collectors.toList()));
                model = builder.build();
                version = new MavenModelVersion().getModelVersion(model);
                model = model.withModelVersion(version);
            }

            try {
                Files.createDirectories(dest.getParent());
                try (Writer w = Files.newBufferedWriter(dest)) {
                    MavenStaxWriter writer = new MavenStaxWriter();
                    writer.setNamespace(String.format(NAMESPACE_FORMAT, version));
                    writer.setSchemaLocation(String.format(SCHEMA_LOCATION_FORMAT, version));
                    writer.setAddLocationInformation(false);
                    writer.write(w, model);
                }
            } catch (XMLStreamException | IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static <T extends ModelBase.Builder> T prune(T builder, ModelBase model) {
        builder.properties(null).reporting(null);
        if (model.getDistributionManagement() != null
                && model.getDistributionManagement().getRelocation() != null) {
            // keep relocation only
            builder.distributionManagement(DistributionManagement.newBuilder()
                    .relocation(model.getDistributionManagement().getRelocation())
                    .build());
        }
        // only keep repositories others than 'central'
        builder.pluginRepositories(pruneRepositories(model.getPluginRepositories()));
        builder.repositories(pruneRepositories(model.getRepositories()));
        return builder;
    }

    private static List<org.apache.maven.api.model.Repository> pruneRepositories(
            List<org.apache.maven.api.model.Repository> repositories) {
        return repositories.stream()
                .filter(r -> !Repository.CENTRAL_ID.equals(r.getId()))
                .collect(Collectors.toList());
    }
}
