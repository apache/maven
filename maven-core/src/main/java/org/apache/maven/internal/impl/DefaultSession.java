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
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

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
import org.apache.maven.rtinfo.RuntimeInformation;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;

import static org.apache.maven.internal.impl.Utils.map;
import static org.apache.maven.internal.impl.Utils.nonNull;

public class DefaultSession extends AbstractSession {

    private final MavenSession mavenSession;
    private final RepositorySystemSession session;
    private final RepositorySystem repositorySystem;
    private final List<RemoteRepository> repositories;
    private final MavenRepositorySystem mavenRepositorySystem;
    private final Lookup lookup;
    private final RuntimeInformation runtimeInformation;
    private final Map<Class<? extends Service>, Service> services = new ConcurrentHashMap<>();

    @SuppressWarnings("checkstyle:ParameterNumber")
    public DefaultSession(
            @Nonnull MavenSession session,
            @Nonnull RepositorySystem repositorySystem,
            @Nullable List<RemoteRepository> repositories,
            @Nonnull MavenRepositorySystem mavenRepositorySystem,
            @Nonnull Lookup lookup,
            @Nonnull RuntimeInformation runtimeInformation) {
        this.mavenSession = nonNull(session);
        this.session = mavenSession.getRepositorySession();
        this.repositorySystem = nonNull(repositorySystem);
        this.repositories = repositories != null
                ? repositories
                : map(
                        mavenSession.getRequest().getRemoteRepositories(),
                        r -> getRemoteRepository(RepositoryUtils.toRepo(r)));
        this.mavenRepositorySystem = mavenRepositorySystem;
        this.lookup = lookup;
        this.runtimeInformation = runtimeInformation;
    }

    public MavenSession getMavenSession() {
        return mavenSession;
    }

    @Nonnull
    @Override
    public LocalRepository getLocalRepository() {
        return new DefaultLocalRepository(session.getLocalRepository());
    }

    @Nonnull
    @Override
    public List<RemoteRepository> getRemoteRepositories() {
        return Collections.unmodifiableList(repositories);
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
        HashMap<String, String> result = new HashMap<>(new PropertiesAsMap(mavenSession.getSystemProperties()));
        if (project != null) {
            result.putAll(
                    new PropertiesAsMap(((DefaultProject) project).getProject().getProperties()));
        }
        result.putAll(new PropertiesAsMap(mavenSession.getUserProperties()));
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
        return mavenSession.getStartTime().toInstant();
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

    @Nonnull
    @Override
    public SessionData getData() {
        org.eclipse.aether.SessionData data = session.getData();
        return new SessionData() {
            @Override
            public void set(@Nonnull Object key, @Nullable Object value) {
                data.set(key, value);
            }

            @Override
            public boolean set(@Nonnull Object key, @Nullable Object oldValue, @Nullable Object newValue) {
                return data.set(key, oldValue, newValue);
            }

            @Nullable
            @Override
            public Object get(@Nonnull Object key) {
                return data.get(key);
            }

            @Nullable
            @Override
            public Object computeIfAbsent(@Nonnull Object key, @Nonnull Supplier<Object> supplier) {
                return data.computeIfAbsent(key, supplier);
            }
        };
    }

    @Nonnull
    @Override
    public Session withLocalRepository(@Nonnull LocalRepository localRepository) {
        nonNull(localRepository, "localRepository");
        if (session.getLocalRepository() != null
                && Objects.equals(session.getLocalRepository().getBasedir().toPath(), localRepository.getPath())) {
            return this;
        }
        org.eclipse.aether.repository.LocalRepository repository = toRepository(localRepository);
        org.eclipse.aether.repository.LocalRepositoryManager localRepositoryManager =
                repositorySystem.newLocalRepositoryManager(session, repository);

        RepositorySystemSession repoSession =
                new DefaultRepositorySystemSession(session).setLocalRepositoryManager(localRepositoryManager);
        MavenSession newSession = new MavenSession(repoSession, mavenSession.getRequest(), mavenSession.getResult());
        return new DefaultSession(
                newSession, repositorySystem, repositories, mavenRepositorySystem, lookup, runtimeInformation);
    }

    @Nonnull
    @Override
    public Session withRemoteRepositories(@Nonnull List<RemoteRepository> repositories) {
        return new DefaultSession(
                mavenSession, repositorySystem, repositories, mavenRepositorySystem, lookup, runtimeInformation);
    }

    @Nonnull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends Service> T getService(Class<T> clazz) throws NoSuchElementException {
        T t = (T) services.computeIfAbsent(clazz, this::lookup);
        if (t == null) {
            throw new NoSuchElementException(clazz.getName());
        }
        return t;
    }

    private Service lookup(Class<? extends Service> c) {
        try {
            return lookup.lookup(c);
        } catch (LookupException e) {
            NoSuchElementException nsee = new NoSuchElementException(c.getName());
            e.initCause(e);
            throw nsee;
        }
    }

    @Nonnull
    public RepositorySystemSession getSession() {
        return session;
    }

    @Nonnull
    public RepositorySystem getRepositorySystem() {
        return repositorySystem;
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

    public org.eclipse.aether.graph.Dependency toDependency(DependencyCoordinate dependency, boolean managed) {
        org.eclipse.aether.graph.Dependency dep;
        if (dependency instanceof DefaultDependencyCoordinate) {
            dep = ((DefaultDependencyCoordinate) dependency).getDependency();
        } else {
            dep = new org.eclipse.aether.graph.Dependency(
                    new org.eclipse.aether.artifact.DefaultArtifact(
                            dependency.getGroupId(),
                            dependency.getArtifactId(),
                            dependency.getClassifier(),
                            dependency.getType().getExtension(),
                            dependency.getVersion().toString(),
                            null),
                    dependency.getScope().id());
        }
        if (!managed && "".equals(dep.getScope())) {
            dep = dep.setScope(DependencyScope.COMPILE.id());
        }
        return dep;
    }
}
