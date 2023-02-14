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
package org.apache.maven.api.plugin.testing.stubs;

import java.io.File;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.apache.maven.api.Artifact;
import org.apache.maven.api.DependencyCoordinate;
import org.apache.maven.api.Project;
import org.apache.maven.api.RemoteRepository;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.model.Model;

/**
 * @author Olivier Lamy
 * @since 1.0-beta-1
 *
 */
public class ProjectStub implements Project {

    private Model model = Model.newInstance();
    private Path basedir;
    private File pomPath;
    private boolean executionRoot;
    private Artifact artifact;

    public void setModel(Model model) {
        this.model = model;
    }

    @Nonnull
    @Override
    public String getGroupId() {
        return model.getGroupId();
    }

    @Nonnull
    @Override
    public String getArtifactId() {
        return model.getArtifactId();
    }

    @Nonnull
    @Override
    public String getVersion() {
        return model.getVersion();
    }

    public String getName() {
        return model.getName();
    }

    @Nonnull
    @Override
    public String getPackaging() {
        return model.getPackaging();
    }

    @Nonnull
    @Override
    public Artifact getArtifact() {
        return artifact;
    }

    @Nonnull
    @Override
    public Model getModel() {
        return model;
    }

    @Nonnull
    @Override
    public Optional<Path> getPomPath() {
        return Optional.ofNullable(pomPath).map(File::toPath);
    }

    @Nonnull
    @Override
    public List<DependencyCoordinate> getDependencies() {
        return null;
    }

    @Nonnull
    @Override
    public List<DependencyCoordinate> getManagedDependencies() {
        return null;
    }

    @Override
    public Optional<Path> getBasedir() {
        return Optional.ofNullable(basedir);
    }

    public void setBasedir(Path basedir) {
        this.basedir = basedir;
    }

    @Override
    public boolean isExecutionRoot() {
        return executionRoot;
    }

    @Override
    public Optional<Project> getParent() {
        return Optional.empty();
    }

    @Override
    public List<RemoteRepository> getRemoteProjectRepositories() {
        return Collections.emptyList();
    }

    @Override
    public List<RemoteRepository> getRemotePluginRepositories() {
        return Collections.emptyList();
    }

    public void setGroupId(String groupId) {
        model = model.withGroupId(groupId);
    }

    public void setArtifactId(String artifactId) {
        model = model.withArtifactId(artifactId);
    }

    public void setVersion(String version) {
        model = model.withVersion(version);
    }

    public void setName(String name) {
        model = model.withName(name);
    }

    public void setPackaging(String packaging) {
        model = model.withPackaging(packaging);
    }

    public void setArtifact(Artifact artifact) {
        this.artifact = artifact;
    }

    public void setPomPath(File pomPath) {
        this.pomPath = pomPath;
    }

    public void setExecutionRoot(boolean executionRoot) {
        this.executionRoot = executionRoot;
    }

    public void setMavenModel(org.apache.maven.model.Model model) {
        this.model = model.getDelegate();
    }
}
