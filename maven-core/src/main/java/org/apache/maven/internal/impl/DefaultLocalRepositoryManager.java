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
import javax.inject.Named;
import javax.inject.Singleton;
import org.apache.maven.api.Artifact;
import org.apache.maven.api.LocalRepository;
import org.apache.maven.api.RemoteRepository;
import org.apache.maven.api.Session;
import org.apache.maven.api.services.LocalRepositoryManager;

@Named
@Singleton
public class DefaultLocalRepositoryManager implements LocalRepositoryManager {

    @Override
    public Path getPathForLocalArtifact(Session session, LocalRepository local, Artifact artifact) {
        DefaultSession s = (DefaultSession) session;
        String path = getManager(s, local).getPathForLocalArtifact(s.toArtifact(artifact));
        return local.getPath().resolve(path);
    }

    @Override
    public Path getPathForRemoteArtifact(
            Session session, LocalRepository local, RemoteRepository remote, Artifact artifact) {
        DefaultSession s = (DefaultSession) session;
        String path =
                getManager(s, local).getPathForRemoteArtifact(s.toArtifact(artifact), s.toRepository(remote), null);
        return local.getPath().resolve(path);
    }

    private org.eclipse.aether.repository.LocalRepositoryManager getManager(
            DefaultSession session, LocalRepository local) {
        org.eclipse.aether.repository.LocalRepository repository = session.toRepository(local);
        if ("enhanced".equals(repository.getContentType())) {
            repository = new org.eclipse.aether.repository.LocalRepository(repository.getBasedir(), "");
        }
        return session.getRepositorySystem().newLocalRepositoryManager(session.getSession(), repository);
    }
}
