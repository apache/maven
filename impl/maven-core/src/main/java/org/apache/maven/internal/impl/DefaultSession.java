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

import java.nio.file.Path;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.maven.RepositoryUtils;
import org.apache.maven.api.Project;
import org.apache.maven.api.RemoteRepository;
import org.apache.maven.api.Session;
import org.apache.maven.api.Version;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.annotations.Nullable;
import org.apache.maven.api.services.Lookup;
import org.apache.maven.api.services.LookupException;
import org.apache.maven.api.services.MavenException;
import org.apache.maven.api.settings.Settings;
import org.apache.maven.api.toolchain.ToolchainModel;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.bridge.MavenRepositorySystem;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.impl.AbstractSession;
import org.apache.maven.impl.DefaultRemoteRepository;
import org.apache.maven.impl.PropertiesAsMap;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;
import org.apache.maven.rtinfo.RuntimeInformation;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;

import static org.apache.maven.impl.Utils.map;
import static org.apache.maven.impl.Utils.nonNull;

public class DefaultSession extends AbstractSession implements InternalMavenSession {

    private final MavenSession mavenSession;
    private final MavenRepositorySystem mavenRepositorySystem;
    private final RuntimeInformation runtimeInformation;
    private final Map<String, Project> allProjects = new ConcurrentHashMap<>();

    @SuppressWarnings("checkstyle:ParameterNumber")
    public DefaultSession(
            @Nonnull MavenSession session,
            @Nonnull RepositorySystem repositorySystem,
            @Nullable List<RemoteRepository> remoteRepositories,
            @Nonnull MavenRepositorySystem mavenRepositorySystem,
            @Nonnull Lookup lookup,
            @Nonnull RuntimeInformation runtimeInformation) {
        super(
                nonNull(session).getRepositorySession(),
                repositorySystem,
                remoteRepositories,
                remoteRepositories == null
                        ? map(session.getRequest().getRemoteRepositories(), RepositoryUtils::toRepo)
                        : null,
                lookup);
        this.mavenSession = session;
        this.mavenRepositorySystem = mavenRepositorySystem;
        this.runtimeInformation = runtimeInformation;
    }

    public MavenSession getMavenSession() {
        if (mavenSession == null) {
            throw new IllegalArgumentException("Found null mavenSession on session " + this);
        }
        return mavenSession;
    }

    @Override
    public List<Project> getProjects(List<MavenProject> projects) {
        return projects == null ? null : map(projects, this::getProject);
    }

    @Override
    public Project getProject(MavenProject project) {
        return project != null && project.getBasedir() != null
                ? allProjects.computeIfAbsent(project.getId(), id -> new DefaultProject(this, project))
                : null;
    }

    @Override
    public List<ArtifactRepository> toArtifactRepositories(List<RemoteRepository> repositories) {
        return repositories == null ? null : map(repositories, this::toArtifactRepository);
    }

    @Nonnull
    @Override
    public Settings getSettings() {
        return getMavenSession().getSettings().getDelegate();
    }

    @Nonnull
    @Override
    public Collection<ToolchainModel> getToolchains() {
        return getMavenSession().getRequest().getToolchains().values().stream()
                .flatMap(Collection::stream)
                .map(org.apache.maven.toolchain.model.ToolchainModel::getDelegate)
                .toList();
    }

    @Nonnull
    @Override
    public Map<String, String> getUserProperties() {
        return Collections.unmodifiableMap(new PropertiesAsMap(getMavenSession().getUserProperties()));
    }

    @Nonnull
    @Override
    public Map<String, String> getSystemProperties() {
        return Collections.unmodifiableMap(new PropertiesAsMap(getMavenSession().getSystemProperties()));
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

    @Nonnull
    @Override
    public Version getMavenVersion() {
        return parseVersion(runtimeInformation.getMavenVersion());
    }

    @Override
    public int getDegreeOfConcurrency() {
        return getMavenSession().getRequest().getDegreeOfConcurrency();
    }

    @Nonnull
    @Override
    public Instant getStartTime() {
        return getMavenSession().getRequest().getStartInstant();
    }

    @Override
    public Path getRootDirectory() {
        return getMavenSession().getRequest().getRootDirectory();
    }

    @Override
    public Path getTopDirectory() {
        return getMavenSession().getRequest().getTopDirectory();
    }

    @Nonnull
    @Override
    public List<Project> getProjects() {
        return getProjects(getMavenSession().getProjects());
    }

    @Nonnull
    @Override
    public Map<String, Object> getPluginContext(Project project) {
        nonNull(project, "project");
        try {
            MojoExecution mojoExecution = lookup.lookup(MojoExecution.class);
            MojoDescriptor mojoDescriptor = mojoExecution.getMojoDescriptor();
            PluginDescriptor pluginDescriptor = mojoDescriptor.getPluginDescriptor();
            return getMavenSession().getPluginContext(pluginDescriptor, ((DefaultProject) project).getProject());
        } catch (LookupException e) {
            throw new MavenException("The PluginContext is only available during a mojo execution", e);
        }
    }

    @Override
    protected Session newSession(RepositorySystemSession repoSession, List<RemoteRepository> repositories) {
        final MavenSession ms = nonNull(getMavenSession());
        final MavenSession mss;
        if (repoSession != ms.getRepositorySession()) {
            mss = new MavenSession(repoSession, ms.getRequest(), ms.getResult());
        } else {
            mss = ms;
        }
        return newSession(mss, repositories);
    }

    protected Session newSession(MavenSession mavenSession, List<RemoteRepository> repositories) {
        return new DefaultSession(
                nonNull(mavenSession),
                getRepositorySystem(),
                repositories,
                mavenRepositorySystem,
                lookup,
                runtimeInformation);
    }

    public ArtifactRepository toArtifactRepository(RemoteRepository repository) {
        if (repository instanceof DefaultRemoteRepository defaultRemoteRepository) {
            org.eclipse.aether.repository.RemoteRepository rr = defaultRemoteRepository.getRepository();

            try {
                return mavenRepositorySystem.createRepository(
                        rr.getUrl(),
                        rr.getId(),
                        rr.getPolicy(false).isEnabled(),
                        rr.getPolicy(false).getUpdatePolicy(),
                        rr.getPolicy(true).isEnabled(),
                        rr.getPolicy(true).getUpdatePolicy(),
                        rr.getPolicy(false).getChecksumPolicy());

            } catch (Exception e) {
                throw new RuntimeException("Unable to create repository", e);
            }
        } else {
            // TODO
            throw new UnsupportedOperationException("Not yet implemented");
        }
    }
}
