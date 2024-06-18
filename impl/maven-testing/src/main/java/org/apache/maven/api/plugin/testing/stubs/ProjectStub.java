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

import java.nio.file.Path;
import java.util.*;

import org.apache.maven.api.*;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.model.Model;
import org.apache.maven.api.model.PluginContainer;

/**
 * @author Olivier Lamy
 * @since 1.0-beta-1
 *
 */
public class ProjectStub implements Project {

    private Model model = Model.newInstance();
    private Path basedir;
    private Path pomPath;
    private boolean topProject;
    private Path rootDirectory;
    private Map<String, String> properties = new HashMap<>();
    private Artifact mainArtifact;

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
    public Packaging getPackaging() {
        return new Packaging() {
            @Override
            public String id() {
                return model.getPackaging();
            }

            @Override
            public Type type() {
                return new Type() {
                    @Override
                    public String id() {
                        return model.getPackaging();
                    }

                    @Override
                    public Language getLanguage() {
                        return null;
                    }

                    @Override
                    public String getExtension() {
                        return model.getPackaging();
                    }

                    @Override
                    public String getClassifier() {
                        return "";
                    }

                    @Override
                    public boolean isIncludesDependencies() {
                        return false;
                    }

                    @Override
                    public Set<PathType> getPathTypes() {
                        return Set.of();
                    }
                };
            }

            @Override
            public Map<String, PluginContainer> plugins() {
                return Map.of();
            }
        };
    }

    @Override
    public List<Artifact> getArtifacts() {
        Artifact pomArtifact = new ArtifactStub(getGroupId(), getArtifactId(), "", getVersion(), "pom");
        return mainArtifact != null ? Arrays.asList(pomArtifact, mainArtifact) : Arrays.asList(pomArtifact);
    }

    @Nonnull
    @Override
    public Model getModel() {
        return model;
    }

    @Nonnull
    @Override
    public Path getPomPath() {
        return pomPath;
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
    public Path getBasedir() {
        return basedir;
    }

    @Override
    public Optional<Project> getParent() {
        return Optional.empty();
    }

    @Override
    public boolean isTopProject() {
        return topProject;
    }

    @Override
    public boolean isRootProject() {
        return model.isRoot();
    }

    @Override
    public Path getRootDirectory() {
        return rootDirectory;
    }

    //
    // Setters
    //

    public ProjectStub setBasedir(Path basedir) {
        this.basedir = basedir;
        return this;
    }

    public ProjectStub setGroupId(String groupId) {
        model = model.withGroupId(groupId);
        return this;
    }

    public ProjectStub setArtifactId(String artifactId) {
        model = model.withArtifactId(artifactId);
        return this;
    }

    public ProjectStub setVersion(String version) {
        model = model.withVersion(version);
        return this;
    }

    public ProjectStub setName(String name) {
        model = model.withName(name);
        return this;
    }

    public ProjectStub setDescription(String desc) {
        model = model.withDescription(desc);
        return this;
    }

    public ProjectStub setPackaging(String packaging) {
        model = model.withPackaging(packaging);
        return this;
    }

    public ProjectStub setMainArtifact(Artifact mainArtifact) {
        this.mainArtifact = mainArtifact;
        return this;
    }

    public ProjectStub setPomPath(Path pomPath) {
        this.pomPath = pomPath;
        return this;
    }

    public ProjectStub setTopProject(boolean topProject) {
        this.topProject = topProject;
        return this;
    }

    public ProjectStub setMavenModel(org.apache.maven.model.Model model) {
        this.model = model.getDelegate();
        return this;
    }

    public ProjectStub setRootDirectory(Path rootDirectory) {
        this.rootDirectory = rootDirectory;
        return this;
    }

    public ProjectStub addProperty(String key, String value) {
        Map<String, String> props = new HashMap<>(model.getProperties());
        props.put(key, value);
        model = model.withProperties(props);
        return this;
    }
}
