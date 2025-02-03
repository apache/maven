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

import org.apache.maven.api.Artifact;
import org.apache.maven.api.LocalRepository;
import org.apache.maven.api.RemoteRepository;
import org.apache.maven.api.Session;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.di.Named;
import org.apache.maven.api.di.Singleton;
import org.apache.maven.api.services.LocalRepositoryManager;

@Named
@Singleton
public class DefaultLocalRepositoryManager implements LocalRepositoryManager {

    @Nonnull
    @Override
    public Path getPathForLocalArtifact(
            @Nonnull Session session, @Nonnull LocalRepository local, @Nonnull Artifact artifact) {
        InternalSession s = InternalSession.from(session);
        return getManager(s, local).getAbsolutePathForLocalArtifact(s.toArtifact(artifact));
    }

    @Nonnull
    @Override
    public Path getPathForRemoteArtifact(
            @Nonnull Session session,
            @Nonnull LocalRepository local,
            @Nonnull RemoteRepository remote,
            @Nonnull Artifact artifact) {
        InternalSession s = InternalSession.from(session);
        return getManager(s, local)
                .getAbsolutePathForRemoteArtifact(s.toArtifact(artifact), s.toRepository(remote), null);
    }

    private org.eclipse.aether.repository.LocalRepositoryManager getManager(
            InternalSession session, LocalRepository local) {
        org.eclipse.aether.repository.LocalRepository repository = session.toRepository(local);
        if ("enhanced".equals(repository.getContentType())) {
            repository = new org.eclipse.aether.repository.LocalRepository(repository.getBasePath(), "");
        }
        return session.getRepositorySystem().newLocalRepositoryManager(session.getSession(), repository);
    }
}
