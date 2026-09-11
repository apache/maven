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
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.artifact.ProjectArtifact;
import org.eclipse.aether.util.artifact.ArtifactIdUtils;

public class DefaultProject implements Project {

    private final InternalMavenSession session;
    private final MavenProject project;
    private final Packaging packaging;

    public DefaultProject(InternalMavenSession session, MavenProject project) {
        this.session = session;
        this.project = project;
        ClassLoader ttcl = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(project.getClassRealm());
            this.packaging = session.requirePackaging(project.getPackaging());
        } finally {
            Thread.currentThread().setContextClassLoader(ttcl);
        }
    }

    public InternalMavenSession getSession() {
        return session;
    }

    public MavenProject getProject() {
        return project;
    }

    @Nonnull
    @Override
    public String groupId() {
        return project.getGroupId();
    }

    @Nonnull
    @Override
    public String artifactId() {
        return project.getArtifactId();
    }

    @Nonnull
    @Override
    public String version() {
        return project.getVersion();
    }

    @Nonnull
    @Override
    public List<ProducedArtifact> artifacts() {
        org.eclipse.aether.artifact.Artifact pomArtifact = RepositoryUtils.toArtifact(new ProjectArtifact(project));
        org.eclipse.aether.artifact.Artifact projectArtifact = RepositoryUtils.toArtifact(project.getArtifact());

        ArrayList<ProducedArtifact> result = new ArrayList<>(2);
        result.add(session.getArtifact(ProducedArtifact.class, pomArtifact));
        if (!ArtifactIdUtils.equalsVersionlessId(pomArtifact, projectArtifact)) {
            result.add(session.getArtifact(ProducedArtifact.class, projectArtifact));
        }
        return Collections.unmodifiableList(result);
    }

    @Nonnull
    @Override
    public Packaging packaging() {
        return packaging;
    }

    @Nonnull
    @Override
    public Model model() {
        return project.getModel().getDelegate();
    }

    @Nonnull
    @Override
    public Path pomPath() {
        return Objects.requireNonNull(project.getFile(), "pomPath cannot be null")
                .toPath();
    }

    @Nonnull
    @Override
    public Path basedir() {
        return Objects.requireNonNull(project.getBasedir(), "basedir cannot be null")
                .toPath();
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
        return project.getRootDirectory();
    }

    @Override
    public Optional<Project> parent() {
        MavenProject parent = project.getParent();
        return Optional.ofNullable(session.getProject(parent));
    }

    @Override
    @Nonnull
    public List<Profile> declaredProfiles() {
        return model().getProfiles();
    }

    @Override
    @Nonnull
    public List<Profile> effectiveProfiles() {
        return Stream.iterate(this.project, Objects::nonNull, MavenProject::getParent)
                .flatMap(project -> project.getModel().getDelegate().getProfiles().stream())
                .toList();
    }

    @Override
    @Nonnull
    public List<Profile> declaredActiveProfiles() {
        return project.getActiveProfiles().stream()
                .map(org.apache.maven.model.Profile::getDelegate)
                .toList();
    }

    @Override
    @Nonnull
    public List<Profile> effectiveActiveProfiles() {
        return Stream.iterate(this.project, Objects::nonNull, MavenProject::getParent)
                .flatMap(project -> project.getActiveProfiles().stream())
                .map(org.apache.maven.model.Profile::getDelegate)
                .toList();
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
