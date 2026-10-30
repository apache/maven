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
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import org.apache.maven.api.Artifact;
import org.apache.maven.api.Event;
import org.apache.maven.api.ExecutionEvent;
import org.apache.maven.api.ExecutionEventType;
import org.apache.maven.api.ExecutionListener;
import org.apache.maven.api.Listener;
import org.apache.maven.api.Project;
import org.apache.maven.api.RemoteRepository;
import org.apache.maven.api.RepositoryEvent;
import org.apache.maven.api.RepositoryEventType;
import org.apache.maven.api.RepositoryListener;
import org.apache.maven.api.RepositoryMetadata;
import org.apache.maven.api.Session;
import org.apache.maven.api.Version;
import org.apache.maven.api.services.Lookup;
import org.apache.maven.api.services.RequestTrace;
import org.apache.maven.api.settings.Settings;
import org.apache.maven.api.toolchain.ToolchainModel;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.metadata.DefaultMetadata;
import org.eclipse.aether.metadata.Metadata;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MavenRepositoryListenerTest {

    @Test
    void unifiedRegistrationPreservesBuildListenerLambdas() {
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
        when(context.session.getListeners()).thenReturn(List.of(listener));

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
        when(context.session.getListeners()).thenReturn(List.of(listener));

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
        assertEquals(RepositoryEventType.ARTIFACT_RESOLVED, event.type());
        assertSame(context.session, event.session());
        assertSame(artifact, event.artifact().orElseThrow());
        assertSame(repository, event.repository().orElseThrow());
        assertEquals(path, event.path().orElseThrow());
        assertSame(failure, event.exception().orElseThrow());
        assertEquals(List.of(failure), event.exceptions());
        assertEquals("request", event.trace().orElseThrow().data());
        assertTrue(event.metadata().isEmpty());
        assertThrows(
                UnsupportedOperationException.class, () -> event.exceptions().add(new Exception()));
    }

    @Test
    void convertsMetadataWithoutExposingResolverMetadata() {
        TestContext context = new TestContext();
        RecordingListener listener = new RecordingListener();
        when(context.session.getListeners()).thenReturn(List.of(listener));
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

        RepositoryMetadata converted = listener.events.get(0).metadata().orElseThrow();
        assertEquals("org.example", converted.groupId());
        assertEquals("demo", converted.artifactId());
        assertEquals("1.0-SNAPSHOT", converted.version());
        assertEquals("maven-metadata.xml", converted.type());
        assertEquals(RepositoryMetadata.Nature.SNAPSHOT, converted.nature());
        assertEquals(path, converted.path().orElseThrow());
        assertEquals(Map.of("source", "test"), converted.properties());
        assertThrows(
                UnsupportedOperationException.class,
                () -> converted.properties().put("key", "value"));
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
        when(context.session.getListeners()).thenReturn(List.of(failing, succeeding));

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
        when(context.session.getListeners()).thenReturn(List.of(listener));

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

    @Test
    void spiListenerDiscoveredFromLookupReceivesEventsWithoutManualRegistration() {
        Lookup lookup = mock(Lookup.class);
        RecordingListener repoListener = new RecordingListener();
        RecordingExecutionListener execListener = new RecordingExecutionListener();
        RecordingCombinedListener combinedListener = new RecordingCombinedListener();
        when(lookup.lookupList(Listener.class)).thenReturn(List.of(repoListener, execListener, combinedListener));

        DefaultRepositorySystemSession resolverSession = new DefaultRepositorySystemSession(h -> false);
        TestSession session = new TestSession(resolverSession, lookup);
        InternalSession.associate(resolverSession, session);

        assertEquals(3, session.getListeners().size());
        assertTrue(session.getListeners().contains(repoListener));
        assertTrue(session.getListeners().contains(execListener));
        assertTrue(session.getListeners().contains(combinedListener));

        MavenRepositoryListener bridge = new MavenRepositoryListener();
        bridge.artifactResolving(new org.eclipse.aether.RepositoryEvent.Builder(
                        resolverSession, org.eclipse.aether.RepositoryEvent.EventType.ARTIFACT_RESOLVING)
                .build());

        assertEquals(List.of(RepositoryEventType.ARTIFACT_RESOLVING), repoListener.types);
        assertEquals(List.of(RepositoryEventType.ARTIFACT_RESOLVING), combinedListener.repositoryTypes);
        assertTrue(execListener.types.isEmpty());

        ExecutionEvent executionEvent = mock(ExecutionEvent.class);
        when(executionEvent.type()).thenReturn(ExecutionEventType.SESSION_STARTED);
        for (Listener listener : session.getListeners()) {
            listener.onEvent(executionEvent);
        }

        assertEquals(List.of(ExecutionEventType.SESSION_STARTED), execListener.types);
        assertEquals(List.of(ExecutionEventType.SESSION_STARTED), combinedListener.executionTypes);
        assertEquals(List.of(RepositoryEventType.ARTIFACT_RESOLVING), repoListener.types);
    }

    @Test
    void manuallyRegisteredListenersContinueToWorkWithSpiListeners() {
        Lookup lookup = mock(Lookup.class);
        RecordingListener spiListener = new RecordingListener();
        when(lookup.lookupList(Listener.class)).thenReturn(List.of(spiListener));

        DefaultRepositorySystemSession resolverSession = new DefaultRepositorySystemSession(h -> false);
        TestSession session = new TestSession(resolverSession, lookup);
        InternalSession.associate(resolverSession, session);

        RecordingListener manual = new RecordingListener();
        session.registerListener(manual);

        assertEquals(2, session.getListeners().size());

        MavenRepositoryListener bridge = new MavenRepositoryListener();
        bridge.artifactResolved(new org.eclipse.aether.RepositoryEvent.Builder(
                        resolverSession, org.eclipse.aether.RepositoryEvent.EventType.ARTIFACT_RESOLVED)
                .build());

        assertEquals(List.of(RepositoryEventType.ARTIFACT_RESOLVED), spiListener.types);
        assertEquals(List.of(RepositoryEventType.ARTIFACT_RESOLVED), manual.types);

        session.unregisterListener(manual);
        assertEquals(1, session.getListeners().size());
        assertTrue(session.getListeners().contains(spiListener));
    }

    @Test
    void nullOrEmptyLookupDoesNotBreakSessionInitialization() {
        DefaultRepositorySystemSession resolverSessionNull = new DefaultRepositorySystemSession(h -> false);
        TestSession sessionNull = new TestSession(resolverSessionNull, null);
        assertTrue(sessionNull.getListeners().isEmpty());

        DefaultRepositorySystemSession resolverSessionEmpty = new DefaultRepositorySystemSession(h -> false);
        Lookup emptyLookup = mock(Lookup.class);
        when(emptyLookup.lookupList(Listener.class)).thenReturn(List.of());
        TestSession sessionEmpty = new TestSession(resolverSessionEmpty, emptyLookup);
        assertTrue(sessionEmpty.getListeners().isEmpty());
    }

    @Test
    void derivedSessionsDoNotCauseDuplicateListenerNotifications() {
        Lookup lookup = mock(Lookup.class);
        RecordingListener spiListener = new RecordingListener();
        when(lookup.lookupList(Listener.class)).thenReturn(List.of(spiListener));

        DefaultRepositorySystemSession resolverSession = new DefaultRepositorySystemSession(h -> false);
        TestSession rootSession = new TestSession(resolverSession, lookup);
        Session derivedSession = rootSession.withRemoteRepositories(List.of());
        InternalSession.associate(resolverSession, rootSession);

        assertEquals(1, derivedSession.getListeners().size());
        verify(lookup, times(1)).lookupList(Listener.class);

        MavenRepositoryListener bridge = new MavenRepositoryListener();
        bridge.metadataInstalling(new org.eclipse.aether.RepositoryEvent.Builder(
                        resolverSession, org.eclipse.aether.RepositoryEvent.EventType.METADATA_INSTALLING)
                .build());

        assertEquals(1, spiListener.types.size());
        assertEquals(RepositoryEventType.METADATA_INSTALLING, spiListener.types.get(0));
    }

    private static class RecordingExecutionListener implements ExecutionListener {
        final List<ExecutionEventType> types = new ArrayList<>();
        final List<ExecutionEvent> events = new ArrayList<>();

        @Override
        public void sessionStarted(ExecutionEvent event) {
            types.add(event.type());
            events.add(event);
        }
    }

    private static class RecordingCombinedListener implements ExecutionListener, RepositoryListener {
        final List<RepositoryEventType> repositoryTypes = new ArrayList<>();
        final List<RepositoryEvent> repositoryEvents = new ArrayList<>();
        final List<ExecutionEventType> executionTypes = new ArrayList<>();
        final List<ExecutionEvent> executionEvents = new ArrayList<>();

        @Override
        public void onEvent(Event event) {
            ExecutionListener.super.onEvent(event);
            RepositoryListener.super.onEvent(event);
        }

        @Override
        public void sessionStarted(ExecutionEvent event) {
            executionTypes.add(event.type());
            executionEvents.add(event);
        }

        @Override
        public void artifactResolving(RepositoryEvent event) {
            repositoryTypes.add(event.type());
            repositoryEvents.add(event);
        }
    }

    private static class TestSession extends AbstractSession {
        TestSession(RepositorySystemSession session, Lookup lookup) {
            super(session, mock(RepositorySystem.class), List.of(), List.of(), lookup, null);
        }

        @Override
        public Settings getSettings() {
            return null;
        }

        @Override
        public Collection<ToolchainModel> getToolchains() {
            return List.of();
        }

        @Override
        public Map<String, String> getUserProperties() {
            return Map.of();
        }

        @Override
        public Map<String, String> getSystemProperties() {
            return Map.of();
        }

        @Override
        public Map<String, String> getEffectiveProperties(Project project) {
            return Map.of();
        }

        @Override
        public Version getMavenVersion() {
            return null;
        }

        @Override
        public Instant getStartTime() {
            return null;
        }

        @Override
        public Path getTopDirectory() {
            return null;
        }

        @Override
        public Path getRootDirectory() {
            return null;
        }

        @Override
        public List<Project> getProjects() {
            return List.of();
        }

        @Override
        public Map<String, Object> getPluginContext(Project project) {
            return Map.of();
        }

        @Override
        protected Session newSession(
                RepositorySystemSession session, List<RemoteRepository> repositories, RequestTrace context) {
            return new TestSession(session, lookup);
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

    private static class RecordingListener implements RepositoryListener {
        final List<RepositoryEventType> types = new ArrayList<>();
        final List<RepositoryEvent> events = new ArrayList<>();

        private void record(RepositoryEvent event) {
            types.add(event.type());
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
