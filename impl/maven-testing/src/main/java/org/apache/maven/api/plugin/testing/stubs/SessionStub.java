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
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.maven.api.Artifact;
import org.apache.maven.api.ArtifactCoordinates;
import org.apache.maven.api.Dependency;
import org.apache.maven.api.DependencyCoordinates;
import org.apache.maven.api.DependencyScope;
import org.apache.maven.api.DownloadedArtifact;
import org.apache.maven.api.Language;
import org.apache.maven.api.Listener;
import org.apache.maven.api.LocalRepository;
import org.apache.maven.api.Node;
import org.apache.maven.api.Packaging;
import org.apache.maven.api.PathScope;
import org.apache.maven.api.PathType;
import org.apache.maven.api.ProducedArtifact;
import org.apache.maven.api.Project;
import org.apache.maven.api.ProjectScope;
import org.apache.maven.api.RemoteRepository;
import org.apache.maven.api.Service;
import org.apache.maven.api.Session;
import org.apache.maven.api.SessionData;
import org.apache.maven.api.Type;
import org.apache.maven.api.Version;
import org.apache.maven.api.VersionConstraint;
import org.apache.maven.api.VersionRange;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.annotations.Nullable;
import org.apache.maven.api.model.Repository;
import org.apache.maven.api.settings.Settings;
import org.apache.maven.api.toolchain.ToolchainModel;

/**
 * A stub implementation of {@link Session} for basic testing scenarios.
 * Provides minimal implementation of session methods without mock dependencies.
 *
 * <p>For more comprehensive session mocking, consider using {@link SessionMock} instead.</p>
 *
 * @see SessionMock
 * @since 4.0.0
 */
public class SessionStub implements Session {

    private Map<String, String> userProperties;

    private Map<String, String> systemProperties;

    private final Settings settings;

    public SessionStub(Settings settings) {
        this(null, null, settings);
    }

    public SessionStub() {
        this(null, null, null);
    }

    public SessionStub(Map<String, String> userProperties) {
        this(null, userProperties, null);
    }

    public SessionStub(Map<String, String> systemProperties, Map<String, String> userProperties, Settings settings) {

        this.settings = settings;

        this.systemProperties = new HashMap<>();
        if (systemProperties != null) {
            this.systemProperties.putAll(systemProperties);
        }
        System.getProperties().forEach((k, v) -> this.systemProperties.put(k.toString(), v.toString()));

        this.userProperties = new HashMap<>();
        if (userProperties != null) {
            this.userProperties.putAll(userProperties);
        }
    }

    @Override
    public Settings getSettings() {
        return settings;
    }

    @Override
    public Map<String, String> getSystemProperties() {
        return this.systemProperties;
    }

    @Override
    public Map<String, String> getUserProperties() {
        return this.userProperties;
    }

    @Nonnull
    @Override
    public Map<String, String> getEffectiveProperties(@Nullable Project project) {
        HashMap<String, String> result = new HashMap<>(getSystemProperties());
        if (project != null) {
            result.putAll(project.getModel().getProperties());
        }
        result.putAll(getUserProperties());
        return result;
    }

    @Override
    @Nullable
    public LocalRepository getLocalRepository() {
        return null;
    }

    @Override
    @Nullable
    public Path getTopDirectory() {
        return null;
    }

    @Override
    @Nullable
    public Path getRootDirectory() {
        return null;
    }

    @Override
    @Nullable
    public List<RemoteRepository> getRemoteRepositories() {
        return null;
    }

    @Override
    @Nullable
    public SessionData getData() {
        return null;
    }

    @Override
    @Nullable
    public Version getMavenVersion() {
        return null;
    }

    @Override
    public int getDegreeOfConcurrency() {
        return 0;
    }

    @Override
    @Nullable
    public Instant getStartTime() {
        return null;
    }

    @Override
    @Nullable
    public List<Project> getProjects() {
        return null;
    }

    @Override
    @Nullable
    public Map<String, Object> getPluginContext(Project project) {
        return null;
    }

    @Override
    public <T extends Service> @Nullable T getService(Class<T> clazz) {
        return null;
    }

    @Override
    @Nullable
    public Session withLocalRepository(LocalRepository localRepository) {
        return null;
    }

    @Override
    @Nullable
    public Session withRemoteRepositories(List<RemoteRepository> repositories) {
        return null;
    }

    @Override
    public void registerListener(Listener listener) {}

    @Override
    public void unregisterListener(Listener listener) {}

    @Override
    @Nullable
    public Collection<Listener> getListeners() {
        return null;
    }

    @Override
    @Nullable
    public LocalRepository createLocalRepository(Path path) {
        return null;
    }

    @Override
    @Nullable
    public RemoteRepository createRemoteRepository(String id, String url) {
        return null;
    }

    @Override
    @Nullable
    public RemoteRepository createRemoteRepository(Repository repository) {
        return null;
    }

    @Override
    @Nullable
    public Artifact createArtifact(String groupId, String artifactId, String version, String extension) {
        return null;
    }

    @Override
    @Nullable
    public Artifact createArtifact(
            String groupId, String artifactId, String version, String classifier, String extension, String type) {
        return null;
    }

    @Override
    @Nullable
    public ProducedArtifact createProducedArtifact(
            String groupId, String artifactId, String version, String extension) {
        return null;
    }

    @Override
    @Nullable
    public ProducedArtifact createProducedArtifact(
            String groupId, String artifactId, String version, String classifier, String extension, String type) {
        return null;
    }

    @Override
    @Nullable
    public ArtifactCoordinates createArtifactCoordinates(
            String groupId, String artifactId, String version, String extension) {
        return null;
    }

