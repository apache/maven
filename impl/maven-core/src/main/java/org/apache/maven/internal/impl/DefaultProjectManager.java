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
        // Verify groupId and version, intentionally allow artifactId to differ as Maven project may be
        // multi-module with modular sources structure that provide module names used as artifactIds.
        String g1 = project.getGroupId();
        String a1 = project.getArtifactId();
        String v1 = project.getVersion();
        String g2 = artifact.getGroupId();
        String a2 = artifact.getArtifactId();
        String v2 = artifact.getBaseVersion().toString();

        // ArtifactId may differ only for multi-module projects, in which case
        // it must match the module name from a source root in modular sources.
        boolean isMultiModule = false;
        boolean validArtifactId = Objects.equals(a1, a2);
        for (SourceRoot sr : getSourceRoots(project)) {
            Optional<String> moduleName = sr.module();
            if (moduleName.isPresent()) {
                isMultiModule = true;
                if (moduleName.get().equals(a2)) {
                    validArtifactId = true;
                    break;
                }
            }
        }
        boolean isSameGroupAndVersion = Objects.equals(g1, g2) && Objects.equals(v1, v2);
        if (!(isSameGroupAndVersion && validArtifactId)) {
            String message;
            if (isMultiModule) {
                // Multi-module project: artifactId may match any declared module name
                message = String.format(
                        "Cannot attach artifact to project: groupId and version must match the project, "
                                + "and artifactId must match either the project or a declared module name.%n"
                                + "  Project coordinates:  %s:%s:%s%n"
                                + "  Artifact coordinates: %s:%s:%s%n",
                        g1, a1, v1, g2, a2, v2);
                if (isSameGroupAndVersion) {
                    message += String.format(
                            "  Hint: The artifactId '%s' does not match the project artifactId '%s' "
                                    + "nor any declared module name in source roots.",
                            a2, a1);
                }
            } else {
                // Non-modular project: artifactId must match exactly
                message = String.format(
                        "Cannot attach artifact to project: groupId, artifactId and version must match the project.%n"
                                + "  Project coordinates:  %s:%s:%s%n"
                                + "  Artifact coordinates: %s:%s:%s",
                        g1, a1, v1, g2, a2, v2);
            }
            throw new IllegalArgumentException(message);
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
