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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import org.apache.maven.api.Artifact;
import org.apache.maven.api.Listener;
import org.apache.maven.api.RemoteRepository;
import org.apache.maven.api.RepositoryEvent;
import org.apache.maven.api.RepositoryEventType;
import org.apache.maven.api.RepositoryListener;
import org.apache.maven.api.RepositoryMetadata;
import org.apache.maven.api.Session;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.metadata.DefaultMetadata;
import org.eclipse.aether.metadata.Metadata;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MavenRepositoryListenerTest {

    @Test
    void repositoryListenerOverloadDoesNotMakeBuildListenerLambdaAmbiguous() {
        Session session = mock(Session.class);

        session.registerListener(event -> {});

        verify(session).registerListener(org.mockito.ArgumentMatchers.any(Listener.class));
    }

    @Test
    void ignoresEventsBeforeMavenSessionAssociation() {
        DefaultRepositorySystemSession resolverSession = new DefaultRepositorySystemSession(h -> false);
        MavenRepositoryListener bridge = new MavenRepositoryListener();

        bridge.artifactResolving(new org.eclipse.aether.RepositoryEvent.Builder(
                        resolverSession, org.eclipse.aether.RepositoryEvent.EventType.ARTIFACT_RESOLVING)
                .build());
    }

    @Test
    void dispatchesAllRepositoryEventTypes() {
        TestContext context = new TestContext();
        RecordingListener listener = new RecordingListener();
        when(context.session.getRepositoryListeners()).thenReturn(List.of(listener));

        for (org.eclipse.aether.RepositoryEvent.EventType type :
                org.eclipse.aether.RepositoryEvent.EventType.values()) {
            dispatch(
                    context.bridge,
                    new org.eclipse.aether.RepositoryEvent.Builder(context.resolverSession, type).build());
        }

        assertEquals(List.of(RepositoryEventType.values()), listener.types);
    }

    @Test
    void convertsArtifactRepositoryFailurePathAndTrace() {
        TestContext context = new TestContext();
        RecordingListener listener = new RecordingListener();
        when(context.session.getRepositoryListeners()).thenReturn(List.of(listener));

        org.eclipse.aether.artifact.Artifact resolverArtifact =
                new org.eclipse.aether.artifact.DefaultArtifact("org.example:demo:jar:1.0");
        Artifact artifact = mock(Artifact.class);
        when(context.session.getArtifact(resolverArtifact)).thenReturn(artifact);
        org.eclipse.aether.repository.RemoteRepository resolverRepository =
                new org.eclipse.aether.repository.RemoteRepository.Builder("central", "default", "https://repo.example")
                        .build();
        RemoteRepository repository = mock(RemoteRepository.class);
        when(context.session.getRepository(resolverRepository)).thenReturn(Optional.of(repository));
        Exception failure = new Exception("resolution failed");
        Path path = Path.of("target", "demo.jar");

        context.bridge.artifactResolved(new org.eclipse.aether.RepositoryEvent.Builder(
                        context.resolverSession, org.eclipse.aether.RepositoryEvent.EventType.ARTIFACT_RESOLVED)
                .setArtifact(resolverArtifact)
                .setRepository(resolverRepository)
                .setPath(path)
                .setException(failure)
                .setTrace(new org.eclipse.aether.RequestTrace("request"))
                .build());

        RepositoryEvent event = listener.events.get(0);
        assertEquals(RepositoryEventType.ARTIFACT_RESOLVED, event.getType());
        assertSame(context.session, event.getSession());
        assertSame(artifact, event.getArtifact().orElseThrow());
        assertSame(repository, event.getRepository().orElseThrow());
        assertEquals(path, event.getPath().orElseThrow());
        assertSame(failure, event.getException().orElseThrow());
        assertEquals(List.of(failure), event.getExceptions());
        assertEquals("request", event.getTrace().orElseThrow().data());
        assertTrue(event.getMetadata().isEmpty());
        assertThrows(
                UnsupportedOperationException.class, () -> event.getExceptions().add(new Exception()));
    }

    @Test
    void convertsMetadataWithoutExposingResolverMetadata() {
        TestContext context = new TestContext();
        RecordingListener listener = new RecordingListener();
        when(context.session.getRepositoryListeners()).thenReturn(List.of(listener));
        Path path = Path.of("target", "maven-metadata.xml");
        Metadata metadata = new DefaultMetadata(
                "org.example",
                "demo",
                "1.0-SNAPSHOT",
                "maven-metadata.xml",
                Metadata.Nature.SNAPSHOT,
                Map.of("source", "test"),
                path);

        context.bridge.metadataResolved(new org.eclipse.aether.RepositoryEvent.Builder(
                        context.resolverSession, org.eclipse.aether.RepositoryEvent.EventType.METADATA_RESOLVED)
                .setMetadata(metadata)
                .build());

        RepositoryMetadata converted = listener.events.get(0).getMetadata().orElseThrow();
        assertEquals("org.example", converted.getGroupId());
        assertEquals("demo", converted.getArtifactId());
        assertEquals("1.0-SNAPSHOT", converted.getVersion());
        assertEquals("maven-metadata.xml", converted.getType());
        assertEquals(RepositoryMetadata.Nature.SNAPSHOT, converted.getNature());
        assertEquals(path, converted.getPath().orElseThrow());
        assertEquals(Map.of("source", "test"), converted.getProperties());
        assertThrows(
                UnsupportedOperationException.class,
                () -> converted.getProperties().put("key", "value"));
    }

    @Test
    void isolatesListenerFailures() {
        TestContext context = new TestContext();
        RepositoryListener failing = new RepositoryListener() {
            @Override
            public void artifactResolving(RepositoryEvent event) {
                throw new IllegalStateException("listener failure");
            }
        };
        AtomicInteger notifications = new AtomicInteger();
        RepositoryListener succeeding = new RepositoryListener() {
            @Override
            public void artifactResolving(RepositoryEvent event) {
                notifications.incrementAndGet();
            }
        };
        when(context.session.getRepositoryListeners()).thenReturn(List.of(failing, succeeding));

        context.bridge.artifactResolving(new org.eclipse.aether.RepositoryEvent.Builder(
                        context.resolverSession, org.eclipse.aether.RepositoryEvent.EventType.ARTIFACT_RESOLVING)
                .build());

        assertEquals(1, notifications.get());
    }

    @Test
    void supportsConcurrentDispatch() {
        TestContext context = new TestContext();
        AtomicInteger notifications = new AtomicInteger();
        RepositoryListener listener = new RepositoryListener() {
            @Override
            public void artifactResolving(RepositoryEvent event) {
                notifications.incrementAndGet();
            }
        };
        when(context.session.getRepositoryListeners()).thenReturn(List.of(listener));

        IntStream.range(0, 100)
                .parallel()
                .forEach(i -> context.bridge.artifactResolving(new org.eclipse.aether.RepositoryEvent.Builder(
                                context.resolverSession,
                                org.eclipse.aether.RepositoryEvent.EventType.ARTIFACT_RESOLVING)
                        .build()));

        assertEquals(100, notifications.get());
    }

    private static void dispatch(MavenRepositoryListener listener, org.eclipse.aether.RepositoryEvent event) {
        switch (event.getType()) {
            case ARTIFACT_DESCRIPTOR_INVALID -> listener.artifactDescriptorInvalid(event);
            case ARTIFACT_DESCRIPTOR_MISSING -> listener.artifactDescriptorMissing(event);
            case METADATA_INVALID -> listener.metadataInvalid(event);
            case ARTIFACT_RESOLVING -> listener.artifactResolving(event);
            case ARTIFACT_RESOLVED -> listener.artifactResolved(event);
            case METADATA_RESOLVING -> listener.metadataResolving(event);
            case METADATA_RESOLVED -> listener.metadataResolved(event);
            case ARTIFACT_DOWNLOADING -> listener.artifactDownloading(event);
            case ARTIFACT_DOWNLOADED -> listener.artifactDownloaded(event);
            case METADATA_DOWNLOADING -> listener.metadataDownloading(event);
            case METADATA_DOWNLOADED -> listener.metadataDownloaded(event);
            case ARTIFACT_INSTALLING -> listener.artifactInstalling(event);
            case ARTIFACT_INSTALLED -> listener.artifactInstalled(event);
            case METADATA_INSTALLING -> listener.metadataInstalling(event);
            case METADATA_INSTALLED -> listener.metadataInstalled(event);
            case ARTIFACT_DEPLOYING -> listener.artifactDeploying(event);
            case ARTIFACT_DEPLOYED -> listener.artifactDeployed(event);
            case METADATA_DEPLOYING -> listener.metadataDeploying(event);
            case METADATA_DEPLOYED -> listener.metadataDeployed(event);
            default -> throw new IllegalArgumentException("Unknown repository event type: " + event.getType());
        }
    }

    private static final class TestContext {
        private final DefaultRepositorySystemSession resolverSession = new DefaultRepositorySystemSession(h -> false);
        private final InternalSession session = mock(InternalSession.class);
        private final MavenRepositoryListener bridge = new MavenRepositoryListener();

        private TestContext() {
            InternalSession.associate(resolverSession, session);
        }
    }

    private static final class RecordingListener implements RepositoryListener {
        private final List<RepositoryEventType> types = new ArrayList<>();
        private final List<RepositoryEvent> events = new ArrayList<>();

        private void record(RepositoryEvent event) {
            types.add(event.getType());
            events.add(event);
        }

        @Override
        public void artifactDescriptorInvalid(RepositoryEvent event) {
            record(event);
        }

        @Override
        public void artifactDescriptorMissing(RepositoryEvent event) {
            record(event);
        }

        @Override
        public void metadataInvalid(RepositoryEvent event) {
            record(event);
        }

        @Override
        public void artifactResolving(RepositoryEvent event) {
            record(event);
        }

        @Override
        public void artifactResolved(RepositoryEvent event) {
            record(event);
        }

        @Override
        public void metadataResolving(RepositoryEvent event) {
            record(event);
        }

        @Override
        public void metadataResolved(RepositoryEvent event) {
            record(event);
        }

        @Override
        public void artifactDownloading(RepositoryEvent event) {
            record(event);
        }

        @Override
        public void artifactDownloaded(RepositoryEvent event) {
            record(event);
        }

        @Override
        public void metadataDownloading(RepositoryEvent event) {
            record(event);
        }

        @Override
        public void metadataDownloaded(RepositoryEvent event) {
            record(event);
        }

        @Override
        public void artifactInstalling(RepositoryEvent event) {
            record(event);
        }

        @Override
        public void artifactInstalled(RepositoryEvent event) {
            record(event);
        }

        @Override
        public void metadataInstalling(RepositoryEvent event) {
            record(event);
        }

        @Override
        public void metadataInstalled(RepositoryEvent event) {
            record(event);
        }

        @Override
        public void artifactDeploying(RepositoryEvent event) {
            record(event);
        }

        @Override
        public void artifactDeployed(RepositoryEvent event) {
            record(event);
        }

        @Override
        public void metadataDeploying(RepositoryEvent event) {
            record(event);
        }

        @Override
        public void metadataDeployed(RepositoryEvent event) {
            record(event);
        }
    }
}
