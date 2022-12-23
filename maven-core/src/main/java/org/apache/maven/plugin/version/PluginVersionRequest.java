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

import java.util.List;

import org.apache.maven.model.Model;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;

/**
 * Collects settings required to resolve the version for a plugin.
 *
 * @since 3.0
 * @author Benjamin Bentmann
 */
public interface PluginVersionRequest {

    /**
     * Gets the group id of the plugin.
     *
     * @return The group id of the plugin.
     */
    String getGroupId();

    /**
     * Sets the group id of the plugin.
     *
     * @param groupId The group id of the plugin.
     * @return This request, never {@code null}.
     */
    PluginVersionRequest setGroupId(String groupId);

    /**
     * Gets the artifact id of the plugin.
     *
     * @return The artifact id of the plugin.
     */
    String getArtifactId();

    /**
     * Sets the artifact id of the plugin.
     *
     * @param artifactId The artifact id of the plugin.
     * @return This request, never {@code null}.
     */
    PluginVersionRequest setArtifactId(String artifactId);

    /**
     * Gets the POM whose build plugins are to be scanned for the version.
     *
     * @return The POM whose build plugins are to be scanned for the version or {@code null} to only search the plugin
     *         repositories.
     */
    Model getPom();

    /**
     * Sets the POM whose build plugins are to be scanned for the version.
     *
     * @param pom The POM whose build plugins are to be scanned for the version, may be {@code null} to only search the
     *            plugin repositories.
     * @return This request, never {@code null}.
     */
    PluginVersionRequest setPom(Model pom);

    /**
     * Gets the remote repositories to use.
     *
     * @return The remote repositories to use, never {@code null}.
     */
    List<RemoteRepository> getRepositories();

    /**
     * Sets the remote repositories to use. <em>Note:</em> When creating a request from a project, be sure to use the
     * plugin repositories and not the regular project repositories.
     *
     * @param repositories The remote repositories to use.
     * @return This request, never {@code null}.
     */
    PluginVersionRequest setRepositories(List<RemoteRepository> repositories);

    /**
     * Gets the session to use for repository access.
     *
     * @return The repository session or {@code null} if not set.
     */
    RepositorySystemSession getRepositorySession();

    /**
     * Sets the session to use for repository access.
     *
     * @param repositorySession The repository session to use.
     * @return This request, never {@code null}.
     */
    PluginVersionRequest setRepositorySession(RepositorySystemSession repositorySession);
}
