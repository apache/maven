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
package org.apache.maven.project;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.Profile;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.properties.internal.SystemProperties;
import org.eclipse.aether.RepositorySystemSession;

/**
 * DefaultProjectBuildingRequest
 *
 * @deprecated use {@code org.apache.maven.api.services.ProjectBuilder} instead
 */
@Deprecated(since = "4.0.0")
public class DefaultProjectBuildingRequest implements ProjectBuildingRequest {

    private RepositorySystemSession repositorySession;

    private ArtifactRepository localRepository;

    private List<ArtifactRepository> remoteRepositories;

    private List<ArtifactRepository> pluginArtifactRepositories;

    private MavenProject project;

    private int validationLevel = ModelBuildingRequest.VALIDATION_LEVEL_STRICT;

    private boolean processPlugins;

    private List<Profile> profiles;

    private List<String> activeProfileIds;

    private List<String> inactiveProfileIds;

    private Properties systemProperties;

    private Properties userProperties;

    private Instant buildStartTime;

    private boolean resolveDependencies;

    @Deprecated
    private boolean resolveVersionRanges;

    private RepositoryMerging repositoryMerging = RepositoryMerging.POM_DOMINANT;

    public DefaultProjectBuildingRequest() {
        processPlugins = true;
        profiles = new ArrayList<>();
        activeProfileIds = new ArrayList<>();
        inactiveProfileIds = new ArrayList<>();
        systemProperties = new Properties();
        userProperties = new Properties();
        remoteRepositories = new ArrayList<>();
        pluginArtifactRepositories = new ArrayList<>();
    }

    public DefaultProjectBuildingRequest(ProjectBuildingRequest request) {
        this();
        setProcessPlugins(request.isProcessPlugins());
        setProfiles(request.getProfiles());
        setActiveProfileIds(request.getActiveProfileIds());
        setInactiveProfileIds(request.getInactiveProfileIds());
        setSystemProperties(request.getSystemProperties());
        setUserProperties(request.getUserProperties());
        setRemoteRepositories(request.getRemoteRepositories());
        setPluginArtifactRepositories(request.getPluginArtifactRepositories());
        setRepositorySession(request.getRepositorySession());
        setLocalRepository(request.getLocalRepository());
        setBuildStartTime(request.getBuildStartTime());
        setProject(request.getProject());
        setResolveDependencies(request.isResolveDependencies());
        setValidationLevel(request.getValidationLevel());
        setResolveVersionRanges(request.isResolveVersionRanges());
        setRepositoryMerging(request.getRepositoryMerging());
    }

    @Override
    public MavenProject getProject() {
        return project;
    }

    @Override
    public void setProject(MavenProject mavenProject) {
        this.project = mavenProject;
    }

    @Override
    public ProjectBuildingRequest setLocalRepository(ArtifactRepository localRepository) {
        this.localRepository = localRepository;
        return this;
    }

    @Override
    public ArtifactRepository getLocalRepository() {
        return localRepository;
    }

    @Override
    public List<ArtifactRepository> getRemoteRepositories() {
        return remoteRepositories;
    }

    @Override
    public ProjectBuildingRequest setRemoteRepositories(List<ArtifactRepository> remoteRepositories) {
        if (remoteRepositories != null) {
            this.remoteRepositories = new ArrayList<>(remoteRepositories);
        } else {
            this.remoteRepositories.clear();
        }

        return this;
    }

    @Override
    public List<ArtifactRepository> getPluginArtifactRepositories() {
        return pluginArtifactRepositories;
    }

    @Override
    public ProjectBuildingRequest setPluginArtifactRepositories(List<ArtifactRepository> pluginArtifactRepositories) {
        if (pluginArtifactRepositories != null) {
            this.pluginArtifactRepositories = new ArrayList<>(pluginArtifactRepositories);
        } else {
            this.pluginArtifactRepositories.clear();
        }

        return this;
    }

    @Override
    public Properties getSystemProperties() {
        return systemProperties;
    }

    @Override
    public ProjectBuildingRequest setSystemProperties(Properties systemProperties) {
        if (systemProperties != null) {
            this.systemProperties = SystemProperties.copyProperties(systemProperties);
        } else {
            this.systemProperties.clear();
        }

        return this;
    }

