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

import java.io.File;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.apache.maven.RepositoryUtils;
import org.apache.maven.api.Artifact;
import org.apache.maven.api.DependencyCoordinate;
import org.apache.maven.api.Exclusion;
import org.apache.maven.api.Project;
import org.apache.maven.api.RemoteRepository;
import org.apache.maven.api.Scope;
import org.apache.maven.api.Type;
import org.apache.maven.api.VersionRange;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.annotations.Nullable;
import org.apache.maven.api.model.DependencyManagement;
import org.apache.maven.api.model.Model;
import org.apache.maven.api.services.ArtifactManager;
import org.apache.maven.api.services.TypeRegistry;
import org.apache.maven.project.MavenProject;

public class DefaultProject implements Project {

    private final AbstractSession session;
    private final MavenProject project;

    public DefaultProject(AbstractSession session, MavenProject project) {
        this.session = session;
        this.project = project;
    }

    public AbstractSession getSession() {
        return session;
    }

    public MavenProject getProject() {
        return project;
    }

    @Nonnull
    @Override
    public String getGroupId() {
        return project.getGroupId();
    }

    @Nonnull
    @Override
    public String getArtifactId() {
        return project.getArtifactId();
    }

    @Nonnull
    @Override
    public String getVersion() {
        return project.getVersion();
    }

    @Nonnull
    @Override
    public Artifact getArtifact() {
        org.eclipse.aether.artifact.Artifact resolverArtifact = RepositoryUtils.toArtifact(project.getArtifact());
        Artifact artifact = session.getArtifact(resolverArtifact);
        Path path =
                resolverArtifact.getFile() != null ? resolverArtifact.getFile().toPath() : null;
        session.getService(ArtifactManager.class).setPath(artifact, path);
        return artifact;
    }

    @Nonnull
    @Override
    public String getPackaging() {
        return project.getPackaging();
    }

    @Nonnull
    @Override
    public Model getModel() {
        return project.getModel().getDelegate();
    }

    @Nonnull
    @Override
    public Optional<Path> getPomPath() {
        File file = project.getFile();
        return Optional.ofNullable(file).map(File::toPath);
    }

    @Nonnull
    @Override
    public List<DependencyCoordinate> getDependencies() {
        return new MappedList<>(getModel().getDependencies(), this::toDependency);
    }

    @Nonnull
    @Override
    public List<DependencyCoordinate> getManagedDependencies() {
        DependencyManagement dependencyManagement = getModel().getDependencyManagement();
        if (dependencyManagement != null) {
            return new MappedList<>(dependencyManagement.getDependencies(), this::toDependency);
        }
        return Collections.emptyList();
    }

    @Override
    public boolean isExecutionRoot() {
        return project.isExecutionRoot();
    }

    @Override
    public Optional<Project> getParent() {
        MavenProject parent = project.getParent();
        return parent != null ? Optional.of(session.getProject(parent)) : Optional.empty();
    }

    @Override
    public List<RemoteRepository> getRemoteProjectRepositories() {
        return new MappedList<>(project.getRemoteProjectRepositories(), session::getRemoteRepository);
    }

    @Override
    public List<RemoteRepository> getRemotePluginRepositories() {
        return new MappedList<>(project.getRemotePluginRepositories(), session::getRemoteRepository);
    }

    @Nonnull
    private DependencyCoordinate toDependency(org.apache.maven.api.model.Dependency dependency) {
        return new DependencyCoordinate() {
            @Override
            public String getGroupId() {
                return dependency.getGroupId();
            }

            @Override
            public String getArtifactId() {
                return dependency.getArtifactId();
            }

            @Override
            public String getClassifier() {
                return dependency.getClassifier();
            }

            @Override
            public VersionRange getVersion() {
                return session.parseVersionRange(dependency.getVersion());
            }

            @Override
            public String getExtension() {
                return getType().getExtension();
            }

            @Override
            public Type getType() {
                String type = dependency.getType();
                return session.getService(TypeRegistry.class).getType(type);
            }

            @Nonnull
            @Override
            public Scope getScope() {
                return Scope.get(dependency.getScope());
            }

            @Override
            public Boolean getOptional() {
                return dependency.isOptional();
            }

            @Nonnull
            @Override
            public Collection<Exclusion> getExclusions() {
                return new MappedCollection<>(dependency.getExclusions(), this::toExclusion);
            }

            private Exclusion toExclusion(org.apache.maven.api.model.Exclusion exclusion) {
                return new Exclusion() {
                    @Nullable
                    @Override
                    public String getGroupId() {
                        return exclusion.getGroupId();
                    }

                    @Nullable
                    @Override
                    public String getArtifactId() {
                        return exclusion.getArtifactId();
                    }
                };
            }
        };
    }
}
