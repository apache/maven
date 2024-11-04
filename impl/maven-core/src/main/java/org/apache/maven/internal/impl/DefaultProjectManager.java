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
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;

import org.apache.maven.RepositoryUtils;
import org.apache.maven.api.ProducedArtifact;
import org.apache.maven.api.Project;
import org.apache.maven.api.ProjectScope;
import org.apache.maven.api.RemoteRepository;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.di.SessionScoped;
import org.apache.maven.api.model.Resource;
import org.apache.maven.api.services.ArtifactManager;
import org.apache.maven.api.services.ProjectManager;
import org.apache.maven.project.MavenProject;
import org.eclipse.sisu.Typed;

import static java.util.stream.Collectors.toList;
import static org.apache.maven.internal.impl.Utils.map;
import static org.apache.maven.internal.impl.Utils.nonNull;

@Named
@Typed
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
    public Optional<Path> getPath(Project project) {
        Optional<ProducedArtifact> mainArtifact = project.getMainArtifact();
        if (mainArtifact.isPresent()) {
            return artifactManager.getPath(mainArtifact.get());
        }
        return Optional.empty();
    }

    @Nonnull
    @Override
    public Collection<ProducedArtifact> getAttachedArtifacts(Project project) {
        InternalMavenSession session = ((DefaultProject) project).getSession();
        Collection<ProducedArtifact> attached = map(
                getMavenProject(project).getAttachedArtifacts(),
                a -> session.getArtifact(ProducedArtifact.class, RepositoryUtils.toArtifact(a)));
        return Collections.unmodifiableCollection(attached);
    }

    @Override
    public Collection<ProducedArtifact> getAllArtifacts(Project project) {
        ArrayList<ProducedArtifact> result = new ArrayList<>(2);
        result.addAll(project.getArtifacts());
        result.addAll(getAttachedArtifacts(project));
        return Collections.unmodifiableCollection(result);
    }

    @Override
    public void attachArtifact(Project project, ProducedArtifact artifact, Path path) {
        nonNull(project, "project");
        nonNull(artifact, "artifact");
        nonNull(path, "path");
        if (artifact.getGroupId().isEmpty()
                || artifact.getArtifactId().isEmpty()
                || artifact.getBaseVersion().asString().isEmpty()) {
            artifact = session.createProducedArtifact(
                    artifact.getGroupId().isEmpty() ? project.getGroupId() : artifact.getGroupId(),
                    artifact.getArtifactId().isEmpty() ? project.getArtifactId() : artifact.getArtifactId(),
                    artifact.getBaseVersion().asString().isEmpty()
                            ? session.parseVersion(project.getVersion()).asString()
                            : artifact.getBaseVersion().asString(),
                    artifact.getClassifier(),
                    artifact.getExtension(),
                    null);
        }
        if (!Objects.equals(project.getGroupId(), artifact.getGroupId())
                || !Objects.equals(project.getArtifactId(), artifact.getArtifactId())
                || !Objects.equals(
                        project.getVersion(), artifact.getBaseVersion().asString())) {
            throw new IllegalArgumentException(
                    "The produced artifact must have the same groupId/artifactId/version than the project it is attached to. Expecting "
                            + project.getGroupId() + ":" + project.getArtifactId() + ":" + project.getVersion()
                            + " but received " + artifact.getGroupId() + ":" + artifact.getArtifactId() + ":"
                            + artifact.getBaseVersion());
        }
        getMavenProject(project)
                .addAttachedArtifact(RepositoryUtils.toArtifact(
                        ((DefaultProject) project).getSession().toArtifact(artifact)));
        artifactManager.setPath(artifact, path);
    }

    @Override
    public List<Path> getCompileSourceRoots(Project project, ProjectScope scope) {
        MavenProject prj = getMavenProject(nonNull(project, "project"));
        List<String> roots;
        if (nonNull(scope, "scope") == ProjectScope.MAIN) {
            roots = prj.getCompileSourceRoots();
        } else if (scope == ProjectScope.TEST) {
            roots = prj.getTestCompileSourceRoots();
        } else {
            throw new IllegalArgumentException("Unsupported scope " + scope);
        }
        return roots.stream()
                .map(Paths::get)
                .collect(Collectors.collectingAndThen(toList(), Collections::unmodifiableList));
    }

    @Override
    public void addCompileSourceRoot(Project project, ProjectScope scope, Path sourceRoot) {
        MavenProject prj = getMavenProject(nonNull(project, "project"));
        String root = nonNull(sourceRoot, "sourceRoot").toAbsolutePath().toString();
        if (nonNull(scope, "scope") == ProjectScope.MAIN) {
            prj.addCompileSourceRoot(root);
        } else if (scope == ProjectScope.TEST) {
            prj.addTestCompileSourceRoot(root);
        } else {
            throw new IllegalArgumentException("Unsupported scope " + scope);
        }
    }

    @Override
    public List<Resource> getResources(@Nonnull Project project, @Nonnull ProjectScope scope) {
        Project prj = nonNull(project, "project");
        if (nonNull(scope, "scope") == ProjectScope.MAIN) {
            return prj.getBuild().getResources();
        } else if (scope == ProjectScope.TEST) {
            return prj.getBuild().getTestResources();
        } else {
            throw new IllegalArgumentException("Unsupported scope " + scope);
        }
    }

    @Override
    public void addResource(@Nonnull Project project, @Nonnull ProjectScope scope, @Nonnull Resource resource) {
        // TODO: we should not modify the underlying model here, but resources should be stored
        // TODO: in a separate field in the project, however, that could break v3 plugins
        MavenProject prj = getMavenProject(nonNull(project, "project"));
        org.apache.maven.model.Resource res = new org.apache.maven.model.Resource(nonNull(resource, "resource"));
        if (nonNull(scope, "scope") == ProjectScope.MAIN) {
            prj.addResource(res);
        } else if (scope == ProjectScope.TEST) {
            prj.addTestResource(res);
        } else {
            throw new IllegalArgumentException("Unsupported scope " + scope);
        }
    }

    @Override
    public List<RemoteRepository> getRemoteProjectRepositories(Project project) {
        List<org.eclipse.aether.repository.RemoteRepository> remoteRepositories =
                ((DefaultProject) project).getProject().getRemoteProjectRepositories();
        if (remoteRepositories == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new MappedList<>(remoteRepositories, session::getRemoteRepository));
    }

    @Override
    public List<RemoteRepository> getRemotePluginRepositories(Project project) {
        List<org.eclipse.aether.repository.RemoteRepository> remoteRepositories =
                ((DefaultProject) project).getProject().getRemoteProjectRepositories();
        if (remoteRepositories == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new MappedList<>(remoteRepositories, session::getRemoteRepository));
    }

    @Override
    public void setProperty(Project project, String key, String value) {
        Properties properties = getMavenProject(project).getProperties();
        if (value == null) {
            properties.remove(key);
        } else {
            properties.setProperty(key, value);
        }
    }

    @Override
    public Map<String, String> getProperties(Project project) {
        return Collections.unmodifiableMap(
                new PropertiesAsMap(((DefaultProject) project).getProject().getProperties()));
    }

    @Override
    public Optional<Project> getExecutionProject(Project project) {
        // Session keep tracks of the Project per project id,
        // so we cannot use session.getProject(p) for forked projects
        // which are temporary clones
        return Optional.ofNullable(getMavenProject(project).getExecutionProject())
                .map(p -> new DefaultProject(session, p));
    }

    private MavenProject getMavenProject(Project project) {
        return ((DefaultProject) project).getProject();
    }
}
