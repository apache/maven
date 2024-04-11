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
import java.util.*;

import org.apache.maven.RepositoryUtils;
import org.apache.maven.api.*;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.annotations.Nullable;
import org.apache.maven.api.services.Lookup;
import org.apache.maven.api.services.LookupException;
import org.apache.maven.api.services.MavenException;
import org.apache.maven.api.settings.Settings;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.bridge.MavenRepositorySystem;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;
import org.apache.maven.rtinfo.RuntimeInformation;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;

import static org.apache.maven.internal.impl.Utils.map;
import static org.apache.maven.internal.impl.Utils.nonNull;

public class DefaultSession extends AbstractSession implements InternalMavenSession {

    private final MavenSession mavenSession;
    private final MavenRepositorySystem mavenRepositorySystem;
    private final RuntimeInformation runtimeInformation;
    private final Map<String, Project> allProjects = Collections.synchronizedMap(new WeakHashMap<>());

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
        return mavenSession;
    }

    @Override
    public List<Project> getProjects(List<MavenProject> projects) {
        return projects == null ? null : map(projects, this::getProject);
    }

    @Override
    public Project getProject(MavenProject project) {
        return allProjects.computeIfAbsent(project.getId(), id -> new DefaultProject(this, project));
    }

    @Override
    public List<ArtifactRepository> toArtifactRepositories(List<RemoteRepository> repositories) {
        return repositories == null ? null : map(repositories, this::toArtifactRepository);
    }

    @Nonnull
    @Override
    public Settings getSettings() {
        return mavenSession.getSettings().getDelegate();
    }

    @Nonnull
    @Override
    public Map<String, String> getUserProperties() {
        return Collections.unmodifiableMap(new PropertiesAsMap(mavenSession.getUserProperties()));
    }

    @Nonnull
    @Override
    public Map<String, String> getSystemProperties() {
        return Collections.unmodifiableMap(new PropertiesAsMap(mavenSession.getSystemProperties()));
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
        return mavenSession.getRequest().getDegreeOfConcurrency();
    }

    @Nonnull
    @Override
    public Instant getStartTime() {
        return mavenSession.getRequest().getStartTime().toInstant();
    }

    @Override
    public Path getRootDirectory() {
        return mavenSession.getRequest().getRootDirectory();
    }

    @Override
    public Path getTopDirectory() {
        return mavenSession.getRequest().getTopDirectory();
    }

    @Nonnull
    @Override
    public List<Project> getProjects() {
        return getProjects(mavenSession.getProjects());
    }

    @Nonnull
    @Override
    public Map<String, Object> getPluginContext(Project project) {
        nonNull(project, "project");
        try {
            MojoExecution mojoExecution = lookup.lookup(MojoExecution.class);
            MojoDescriptor mojoDescriptor = mojoExecution.getMojoDescriptor();
            PluginDescriptor pluginDescriptor = mojoDescriptor.getPluginDescriptor();
            return mavenSession.getPluginContext(pluginDescriptor, ((DefaultProject) project).getProject());
        } catch (LookupException e) {
            throw new MavenException("The PluginContext is only available during a mojo execution", e);
        }
    }

    protected Session newSession(RepositorySystemSession repoSession, List<RemoteRepository> repositories) {
        final MavenSession ms = nonNull(mavenSession);
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
        if (repository instanceof DefaultRemoteRepository) {
            org.eclipse.aether.repository.RemoteRepository rr = ((DefaultRemoteRepository) repository).getRepository();

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
