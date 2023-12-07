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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import java.util.List;
import java.util.Optional;

import org.apache.maven.api.Repository;
import org.apache.maven.api.Version;
import org.apache.maven.api.services.VersionResolver;
import org.apache.maven.api.services.VersionResolverException;
import org.apache.maven.api.services.VersionResolverRequest;
import org.apache.maven.api.services.VersionResolverResult;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.resolution.VersionRequest;
import org.eclipse.aether.resolution.VersionResolutionException;
import org.eclipse.aether.resolution.VersionResult;

import static org.apache.maven.internal.impl.Utils.nonNull;

@Named
@Singleton
public class DefaultVersionResolver implements VersionResolver {

    private final RepositorySystem repositorySystem;

    @Inject
    public DefaultVersionResolver(RepositorySystem repositorySystem) {
        this.repositorySystem = repositorySystem;
    }

    @Override
    public VersionResolverResult resolve(VersionResolverRequest request) throws VersionResolverException {
        nonNull(request, "request");
        InternalSession session = InternalSession.from(request.getSession());

        try {
            VersionResult res = repositorySystem.resolveVersion(
                    session.getSession(),
                    new VersionRequest(
                            session.toArtifact(request.getArtifactCoordinate()),
                            session.toRepositories(session.getRemoteRepositories()),
                            null));

            return new VersionResolverResult() {
                @Override
                public List<Exception> getExceptions() {
                    return res.getExceptions();
                }

                @Override
                public Version getVersion() {
                    return session.parseVersion(res.getVersion());
                }

                @Override
                public Optional<Repository> getRepository() {
                    if (res.getRepository() instanceof org.eclipse.aether.repository.LocalRepository) {
                        return Optional.of(new DefaultLocalRepository(
                                (org.eclipse.aether.repository.LocalRepository) res.getRepository()));
                    } else if (res.getRepository() instanceof org.eclipse.aether.repository.RemoteRepository) {
                        return Optional.of(new DefaultRemoteRepository(
                                (org.eclipse.aether.repository.RemoteRepository) res.getRepository()));
                    } else {
                        return Optional.empty();
                    }
                }
            };
        } catch (VersionResolutionException e) {
            throw new VersionResolverException("Unable to resolve version", e);
        }
    }
}