    @Override
    public Properties getUserProperties() {
        return userProperties;
    }

    @Override
    public ProjectBuildingRequest setUserProperties(Properties userProperties) {
        if (userProperties != null) {
            this.userProperties = new Properties();
            this.userProperties.putAll(userProperties);
        } else {
            this.userProperties.clear();
        }

        return this;
    }

    @Override
    public boolean isProcessPlugins() {
        return processPlugins;
    }

    @Override
    public ProjectBuildingRequest setProcessPlugins(boolean processPlugins) {
        this.processPlugins = processPlugins;
        return this;
    }

    @Override
    public ProjectBuildingRequest setResolveDependencies(boolean resolveDependencies) {
        this.resolveDependencies = resolveDependencies;
        return this;
    }

    @Override
    public boolean isResolveDependencies() {
        return resolveDependencies;
    }

    /**
     * @since 3.2.2
     * @deprecated This got added when implementing MNG-2199 and is no longer used.
     * Commit 6cf9320942c34bc68205425ab696b1712ace9ba4 updated the way 'MavenProject' objects are initialized.
     */
    @Deprecated
    @Override
    public ProjectBuildingRequest setResolveVersionRanges(boolean value) {
        this.resolveVersionRanges = value;
        return this;
    }

    /**
     * @since 3.2.2
     * @deprecated This got added when implementing MNG-2199 and is no longer used.
     * Commit 6cf9320942c34bc68205425ab696b1712ace9ba4 updated the way 'MavenProject' objects are initialized.
     */
    @Deprecated
    @Override
    public boolean isResolveVersionRanges() {
        return this.resolveVersionRanges;
    }

    @Override
    public ProjectBuildingRequest setValidationLevel(int validationLevel) {
        this.validationLevel = validationLevel;
        return this;
    }

    @Override
    public int getValidationLevel() {
        return validationLevel;
    }

    @Override
    public List<String> getActiveProfileIds() {
        return activeProfileIds;
    }

    @Override
    public void setActiveProfileIds(List<String> activeProfileIds) {
        if (activeProfileIds != null) {
            this.activeProfileIds = new ArrayList<>(activeProfileIds);
        } else {
            this.activeProfileIds.clear();
        }
    }

    @Override
    public List<String> getInactiveProfileIds() {
        return inactiveProfileIds;
    }

    @Override
    public void setInactiveProfileIds(List<String> inactiveProfileIds) {
        if (inactiveProfileIds != null) {
            this.inactiveProfileIds = new ArrayList<>(inactiveProfileIds);
        } else {
            this.inactiveProfileIds.clear();
        }
    }

    @Override
    public void setProfiles(List<Profile> profiles) {
        if (profiles != null) {
            this.profiles = new ArrayList<>(profiles);
        } else {
            this.profiles.clear();
        }
    }

    @Override
    public void addProfile(Profile profile) {
        profiles.add(profile);
    }

    @Override
    public List<Profile> getProfiles() {
        return profiles;
    }

    @Deprecated
    @Override
    public Date getBuildStartTime() {
        return buildStartTime != null ? new Date(buildStartTime.toEpochMilli()) : null;
    }

    @Deprecated
    @Override
    public void setBuildStartTime(Date buildStartTime) {
        setBuildStartInstant(buildStartTime != null ? Instant.ofEpochMilli(buildStartTime.getTime()) : null);
    }

    @Override
    public Instant getBuildStartInstant() {
        return this.buildStartTime;
    }

    @Override
    public void setBuildStartInstant(Instant buildStartTime) {
        this.buildStartTime = buildStartTime;
    }

    @Override
    public RepositorySystemSession getRepositorySession() {
        return repositorySession;
    }

    @Override
    public DefaultProjectBuildingRequest setRepositorySession(RepositorySystemSession repositorySession) {
        this.repositorySession = repositorySession;
        return this;
    }

    @Override
    public DefaultProjectBuildingRequest setRepositoryMerging(RepositoryMerging repositoryMerging) {
        this.repositoryMerging = Objects.requireNonNull(repositoryMerging, "repositoryMerging cannot be null");
        return this;
    }

    @Override
    public RepositoryMerging getRepositoryMerging() {
        return repositoryMerging;
    }
}
