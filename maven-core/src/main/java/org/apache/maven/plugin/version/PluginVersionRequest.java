package org.apache.maven.plugin.version;

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

import java.util.List;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.RepositoryCache;
import org.apache.maven.artifact.repository.RepositoryRequest;
import org.apache.maven.model.Model;
import org.apache.maven.repository.ArtifactTransferListener;

/**
 * Collects settings required to resolve the version for a plugin.
 * 
 * @since 3.0-alpha-3
 * @author Benjamin Bentmann
 */
public interface PluginVersionRequest
    extends RepositoryRequest
{

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
    PluginVersionRequest setGroupId( String groupId );

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
    PluginVersionRequest setArtifactId( String artifactId );

    /**
     * Gets the POM whose build plugins are to be scanned for the version.
     * 
     * @return The POM whose build plugins are to be scanned for the verion or {@code null} to only search the plugin
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
    PluginVersionRequest setPom( Model pom );

    /**
     * Indicates whether network access to remote repositories has been disabled.
     * 
     * @return {@code true} if remote access has been disabled, {@code false} otherwise.
     */
    boolean isOffline();

    /**
     * Enables/disables network access to remote repositories.
     * 
     * @param offline {@code true} to disable remote access, {@code false} to allow network access.
     * @return This request, never {@code null}.
     */
    PluginVersionRequest setOffline( boolean offline );

    /**
     * Gets the local repository to use.
     * 
     * @return The local repository to use or {@code null} if not set.
     */
    ArtifactRepository getLocalRepository();

    /**
     * Sets the local repository to use.
     * 
     * @param localRepository The local repository to use.
     * @return This request, never {@code null}.
     */
    PluginVersionRequest setLocalRepository( ArtifactRepository localRepository );

    /**
     * Gets the remote repositories to use.
     * 
     * @return The remote repositories to use, never {@code null}.
     */
    List<ArtifactRepository> getRemoteRepositories();

    /**
     * Sets the remote repositories to use. <em>Note:</em> When creating a request from a project, be sure to use the
     * plugin artifact repositories and not the regular artifact repositories.
     * 
     * @param remoteRepositories The remote repositories to use.
     * @return This request, never {@code null}.
     */
    PluginVersionRequest setRemoteRepositories( List<ArtifactRepository> remoteRepositories );

    /**
     * Gets the repository cache to use.
     * 
     * @return The repository cache to use or {@code null} if none.
     */
    RepositoryCache getCache();

    /**
     * Sets the repository cache to use.
     * 
     * @param cache The repository cache to use, may be {@code null}.
     * @return This request, never {@code null}.
     */
    PluginVersionRequest setCache( RepositoryCache cache );

    /**
     * Gets the listener to notify of transfer events.
     * 
     * @return The transfer listener or {@code null} if none.
     */
    ArtifactTransferListener getTransferListener();

    /**
     * Sets the listener to notify of transfer events.
     * 
     * @param transferListener The transfer listener to notify, may be {@code null}.
     * @return This request, never {@code null}.
     */
    PluginVersionRequest setTransferListener( ArtifactTransferListener transferListener );

}