    @Override
    @Nullable
    public ArtifactCoordinates createArtifactCoordinates(String coordString) {
        return null;
    }

    @Override
    @Nullable
    public ArtifactCoordinates createArtifactCoordinates(
            String groupId, String artifactId, String version, String classifier, String extension, String type) {
        return null;
    }

    @Override
    @Nullable
    public ArtifactCoordinates createArtifactCoordinates(Artifact artifact) {
        return null;
    }

    @Override
    @Nullable
    public DependencyCoordinates createDependencyCoordinates(ArtifactCoordinates artifactCoordinates) {
        return null;
    }

    @Override
    @Nullable
    public DependencyCoordinates createDependencyCoordinates(Dependency dependency) {
        return null;
    }

    @Override
    @Nullable
    public DownloadedArtifact resolveArtifact(Artifact artifact) {
        return null;
    }

    @Override
    @Nullable
    public DownloadedArtifact resolveArtifact(ArtifactCoordinates coordinate) {
        return null;
    }

    @Override
    @Nullable
    public DownloadedArtifact resolveArtifact(ArtifactCoordinates coordinates, List<RemoteRepository> repositories) {
        return null;
    }

    @Override
    @Nullable
    public DownloadedArtifact resolveArtifact(Artifact artifact, List<RemoteRepository> repositories) {
        return null;
    }

    @Override
    @Nullable
    public Collection<DownloadedArtifact> resolveArtifacts(ArtifactCoordinates... artifactCoordinates) {
        return null;
    }

    @Override
    @Nullable
    public Collection<DownloadedArtifact> resolveArtifacts(Collection<? extends ArtifactCoordinates> collection) {
        return null;
    }

    @Override
    @Nullable
    public Collection<DownloadedArtifact> resolveArtifacts(Artifact... artifacts) {
        return null;
    }

    @Override
    @Nullable
    public Collection<DownloadedArtifact> resolveArtifacts(
            Collection<? extends ArtifactCoordinates> coordinates, List<RemoteRepository> repositories) {
        return null;
    }

    @Override
    @Nullable
    public List<Node> flattenDependencies(Node node, PathScope scope) {
        return null;
    }

    @Override
    @Nullable
    public List<Path> resolveDependencies(DependencyCoordinates dependencyCoordinates) {
        return null;
    }

    @Override
    @Nullable
    public List<Path> resolveDependencies(List<DependencyCoordinates> dependencyCoordinatess) {
        return null;
    }

    @Override
    @Nullable
    public List<Path> resolveDependencies(Project project, PathScope scope) {
        return null;
    }

    @Override
    @Nullable
    public Version resolveVersion(ArtifactCoordinates artifact) {
        return null;
    }

    @Override
    @Nullable
    public List<Version> resolveVersionRange(ArtifactCoordinates artifact) {
        return null;
    }

    @Override
    @Nullable
    public List<Version> resolveVersionRange(ArtifactCoordinates artifact, List<RemoteRepository> repositories) {
        return null;
    }

    @Override
    public void installArtifacts(ProducedArtifact... artifacts) {}

    @Override
    public void installArtifacts(Collection<ProducedArtifact> artifacts) {}

    @Override
    public void deployArtifact(RemoteRepository repository, ProducedArtifact... artifacts) {}

    @Override
    public void setArtifactPath(ProducedArtifact artifact, Path path) {}

    @Override
    public Optional<Path> getArtifactPath(Artifact artifact) {
        return Optional.empty();
    }

    @Override
    public boolean isVersionSnapshot(String version) {
        return false;
    }

    @Override
    @Nullable
    public Node collectDependencies(Artifact artifact, PathScope scope) {
        return null;
    }

    @Override
    @Nullable
    public Node collectDependencies(Project project, PathScope scope) {
        return null;
    }

    @Override
    @Nullable
    public Node collectDependencies(DependencyCoordinates dependencyCoordinates, PathScope scope) {
        return null;
    }

    @Override
    @Nullable
    public Path getPathForLocalArtifact(Artifact artifact) {
        return null;
    }

    @Override
    @Nullable
    public Path getPathForRemoteArtifact(RemoteRepository remote, Artifact artifact) {
        return null;
    }

    @Override
    @Nullable
    public Version parseVersion(String version) {
        return null;
    }

    @Override
    @Nullable
    public VersionRange parseVersionRange(String versionRange) {
        return null;
    }

    @Override
    @Nullable
    public VersionConstraint parseVersionConstraint(String s) {
        return null;
    }

    @Override
    public Map<PathType, List<Path>> resolveDependencies(
            DependencyCoordinates dependencyCoordinates, PathScope scope, Collection<PathType> desiredTypes) {
        return Map.of();
    }

    @Override
    public Map<PathType, List<Path>> resolveDependencies(
            Project project, PathScope scope, Collection<PathType> desiredTypes) {
        return Map.of();
    }

    @Override
    @Nullable
    public Type requireType(String id) {
        return null;
    }

    @Override
    @Nullable
    public Language requireLanguage(String id) {
        return null;
    }

    @Override
    @Nullable
    public Packaging requirePackaging(String id) {
        return null;
    }

    @Override
    @Nullable
    public ProjectScope requireProjectScope(String id) {
        return null;
    }

    @Override
    @Nullable
    public DependencyScope requireDependencyScope(String id) {
        return null;
    }

    @Override
    @Nullable
    public PathScope requirePathScope(String id) {
        return null;
    }

    @Override
    public Optional<Version> resolveHighestVersion(ArtifactCoordinates artifact, List<RemoteRepository> repositories) {
        return Optional.empty();
    }

    @Override
    public Collection<ToolchainModel> getToolchains() {
        return List.of();
    }
}
