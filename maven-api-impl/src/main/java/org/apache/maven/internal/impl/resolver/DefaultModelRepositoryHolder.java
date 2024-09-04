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
package org.apache.maven.internal.impl.resolver;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.maven.api.RemoteRepository;
import org.apache.maven.api.Session;
import org.apache.maven.api.model.Repository;
import org.apache.maven.api.services.ModelBuilderRequest.RepositoryMerging;
import org.apache.maven.api.services.RepositoryFactory;

public class DefaultModelRepositoryHolder {

    final Session session;
    final RepositoryMerging repositoryMerging;

    List<RemoteRepository> pomRepositories;
    List<RemoteRepository> repositories;
    List<RemoteRepository> externalRepositories;
    Set<String> ids;

    public DefaultModelRepositoryHolder(
            Session session, RepositoryMerging repositoryMerging, List<RemoteRepository> externalRepositories) {
        this.session = session;
        this.repositoryMerging = repositoryMerging;
        this.pomRepositories = List.of();
        this.externalRepositories = List.copyOf(externalRepositories);
        this.repositories = List.copyOf(externalRepositories);
        this.ids = new HashSet<>();
    }

    protected DefaultModelRepositoryHolder(DefaultModelRepositoryHolder holder) {
        this.session = holder.session;
        this.repositoryMerging = holder.repositoryMerging;
        this.pomRepositories = List.copyOf(holder.pomRepositories);
        this.externalRepositories = List.copyOf(holder.externalRepositories);
        this.repositories = List.copyOf(holder.repositories);
    }

    public void merge(List<Repository> toAdd, boolean replace) {
        List<RemoteRepository> repos =
                toAdd.stream().map(session::createRemoteRepository).toList();
        if (replace) {
            Set<String> ids = repos.stream().map(RemoteRepository::getId).collect(Collectors.toSet());
            repositories =
                    repositories.stream().filter(r -> !ids.contains(r.getId())).toList();
            pomRepositories = pomRepositories.stream()
                    .filter(r -> !ids.contains(r.getId()))
                    .toList();
        } else {
            Set<String> ids =
                    pomRepositories.stream().map(RemoteRepository::getId).collect(Collectors.toSet());
            repos = repos.stream().filter(r -> !ids.contains(r.getId())).toList();
        }

        RepositoryFactory repositoryFactory = session.getService(RepositoryFactory.class);
        if (repositoryMerging == RepositoryMerging.REQUEST_DOMINANT) {
            repositories = repositoryFactory.aggregate(session, repositories, repos, true);
            pomRepositories = repositories;
        } else {
            pomRepositories = repositoryFactory.aggregate(session, pomRepositories, repos, true);
            repositories = repositoryFactory.aggregate(session, pomRepositories, externalRepositories, false);
        }
    }

    public List<org.apache.maven.api.RemoteRepository> getRepositories() {
        return List.copyOf(repositories);
    }

    public DefaultModelRepositoryHolder copy() {
        return new DefaultModelRepositoryHolder(this);
    }
}
