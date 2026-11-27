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
package org.apache.maven.internal.impl;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.maven.RepositoryUtils;
import org.apache.maven.api.DependencyCoordinates;
import org.apache.maven.api.DependencyScope;
import org.apache.maven.api.Exclusion;
import org.apache.maven.api.Packaging;
import org.apache.maven.api.ProducedArtifact;
import org.apache.maven.api.Project;
import org.apache.maven.api.Type;
import org.apache.maven.api.VersionConstraint;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.annotations.Nullable;
import org.apache.maven.api.model.DependencyManagement;
import org.apache.maven.api.model.Model;
import org.apache.maven.api.model.Profile;
import org.apache.maven.impl.MappedCollection;
import org.apache.maven.impl.MappedList;
import org.apache.maven.impl.MutableProject;
import org.apache.maven.model.root.RootLocator;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.SourceQueries;
import org.apache.maven.project.artifact.ProjectArtifact;
import org.eclipse.aether.util.artifact.ArtifactIdUtils;

import static java.util.Objects.requireNonNull;

public class DefaultProject implements Project {

    private final InternalMavenSession session;
    private final String groupId;
    private final String artifactId;
    private final String version;
    private final Packaging packaging;
    private final List<org.eclipse.aether.artifact.Artifact> artifacts;
    private final boolean producesMainArtifact;
    private final Model model;
    private final Path pomPath;
    private final Path basedir;
    private final Path rootDirectory;
    private final Project parent;
    private final List<Profile> effectiveProfiles;
    private final List<Profile> declaredActiveProfiles;
    private final List<Profile> effectiveActiveProfiles;
    private final MutableProject mutableProject;

    public DefaultProject(InternalMavenSession session, MavenProject project) {
        this(session, project, null);
    }

