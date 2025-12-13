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

import javax.inject.Inject;
import javax.inject.Named;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Stream;

import org.apache.maven.RepositoryUtils;
import org.apache.maven.api.Language;
import org.apache.maven.api.ProducedArtifact;
import org.apache.maven.api.Project;
import org.apache.maven.api.ProjectScope;
import org.apache.maven.api.RemoteRepository;
import org.apache.maven.api.Service;
import org.apache.maven.api.SourceRoot;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.di.SessionScoped;
import org.apache.maven.api.services.ArtifactManager;
import org.apache.maven.api.services.ProjectManager;
import org.apache.maven.impl.InternalSession;
import org.apache.maven.impl.MappedList;
import org.apache.maven.impl.PropertiesAsMap;
import org.apache.maven.project.MavenProject;
import org.eclipse.sisu.Typed;

import static java.util.Objects.requireNonNull;
import static org.apache.maven.internal.impl.CoreUtils.map;

/**
 * This implementation of {@code ProjectManager} is explicitly bound to
 * both {@code ProjectManager} and {@code Service} interfaces so that it can be retrieved using
 * {@link InternalSession#getAllServices()}.
 */
@Named
@Typed({ProjectManager.class, Service.class})
@SessionScoped
public class DefaultProjectManager implements ProjectManager {

    private final InternalMavenSession session;
    private final ArtifactManager artifactManager;

    @Inject
    public DefaultProjectManager(InternalMavenSession session, ArtifactManager artifactManager) {
        this.session = session;
        this.artifactManager = artifactManager;
    }

    @Nonnull
    @Override
    public Optional<Path> getPath(@Nonnull Project project) {
        requireNonNull(project, "project" + " cannot be null");
        Optional<ProducedArtifact> mainArtifact = project.getMainArtifact();
        return mainArtifact.flatMap(artifactManager::getPath);
    }

    @Nonnull
    @Override
    public Collection<ProducedArtifact> getAttachedArtifacts(@Nonnull Project project) {
        requireNonNull(project, "project" + " cannot be null");
        Collection<ProducedArtifact> attached =
                map(getMavenProject(project).getAttachedArtifacts(), a -> getSession(project)
                        .getArtifact(ProducedArtifact.class, RepositoryUtils.toArtifact(a)));
        return Collections.unmodifiableCollection(attached);
    }

    @Override
    @Nonnull
    public Collection<ProducedArtifact> getAllArtifacts(@Nonnull Project project) {
        requireNonNull(project, "project cannot be null");
        ArrayList<ProducedArtifact> result = new ArrayList<>(2);
        result.addAll(project.getArtifacts());
        result.addAll(getAttachedArtifacts(project));
        return Collections.unmodifiableCollection(result);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void attachArtifact(@Nonnull Project project, @Nonnull ProducedArtifact artifact, @Nonnull Path path) {
        requireNonNull(project, "project cannot be null");
        requireNonNull(artifact, "artifact cannot be null");
        requireNonNull(path, "path cannot be null");
        if (artifact.getGroupId().isEmpty()
                || artifact.getArtifactId().isEmpty()
                || artifact.getBaseVersion().toString().isEmpty()) {
            artifact = session.createProducedArtifact(
                    artifact.getGroupId().isEmpty() ? project.getGroupId() : artifact.getGroupId(),
                    artifact.getArtifactId().isEmpty() ? project.getArtifactId() : artifact.getArtifactId(),
                    artifact.getBaseVersion().toString().isEmpty()
                            ? session.parseVersion(project.getVersion()).toString()
                            : artifact.getBaseVersion().toString(),
                    artifact.getClassifier(),
                    artifact.getExtension(),
                    null);
        }
        // Verify groupId and version, intentionally allow artifactId to differ as a project may be multi-module.
        String g1 = project.getGroupId();
        String g2 = artifact.getGroupId();
        String v1 = project.getVersion();
        String v2 = artifact.getBaseVersion().toString();
        if (!Objects.equals(g1, g2) || !Objects.equals(v1, v2)) {
            throw new IllegalArgumentException(
                    "The produced artifact must have the same groupId and version than the project it is attached to. Expecting "
                            + g1 + ':' + project.getArtifactId() + ':' + v1 + " but received "
                            + g2 + ':' + artifact.getArtifactId() + ':' + v2);
        }
        getMavenProject(project)
                .addAttachedArtifact(
                        RepositoryUtils.toArtifact(getSession(project).toArtifact(artifact)));
        artifactManager.setPath(artifact, path);
    }

    @Nonnull
    @Override
    public Collection<SourceRoot> getSourceRoots(@Nonnull Project project) {
        MavenProject prj = getMavenProject(requireNonNull(project, "project" + " cannot be null"));
        return prj.getSourceRoots();
    }

    @Nonnull
    @Override
    public Stream<SourceRoot> getEnabledSourceRoots(@Nonnull Project project, ProjectScope scope, Language language) {
        MavenProject prj = getMavenProject(requireNonNull(project, "project" + " cannot be null"));
        return prj.getEnabledSourceRoots(scope, language);
    }

    @Override
    public void addSourceRoot(@Nonnull Project project, @Nonnull SourceRoot source) {
        MavenProject prj = getMavenProject(requireNonNull(project, "project" + " cannot be null"));
        prj.addSourceRoot(requireNonNull(source, "source" + " cannot be null"));
    }

    @Override
    public void addSourceRoot(
            @Nonnull Project project,
            @Nonnull ProjectScope scope,
            @Nonnull Language language,
            @Nonnull Path directory) {
        MavenProject prj = getMavenProject(requireNonNull(project, "project" + " cannot be null"));
        prj.addSourceRoot(
                requireNonNull(scope, "scope" + " cannot be null"),
                requireNonNull(language, "language" + " cannot be null"),
                requireNonNull(directory, "directory" + " cannot be null"));
    }

    @Override
    @Nonnull
    public List<RemoteRepository> getRemoteProjectRepositories(@Nonnull Project project) {
        return Collections.unmodifiableList(new MappedList<>(
                getMavenProject(project).getRemoteProjectRepositories(), session::getRemoteRepository));
    }

    @Override
    @Nonnull
    public List<RemoteRepository> getRemotePluginRepositories(@Nonnull Project project) {
        return Collections.unmodifiableList(
                new MappedList<>(getMavenProject(project).getRemotePluginRepositories(), session::getRemoteRepository));
    }

    @Override
    public void setProperty(@Nonnull Project project, @Nonnull String key, String value) {
        Properties properties = getMavenProject(project).getProperties();
        if (value == null) {
            properties.remove(key);
        } else {
            properties.setProperty(key, value);
        }
    }

    @Override
    @Nonnull
    public Map<String, String> getProperties(@Nonnull Project project) {
        return Collections.unmodifiableMap(
                new PropertiesAsMap(getMavenProject(project).getProperties()));
    }

    @Override
    @Nonnull
    public Optional<Project> getExecutionProject(@Nonnull Project project) {
        // Session keep tracks of the Project per project id,
        // so we cannot use session.getProject(p) for forked projects
        // which are temporary clones
        return Optional.ofNullable(getMavenProject(project).getExecutionProject())
                .map(p -> new DefaultProject(session, p));
    }

    private MavenProject getMavenProject(Project project) {
        return ((DefaultProject) project).getProject();
    }

    private static InternalSession getSession(Project project) {
        return ((DefaultProject) project).getSession();
    }
}
