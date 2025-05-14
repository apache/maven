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
package org.apache.maven.impl;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import org.apache.maven.api.LocalRepository;
import org.apache.maven.api.RemoteRepository;
import org.apache.maven.api.Session;
import org.apache.maven.api.di.Inject;
import org.apache.maven.api.di.Named;
import org.apache.maven.api.di.Singleton;
import org.apache.maven.api.model.Repository;
import org.apache.maven.api.services.RepositoryFactory;
import org.eclipse.aether.impl.RemoteRepositoryManager;
import org.eclipse.aether.repository.RepositoryPolicy;

@Named
@Singleton
public class DefaultRepositoryFactory implements RepositoryFactory {

    final RemoteRepositoryManager remoteRepositoryManager;

    @Inject
    public DefaultRepositoryFactory(RemoteRepositoryManager remoteRepositoryManager) {
        this.remoteRepositoryManager = remoteRepositoryManager;
    }

    @Override
    public LocalRepository createLocal(Path path) {
        return new DefaultLocalRepository(new org.eclipse.aether.repository.LocalRepository(path.toAbsolutePath()));
    }

    @Override
    public RemoteRepository createRemote(String id, String url) {
        return new DefaultRemoteRepository(
                new org.eclipse.aether.repository.RemoteRepository.Builder(id, "default", url).build());
    }

    @Override
    public RemoteRepository createRemote(Repository repository) throws IllegalArgumentException {
        return new DefaultRemoteRepository(new org.eclipse.aether.repository.RemoteRepository.Builder(
                        repository.getId(), repository.getLayout(), repository.getUrl())
                .setReleasePolicy(buildRepositoryPolicy(repository.getReleases()))
                .setSnapshotPolicy(buildRepositoryPolicy(repository.getSnapshots()))
                .build());
    }

    @Override
    public List<RemoteRepository> aggregate(
            Session session,
            List<RemoteRepository> dominant,
            List<RemoteRepository> recessive,
            boolean processRecessive) {
        InternalSession internalSession =
                InternalSession.from(Objects.requireNonNull(session, "session cannot be null"));
        List<org.eclipse.aether.repository.RemoteRepository> repos = remoteRepositoryManager.aggregateRepositories(
                internalSession.getSession(),
                internalSession.toRepositories(Objects.requireNonNull(dominant, "dominant cannot be null")),
                internalSession.toRepositories(Objects.requireNonNull(recessive, "recessive cannot be null")),
                processRecessive);
        return repos.stream()
                .<RemoteRepository>map(DefaultRemoteRepository::new)
                .toList();
    }

    public static org.eclipse.aether.repository.RepositoryPolicy buildRepositoryPolicy(
            org.apache.maven.api.model.RepositoryPolicy policy) {
        boolean enabled = true;
        String updatePolicy = RepositoryPolicy.UPDATE_POLICY_DAILY;
        String checksumPolicy = RepositoryPolicy.CHECKSUM_POLICY_FAIL;
        if (policy != null) {
            enabled = policy.isEnabled();
            if (policy.getUpdatePolicy() != null) {
                updatePolicy = policy.getUpdatePolicy();
            }
            if (policy.getChecksumPolicy() != null) {
                checksumPolicy = policy.getChecksumPolicy();
            }
        }
        return new org.eclipse.aether.repository.RepositoryPolicy(enabled, updatePolicy, checksumPolicy);
    }
}
