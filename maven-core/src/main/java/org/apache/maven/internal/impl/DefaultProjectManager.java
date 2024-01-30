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
import java.util.*;

import org.apache.maven.RepositoryUtils;
import org.apache.maven.api.*;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.di.SessionScoped;
import org.apache.maven.api.model.Resource;
import org.apache.maven.api.services.*;
import org.apache.maven.project.MavenProject;
import org.eclipse.sisu.Typed;

import static org.apache.maven.internal.impl.Utils.map;

@Named
@Typed
@SessionScoped
public class DefaultProjectManager implements ProjectManager {

    private final InternalSession session;
    private final ArtifactManager artifactManager;

    @Inject
    public DefaultProjectManager(InternalSession session, ArtifactManager artifactManager) {
        this.session = session;
        this.artifactManager = artifactManager;
    }

    @Nonnull
    @Override
    public Optional<Path> getPath(Project project) {
        Optional<Artifact> mainArtifact = project.getMainArtifact();
        if (mainArtifact.isPresent()) {
            return artifactManager.getPath(mainArtifact.get());
        }
        return Optional.empty();
    }

    @Nonnull
    @Override
    public Collection<Artifact> getAttachedArtifacts(Project project) {
        InternalSession session = ((DefaultProject) project).getSession();
        Collection<Artifact> attached = map(
                getMavenProject(project).getAttachedArtifacts(),
                a -> session.getArtifact(RepositoryUtils.toArtifact(a)));
        return Collections.unmodifiableCollection(attached);
    }

    @Override
    public Collection<Artifact> getAllArtifacts(Project project) {
        ArrayList<Artifact> result = new ArrayList<>(2);
        result.addAll(project.getArtifacts());
        result.addAll(getAttachedArtifacts(project));
        return Collections.unmodifiableCollection(result);
    }

    @Override
    public void attachArtifact(Project project, Artifact artifact, Path path) {
        getMavenProject(project)
                .addAttachedArtifact(RepositoryUtils.toArtifact(
                        ((DefaultProject) project).getSession().toArtifact(artifact)));
        artifactManager.setPath(artifact, path);
    }

    @Override
    public List<String> getCompileSourceRoots(Project project) {
        List<String> roots = getMavenProject(project).getCompileSourceRoots();
        return Collections.unmodifiableList(roots);
    }

    @Override
    public void addCompileSourceRoot(Project project, String sourceRoot) {
        List<String> roots = getMavenProject(project).getCompileSourceRoots();
        roots.add(sourceRoot);
    }

    @Override
    public List<String> getTestCompileSourceRoots(Project project) {
        List<String> roots = getMavenProject(project).getTestCompileSourceRoots();
        return Collections.unmodifiableList(roots);
    }

    @Override
    public void addTestCompileSourceRoot(Project project, String sourceRoot) {
        List<String> roots = getMavenProject(project).getTestCompileSourceRoots();
        roots.add(sourceRoot);
    }

    @Override
    public List<Resource> getResources(Project project) {
        return getMavenProject(project).getBuild().getDelegate().getResources();
    }

    @Override
    public void addResource(Project project, Resource resource) {
        getMavenProject(project).addResource(new org.apache.maven.model.Resource(resource));
    }

    @Override
    public List<Resource> getTestResources(Project project) {
        return getMavenProject(project).getBuild().getDelegate().getTestResources();
    }

    @Override
    public void addTestResource(Project project, Resource resource) {
        getMavenProject(project).addTestResource(new org.apache.maven.model.Resource(resource));
    }

    @Override
    public List<RemoteRepository> getRemoteProjectRepositories(Project project) {
        return Collections.unmodifiableList(new MappedList<>(
                ((DefaultProject) project).getProject().getRemoteProjectRepositories(), session::getRemoteRepository));
    }

    @Override
    public List<RemoteRepository> getRemotePluginRepositories(Project project) {
        return Collections.unmodifiableList(new MappedList<>(
                ((DefaultProject) project).getProject().getRemotePluginRepositories(), session::getRemoteRepository));
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