    private DefaultProject(InternalMavenSession session, MavenProject project, MutableProject mutableProject) {
        this.session = requireNonNull(session, "session cannot be null");
        requireNonNull(project, "project cannot be null");
        this.groupId = project.getGroupId();
        this.artifactId = project.getArtifactId();
        this.version = project.getVersion();
        ClassLoader ttcl = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(project.getClassRealm());
            this.packaging = session.requirePackaging(project.getPackaging());
        } finally {
            Thread.currentThread().setContextClassLoader(ttcl);
        }
        this.producesMainArtifact = packaging != null
                && packaging.producesMainArtifact()
                && !SourceQueries.usesModuleSourceHierarchy(project);
        ArrayList<org.eclipse.aether.artifact.Artifact> artifacts = new ArrayList<>(2);
        if (hasCoordinates(project)) {
            org.eclipse.aether.artifact.Artifact pomArtifact = RepositoryUtils.toArtifact(new ProjectArtifact(project));
            artifacts.add(pomArtifact);
            if (producesMainArtifact) {
                Type type = packaging.type();
                org.eclipse.aether.artifact.Artifact projectArtifact = project.getArtifact() != null
                        ? RepositoryUtils.toArtifact(project.getArtifact())
                        : new org.eclipse.aether.artifact.DefaultArtifact(
                                groupId,
                                artifactId,
                                type.classifier() != null ? type.classifier() : "",
                                type.extension(),
                                version);
                if (!ArtifactIdUtils.equalsVersionlessId(pomArtifact, projectArtifact)) {
                    artifacts.add(projectArtifact);
                }
            }
        }
        this.artifacts = List.copyOf(artifacts);
        this.model = project.getModel().getDelegate();
        this.pomPath = project.getFile() != null ? project.getFile().toPath() : null;
        this.basedir = project.getBasedir() != null ? project.getBasedir().toPath() : null;
        this.rootDirectory = rootDirectory(project);
        this.parent = session.getProject(project.getParent());
        this.effectiveProfiles = Stream.iterate(project, Objects::nonNull, MavenProject::getParent)
                .flatMap(current -> current.getModel().getDelegate().getProfiles().stream())
                .toList();
        this.declaredActiveProfiles = project.getActiveProfiles().stream()
                .map(org.apache.maven.model.Profile::getDelegate)
                .toList();
        this.effectiveActiveProfiles = Stream.iterate(project, Objects::nonNull, MavenProject::getParent)
                .flatMap(current -> current.getActiveProfiles().stream())
                .map(org.apache.maven.model.Profile::getDelegate)
                .toList();
        this.mutableProject = mutableProject != null ? mutableProject : new MutableProject(this);
    }

    private static Path rootDirectory(MavenProject project) {
        try {
            return project.getRootDirectory();
        } catch (IllegalStateException e) {
            return null;
        }
    }

    private static boolean hasCoordinates(MavenProject project) {
        return project.getGroupId() != null
                && !project.getGroupId().isEmpty()
                && project.getArtifactId() != null
                && !project.getArtifactId().isEmpty()
                && project.getVersion() != null
                && !project.getVersion().isEmpty();
    }

    public InternalMavenSession getSession() {
        return session;
    }

    MutableProject mutableProject() {
        return mutableProject;
    }

    DefaultProject newSnapshot(MavenProject project) {
        return new DefaultProject(session, project, mutableProject);
    }

    @Nonnull
    @Override
    public String groupId() {
        return groupId;
    }

    @Nonnull
    @Override
    public String artifactId() {
        return artifactId;
    }

    @Nonnull
    @Override
    public String version() {
        return version;
    }

    @Nonnull
    @Override
    public List<ProducedArtifact> artifacts() {
        return new MappedList<>(artifacts, artifact -> session.getArtifact(ProducedArtifact.class, artifact));
    }

    @Override
    public boolean producesMainArtifact() {
        return producesMainArtifact;
    }

    @Nonnull
    @Override
    public Packaging packaging() {
        return packaging;
    }

    @Nonnull
    @Override
    public Model model() {
        return model;
    }

    @Nonnull
    @Override
    public Path pomPath() {
        return Objects.requireNonNull(pomPath, "pomPath cannot be null");
    }

    @Nonnull
    @Override
    public Path basedir() {
        return Objects.requireNonNull(basedir, "basedir cannot be null");
    }

    @Nonnull
    @Override
    public List<DependencyCoordinates> dependencies() {
        return new MappedList<>(model().getDependencies(), this::toDependency);
    }

    @Nonnull
    @Override
    public List<DependencyCoordinates> managedDependencies() {
        DependencyManagement dependencyManagement = model().getDependencyManagement();
        if (dependencyManagement != null) {
            return new MappedList<>(dependencyManagement.getDependencies(), this::toDependency);
        }
        return Collections.emptyList();
    }

    @Override
    public boolean isTopProject() {
        return basedir().equals(getSession().getTopDirectory());
    }

    @Override
    public boolean isRootProject() {
        return basedir().equals(rootDirectory());
    }

    @Override
    public Path rootDirectory() {
        if (rootDirectory == null) {
            throw new IllegalStateException(RootLocator.UNABLE_TO_FIND_ROOT_PROJECT_MESSAGE);
        }
        return rootDirectory;
    }

    @Override
    public Optional<Project> parent() {
        return Optional.ofNullable(parent);
    }

    @Override
    @Nonnull
    public List<Profile> declaredProfiles() {
        return model().getProfiles();
    }

    @Override
    @Nonnull
    public List<Profile> effectiveProfiles() {
        return effectiveProfiles;
    }

    @Override
    @Nonnull
    public List<Profile> declaredActiveProfiles() {
        return declaredActiveProfiles;
    }

    @Override
    @Nonnull
    public List<Profile> effectiveActiveProfiles() {
        return effectiveActiveProfiles;
    }

    @Nonnull
    private DependencyCoordinates toDependency(org.apache.maven.api.model.Dependency dependency) {
        return new DependencyCoordinates() {
            @Override
            public String groupId() {
                return dependency.getGroupId();
            }

            @Override
            public String artifactId() {
                return dependency.getArtifactId();
            }

            @Override
            public String classifier() {
                String classifier = dependency.getClassifier();
                if (classifier == null || classifier.isEmpty()) {
                    classifier = type().getClassifier();
                    if (classifier == null) {
                        classifier = "";
                    }
                }
                return classifier;
            }

            @Override
            public VersionConstraint versionConstraint() {
                return session.parseVersionConstraint(dependency.getVersion());
            }

            @Override
            public String extension() {
                return type().getExtension();
            }

            @Override
            public Type type() {
                String type = dependency.getType();
                return session.requireType(type);
            }

            @Nonnull
            @Override
            public DependencyScope scope() {
                String scope = dependency.getScope();
                if (scope == null) {
                    scope = "";
                }
                return session.requireDependencyScope(scope);
            }

            @Override
            public Boolean optional() {
                return dependency.isOptional();
            }

            @Nonnull
            @Override
            public Collection<Exclusion> exclusions() {
                return new MappedCollection<>(dependency.getExclusions(), this::toExclusion);
            }

            private Exclusion toExclusion(org.apache.maven.api.model.Exclusion exclusion) {
                return new Exclusion() {
                    @Nullable
                    @Override
                    public String groupId() {
                        return exclusion.getGroupId();
                    }

                    @Nullable
                    @Override
                    public String artifactId() {
                        return exclusion.getArtifactId();
                    }
                };
            }
        };
    }
}
