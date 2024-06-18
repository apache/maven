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
import org.apache.maven.api.ArtifactCoordinate;
import org.apache.maven.api.Dependency;
import org.apache.maven.api.DependencyCoordinate;
import org.apache.maven.api.DependencyScope;
import org.apache.maven.api.Language;
import org.apache.maven.api.Listener;
import org.apache.maven.api.LocalRepository;
import org.apache.maven.api.Node;
import org.apache.maven.api.Packaging;
import org.apache.maven.api.PathScope;
import org.apache.maven.api.PathType;
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

/**
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
    public Map<String, String> getEffectiveProperties(@Nullable Project project) {
        HashMap<String, String> result = new HashMap<>(getSystemProperties());
        if (project != null) {
            result.putAll(project.getModel().getProperties());
        }
        result.putAll(getUserProperties());
        return result;
    }

    @Override
    public LocalRepository getLocalRepository() {
        return null;
    }

    @Override
    public Path getTopDirectory() {
        return null;
    }

    @Override
    public Path getRootDirectory() {
        return null;
    }

    @Override
    public List<RemoteRepository> getRemoteRepositories() {
        return null;
    }

    @Override
    public SessionData getData() {
        return null;
    }

    @Override
    public Version getMavenVersion() {
        return null;
    }

    @Override
    public int getDegreeOfConcurrency() {
        return 0;
    }

    @Override
    public Instant getStartTime() {
        return null;
    }

    @Override
    public List<Project> getProjects() {
        return null;
    }

    @Override
    public Map<String, Object> getPluginContext(Project project) {
        return null;
    }

    @Override
    public <T extends Service> T getService(Class<T> clazz) {
        return null;
    }

    @Override
    public Session withLocalRepository(LocalRepository localRepository) {
        return null;
    }

    @Override
    public Session withRemoteRepositories(List<RemoteRepository> repositories) {
        return null;
    }

    @Override
    public void registerListener(Listener listener) {}

    @Override
    public void unregisterListener(Listener listener) {}

    @Override
    public Collection<Listener> getListeners() {
        return null;
    }

    @Override
    public LocalRepository createLocalRepository(Path path) {
        return null;
    }

    @Override
    public RemoteRepository createRemoteRepository(String id, String url) {
        return null;
    }

    @Override
    public RemoteRepository createRemoteRepository(Repository repository) {
        return null;
    }

    @Override
    public Artifact createArtifact(String groupId, String artifactId, String version, String extension) {
        return null;
    }

    @Override
    public Artifact createArtifact(
            String groupId, String artifactId, String version, String classifier, String extension, String type) {
        return null;
    }

    @Override
    public ArtifactCoordinate createArtifactCoordinate(String s, String s1, String s2, String s3) {
        return null;
    }

    @Override
    public ArtifactCoordinate createArtifactCoordinate(String coordString) {
        return null;
    }

    @Override
    public ArtifactCoordinate createArtifactCoordinate(
            String s, String s1, String s2, String s3, String s4, String s5) {
        return null;
    }

    @Override
    public ArtifactCoordinate createArtifactCoordinate(Artifact artifact) {
        return null;
    }

    @Override
    public DependencyCoordinate createDependencyCoordinate(ArtifactCoordinate artifactCoordinate) {
        return null;
    }

    @Override
    public DependencyCoordinate createDependencyCoordinate(Dependency dependency) {
        return null;
    }

    @Override
    public Map.Entry<Artifact, Path> resolveArtifact(Artifact artifact) {
        return null;
    }

    @Override
    public Map.Entry<Artifact, Path> resolveArtifact(ArtifactCoordinate coordinate) {
        return null;
    }

    @Override
    public Map<Artifact, Path> resolveArtifacts(ArtifactCoordinate... artifactCoordinates) {
        return null;
    }

    @Override
    public Map<Artifact, Path> resolveArtifacts(Collection<? extends ArtifactCoordinate> collection) {
        return null;
    }

    @Override
    public Map<Artifact, Path> resolveArtifacts(Artifact... artifacts) {
        return null;
    }

    @Override
    public List<Node> flattenDependencies(Node node, PathScope scope) {
        return null;
    }

    @Override
    public List<Path> resolveDependencies(DependencyCoordinate dependencyCoordinate) {
        return null;
    }

    @Override
    public List<Path> resolveDependencies(List<DependencyCoordinate> dependencyCoordinates) {
        return null;
    }

    @Override
    public List<Path> resolveDependencies(Project project, PathScope scope) {
        return null;
    }

    @Override
    public Version resolveVersion(ArtifactCoordinate artifact) {
        return null;
    }

    @Override
    public List<Version> resolveVersionRange(ArtifactCoordinate artifact) {
        return null;
    }

    @Override
    public void installArtifacts(Artifact... artifacts) {}

    @Override
    public void installArtifacts(Collection<Artifact> artifacts) {}

    @Override
    public void deployArtifact(RemoteRepository repository, Artifact... artifacts) {}

    @Override
    public void setArtifactPath(Artifact artifact, Path path) {}

    @Override
    public Optional<Path> getArtifactPath(Artifact artifact) {
        return Optional.empty();
    }

    @Override
    public boolean isVersionSnapshot(String version) {
        return false;
    }

    @Override
    public Node collectDependencies(Artifact artifact) {
        return null;
    }

    @Override
    public Node collectDependencies(Project project) {
        return null;
    }

    @Override
    public Node collectDependencies(DependencyCoordinate dependencyCoordinate) {
        return null;
    }

    @Override
    public Path getPathForLocalArtifact(Artifact artifact) {
        return null;
    }

    @Override
    public Path getPathForRemoteArtifact(RemoteRepository remote, Artifact artifact) {
        return null;
    }

    @Override
    public Version parseVersion(String version) {
        return null;
    }

    @Override
    public VersionRange parseVersionRange(String versionRange) {
        return null;
    }

    @Override
    public VersionConstraint parseVersionConstraint(String s) {
        return null;
    }

    @Override
    public Map<PathType, List<Path>> resolveDependencies(
            DependencyCoordinate dependencyCoordinate, PathScope scope, Collection<PathType> desiredTypes) {
        return Map.of();
    }

    @Override
    public Map<PathType, List<Path>> resolveDependencies(
            Project project, PathScope scope, Collection<PathType> desiredTypes) {
        return Map.of();
    }

    @Override
    public Type requireType(String id) {
        return null;
    }

    @Override
    public Language requireLanguage(String id) {
        return null;
    }

    @Override
    public Packaging requirePackaging(String id) {
        return null;
    }

    @Override
    public ProjectScope requireProjectScope(String id) {
        return null;
    }

    @Override
    public DependencyScope requireDependencyScope(String id) {
        return null;
    }

    @Override
    public PathScope requirePathScope(String id) {
        return null;
    }
}
