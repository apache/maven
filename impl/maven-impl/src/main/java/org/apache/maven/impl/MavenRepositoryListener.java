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

import java.util.Collection;
import java.util.function.BiConsumer;

import org.apache.maven.api.RepositoryEvent;
import org.apache.maven.api.RepositoryListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bridges Maven Resolver repository events to the Maven public API.
 */
public final class MavenRepositoryListener extends org.eclipse.aether.AbstractRepositoryListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(MavenRepositoryListener.class);

    private void dispatch(
            org.eclipse.aether.RepositoryEvent event, BiConsumer<RepositoryListener, RepositoryEvent> consumer) {
        Object associatedSession = event.getSession().getData().get(InternalSession.class);
        if (!(associatedSession instanceof InternalSession session)) {
            return;
        }
        Collection<RepositoryListener> listeners = session.getRepositoryListeners();
        if (!listeners.isEmpty()) {
            RepositoryEvent repositoryEvent = new DefaultRepositoryEvent(session, event);
            for (RepositoryListener listener : listeners) {
                try {
                    consumer.accept(listener, repositoryEvent);
                } catch (RuntimeException e) {
                    LOGGER.warn(
                            "Failed to notify repository listener {} about {}",
                            listener.getClass().getName(),
                            event.getType(),
                            e);
                }
            }
        }
    }

    @Override
    public void artifactDescriptorInvalid(org.eclipse.aether.RepositoryEvent event) {
        dispatch(event, RepositoryListener::artifactDescriptorInvalid);
    }

    @Override
    public void artifactDescriptorMissing(org.eclipse.aether.RepositoryEvent event) {
        dispatch(event, RepositoryListener::artifactDescriptorMissing);
    }

    @Override
    public void metadataInvalid(org.eclipse.aether.RepositoryEvent event) {
        dispatch(event, RepositoryListener::metadataInvalid);
    }

    @Override
    public void artifactResolving(org.eclipse.aether.RepositoryEvent event) {
        dispatch(event, RepositoryListener::artifactResolving);
    }

    @Override
    public void artifactResolved(org.eclipse.aether.RepositoryEvent event) {
        dispatch(event, RepositoryListener::artifactResolved);
    }

    @Override
    public void metadataResolving(org.eclipse.aether.RepositoryEvent event) {
        dispatch(event, RepositoryListener::metadataResolving);
    }

    @Override
    public void metadataResolved(org.eclipse.aether.RepositoryEvent event) {
        dispatch(event, RepositoryListener::metadataResolved);
    }

    @Override
    public void artifactDownloading(org.eclipse.aether.RepositoryEvent event) {
        dispatch(event, RepositoryListener::artifactDownloading);
    }

    @Override
    public void artifactDownloaded(org.eclipse.aether.RepositoryEvent event) {
        dispatch(event, RepositoryListener::artifactDownloaded);
    }

    @Override
    public void metadataDownloading(org.eclipse.aether.RepositoryEvent event) {
        dispatch(event, RepositoryListener::metadataDownloading);
    }

    @Override
    public void metadataDownloaded(org.eclipse.aether.RepositoryEvent event) {
        dispatch(event, RepositoryListener::metadataDownloaded);
    }

    @Override
    public void artifactInstalling(org.eclipse.aether.RepositoryEvent event) {
        dispatch(event, RepositoryListener::artifactInstalling);
    }

    @Override
    public void artifactInstalled(org.eclipse.aether.RepositoryEvent event) {
        dispatch(event, RepositoryListener::artifactInstalled);
    }

    @Override
    public void metadataInstalling(org.eclipse.aether.RepositoryEvent event) {
        dispatch(event, RepositoryListener::metadataInstalling);
    }

    @Override
    public void metadataInstalled(org.eclipse.aether.RepositoryEvent event) {
        dispatch(event, RepositoryListener::metadataInstalled);
    }

    @Override
    public void artifactDeploying(org.eclipse.aether.RepositoryEvent event) {
        dispatch(event, RepositoryListener::artifactDeploying);
    }

    @Override
    public void artifactDeployed(org.eclipse.aether.RepositoryEvent event) {
        dispatch(event, RepositoryListener::artifactDeployed);
    }

    @Override
    public void metadataDeploying(org.eclipse.aether.RepositoryEvent event) {
        dispatch(event, RepositoryListener::metadataDeploying);
    }

    @Override
    public void metadataDeployed(org.eclipse.aether.RepositoryEvent event) {
        dispatch(event, RepositoryListener::metadataDeployed);
    }
}
