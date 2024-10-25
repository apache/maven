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
package org.apache.maven.plugin.version;

import java.util.Collections;
import java.util.List;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;

/**
 * Collects settings required to resolve the version for a plugin.
 *
 * @since 3.0
 */
public class DefaultPluginVersionRequest implements PluginVersionRequest {

    private String groupId;

    private String artifactId;

    private Model pom;

    private List<RemoteRepository> repositories = Collections.emptyList();

    private RepositorySystemSession session;

    /**
     * Creates an empty request.
     */
    public DefaultPluginVersionRequest() {}

    /**
     * Creates a request for the specified plugin by copying settings from the specified build session. If the session
     * has a current project, its plugin repositories will be used as well.
     *
     * @param plugin The plugin for which to resolve a version, must not be {@code null}.
     * @param session The Maven session to use, must not be {@code null}.
     */
    public DefaultPluginVersionRequest(Plugin plugin, MavenSession session) {
        setGroupId(plugin.getGroupId());
        setArtifactId(plugin.getArtifactId());

        setRepositorySession(session.getRepositorySession());

        MavenProject project = session.getCurrentProject();
        if (project != null) {
            setRepositories(project.getRemotePluginRepositories());
        }
    }

    /**
     * Creates a request for the specified plugin using the given repository session and plugin repositories.
     *
     * @param plugin The plugin for which to resolve a version, must not be {@code null}.
     * @param session The repository session to use, must not be {@code null}.
     * @param repositories The plugin repositories to query, may be {@code null}.
     */
    public DefaultPluginVersionRequest(
            Plugin plugin, RepositorySystemSession session, List<RemoteRepository> repositories) {
        setGroupId(plugin.getGroupId());
        setArtifactId(plugin.getArtifactId());

        setRepositorySession(session);

        setRepositories(repositories);
    }

    public String getGroupId() {
        return groupId;
    }

    public DefaultPluginVersionRequest setGroupId(String groupId) {
        this.groupId = groupId;

        return this;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public DefaultPluginVersionRequest setArtifactId(String artifactId) {
        this.artifactId = artifactId;

        return this;
    }

    public Model getPom() {
        return pom;
    }

    public DefaultPluginVersionRequest setPom(Model pom) {
        this.pom = pom;

        return this;
    }

    public List<RemoteRepository> getRepositories() {
        return repositories;
    }

    public DefaultPluginVersionRequest setRepositories(List<RemoteRepository> repositories) {
        if (repositories != null) {
            this.repositories = Collections.unmodifiableList(repositories);
        } else {
            this.repositories = Collections.emptyList();
        }

        return this;
    }

    public RepositorySystemSession getRepositorySession() {
        return session;
    }

    public DefaultPluginVersionRequest setRepositorySession(RepositorySystemSession session) {
        this.session = session;

        return this;
    }
}
