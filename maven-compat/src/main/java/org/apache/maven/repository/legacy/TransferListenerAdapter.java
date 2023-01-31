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
package org.apache.maven.repository.legacy;

import java.util.IdentityHashMap;
import java.util.Map;

import org.apache.maven.repository.ArtifactTransferEvent;
import org.apache.maven.repository.ArtifactTransferListener;
import org.apache.maven.repository.ArtifactTransferResource;
import org.apache.maven.wagon.events.TransferEvent;
import org.apache.maven.wagon.events.TransferListener;
import org.apache.maven.wagon.repository.Repository;
import org.apache.maven.wagon.resource.Resource;

/**
 * TransferListenerAdapter
 */
public class TransferListenerAdapter implements TransferListener {

    private final ArtifactTransferListener listener;

    private final Map<Resource, ArtifactTransferResource> artifacts;

    private final Map<Resource, Long> transfers;

    public static TransferListener newAdapter(ArtifactTransferListener listener) {
        if (listener == null) {
            return null;
        } else {
            return new TransferListenerAdapter(listener);
        }
    }

    private TransferListenerAdapter(ArtifactTransferListener listener) {
        this.listener = listener;
        this.artifacts = new IdentityHashMap<>();
        this.transfers = new IdentityHashMap<>();
    }

    public void debug(String message) {}

    public void transferCompleted(TransferEvent transferEvent) {
        ArtifactTransferEvent event = wrap(transferEvent);

        Long transferred;
        synchronized (transfers) {
            transferred = transfers.remove(transferEvent.getResource());
        }
        if (transferred != null) {
            event.setTransferredBytes(transferred);
        }

        synchronized (artifacts) {
            artifacts.remove(transferEvent.getResource());
        }

        listener.transferCompleted(event);
    }

    public void transferError(TransferEvent transferEvent) {
        synchronized (transfers) {
            transfers.remove(transferEvent.getResource());
        }
        synchronized (artifacts) {
            artifacts.remove(transferEvent.getResource());
        }
    }

    public void transferInitiated(TransferEvent transferEvent) {
        listener.transferInitiated(wrap(transferEvent));
    }

    public void transferProgress(TransferEvent transferEvent, byte[] buffer, int length) {
        Long transferred;
        synchronized (transfers) {
            transferred = transfers.get(transferEvent.getResource());
            if (transferred == null) {
                transferred = (long) length;
            } else {
                transferred = transferred + length;
            }
            transfers.put(transferEvent.getResource(), transferred);
        }

        ArtifactTransferEvent event = wrap(transferEvent);
        event.setDataBuffer(buffer);
        event.setDataOffset(0);
        event.setDataLength(length);
        event.setTransferredBytes(transferred);

        listener.transferProgress(event);
    }

    public void transferStarted(TransferEvent transferEvent) {
        listener.transferStarted(wrap(transferEvent));
    }

    private ArtifactTransferEvent wrap(TransferEvent event) {
        if (event == null) {
            return null;
        } else {
            String wagon = event.getWagon().getClass().getName();

            ArtifactTransferResource artifact = wrap(event.getWagon().getRepository(), event.getResource());

            ArtifactTransferEvent evt;
            if (event.getException() != null) {
                evt = new ArtifactTransferEvent(wagon, event.getException(), event.getRequestType(), artifact);
            } else {
                evt = new ArtifactTransferEvent(wagon, event.getEventType(), event.getRequestType(), artifact);
            }

            evt.setLocalFile(event.getLocalFile());

            return evt;
        }
    }

    private ArtifactTransferResource wrap(Repository repository, Resource resource) {
        if (resource == null) {
            return null;
        } else {
            synchronized (artifacts) {
                ArtifactTransferResource artifact = artifacts.get(resource);

                if (artifact == null) {
                    artifact = new MavenArtifact(repository.getUrl(), resource);
                    artifacts.put(resource, artifact);
                }

                return artifact;
            }
        }
    }
}
