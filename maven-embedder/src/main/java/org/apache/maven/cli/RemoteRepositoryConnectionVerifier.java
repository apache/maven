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
package org.apache.maven.cli;

import java.net.URI;
import java.util.Optional;

import org.apache.maven.RepositoryUtils;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.spi.connector.transport.GetTask;
import org.eclipse.aether.spi.connector.transport.Transporter;
import org.eclipse.aether.spi.connector.transport.TransporterProvider;
import org.eclipse.aether.transfer.NoTransporterException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper class to verify connection to a remote repository.
 */
public class RemoteRepositoryConnectionVerifier {
    private final Logger logger;
    private final TransporterProvider transporterProvider;

    public RemoteRepositoryConnectionVerifier(final PlexusContainer container) throws ComponentLookupException {
        this.logger = LoggerFactory.getILoggerFactory().getLogger(RemoteRepositoryConnectionVerifier.class.getName());
        this.transporterProvider = container.lookup(TransporterProvider.class);
    }

    public Optional<String> verifyConnectionToRemoteRepository(
            final RepositorySystemSession session, final ArtifactRepository artifactRepository) {
        final RemoteRepository repository = RepositoryUtils.toRepo(artifactRepository);

        try {
            final Transporter transporter = transporterProvider.newTransporter(session, repository);
            final Optional<String> issue = verifyConnectionUsingTransport(transporter, repository);

            if (!issue.isPresent()) {
                logger.info(
                        "Connection check for repository '{}' at '{}' completed",
                        repository.getId(),
                        repository.getUrl());
            }

            return issue;
        } catch (final NoTransporterException nte) {
            final String message = String.format(
                    "There is no compatible transport for remote repository '%s' with location '%s'",
                    repository.getId(), repository.getUrl());
            return Optional.of(message);
        }
    }

    private Optional<String> verifyConnectionUsingTransport(
            final Transporter transporter, final RemoteRepository remoteRepository) {
        try {
            final GetTask task = new GetTask(URI.create(""));
            transporter.get(task);
            // We could connect, but uncertain to what. Could be the repository, could be a valid web page.
            return Optional.empty();
        } catch (final Exception e) {
            final int errorOrArtifactNotFound = transporter.classify(e);
            if (Transporter.ERROR_NOT_FOUND == errorOrArtifactNotFound) {
                // No-op since we could connect to the repository
                // However we do not know what should or shouldn't be present
                return Optional.empty();
            }
            // In this case it is Transporter.ERROR_OTHER
            return Optional.of(formatException(remoteRepository, e));
        }
    }

    private String formatException(final RemoteRepository remoteRepository, final Exception e) {
        final String repositoryId = remoteRepository.getId();
        final String repositoryUrl = remoteRepository.getUrl();
        final String repository = String.format("%s [%s]", repositoryId, repositoryUrl);

        return String.format("Connection to %s not possible. Cause: %s", repository, e.getMessage());
    }
}
