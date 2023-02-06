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
package org.apache.maven.artifact.repository;

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;

/**
 * Collects basic settings to access the repository system.
 *
 * @author Benjamin Bentmann
 */
public class DefaultRepositoryRequest implements RepositoryRequest {

    private boolean offline;

    private boolean forceUpdate;

    private ArtifactRepository localRepository;

    private List<ArtifactRepository> remoteRepositories;

    /**
     * Creates an empty repository request.
     */
    public DefaultRepositoryRequest() {
        // enables no-arg constructor
    }

    /**
     * Creates a shallow copy of the specified repository request.
     *
     * @param repositoryRequest The repository request to copy from, must not be {@code null}.
     */
    public DefaultRepositoryRequest(RepositoryRequest repositoryRequest) {
        setLocalRepository(repositoryRequest.getLocalRepository());
        setRemoteRepositories(repositoryRequest.getRemoteRepositories());
        setOffline(repositoryRequest.isOffline());
        setForceUpdate(repositoryRequest.isForceUpdate());
    }

    public static RepositoryRequest getRepositoryRequest(MavenSession session, MavenProject project) {
        RepositoryRequest request = new DefaultRepositoryRequest();

        request.setLocalRepository(session.getLocalRepository());
        if (project != null) {
            request.setRemoteRepositories(project.getPluginArtifactRepositories());
        }
        request.setOffline(session.isOffline());
        request.setForceUpdate(session.getRequest().isUpdateSnapshots());

        return request;
    }

    public boolean isOffline() {
        return offline;
    }

    public DefaultRepositoryRequest setOffline(boolean offline) {
        this.offline = offline;

        return this;
    }

    public boolean isForceUpdate() {
        return forceUpdate;
    }

    public DefaultRepositoryRequest setForceUpdate(boolean forceUpdate) {
        this.forceUpdate = forceUpdate;

        return this;
    }

    public ArtifactRepository getLocalRepository() {
        return localRepository;
    }

    public DefaultRepositoryRequest setLocalRepository(ArtifactRepository localRepository) {
        this.localRepository = localRepository;

        return this;
    }

    public List<ArtifactRepository> getRemoteRepositories() {
        if (remoteRepositories == null) {
            remoteRepositories = new ArrayList<>();
        }

        return remoteRepositories;
    }

    public DefaultRepositoryRequest setRemoteRepositories(List<ArtifactRepository> remoteRepositories) {
        this.remoteRepositories = remoteRepositories;

        return this;
    }
}
