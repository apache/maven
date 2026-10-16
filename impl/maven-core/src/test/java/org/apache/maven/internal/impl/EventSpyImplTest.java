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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.maven.api.DependencyResolutionEvent;
import org.apache.maven.api.DependencyResolutionEventType;
import org.apache.maven.api.DependencyResolutionListener;
import org.apache.maven.api.Event;
import org.apache.maven.api.EventType;
import org.apache.maven.api.ExecutionEvent;
import org.apache.maven.api.ExecutionEventType;
import org.apache.maven.api.ExecutionListener;
import org.apache.maven.api.Listener;
import org.apache.maven.api.RepositoryEvent;
import org.apache.maven.api.RepositoryListener;
import org.apache.maven.api.Session;
import org.apache.maven.api.services.Lookup;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.impl.InternalSession;
import org.apache.maven.impl.MavenRepositoryListener;
import org.apache.maven.project.DependencyResolutionRequest;
import org.apache.maven.project.DependencyResolutionResult;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EventSpyImplTest {
    @Test
    void dispatchesAllExecutionCallbacksAndPreservesLegacyEvents() throws Exception {
        TestContext context = new TestContext();
        RecordingListener typed = new RecordingListener();
        List<Event> legacy = new ArrayList<>();
        context.session.registerListener(typed);
        context.session.registerListener(legacy::add);

        for (org.apache.maven.execution.ExecutionEvent.Type type :
                org.apache.maven.execution.ExecutionEvent.Type.values()) {
            context.execution(type);
        }

        assertEquals(List.of(ExecutionEventType.values()), typed.executions);
        assertEquals(
                List.of(EventType.values()), legacy.stream().map(Event::getType).toList());
        for (Event event : legacy) {
            ExecutionEvent typedEvent = (ExecutionEvent) event;
            assertSame(context.session, event.getSession());
            assertSame(event.getSession(), typedEvent.session());
            assertEquals(event.getProject(), typedEvent.project());
            assertEquals(event.getMojoExecution(), typedEvent.mojoExecution());
            assertEquals(event.getException(), typedEvent.exception());
        }
    }

    @Test
    void sharesOneRegistrationForBothEventFamiliesAcrossDerivedSessions() throws Exception {
        TestContext context = new TestContext();
        RecordingListener combined = new RecordingListener();
        AtomicInteger legacyCalls = new AtomicInteger();
        Listener legacy = event -> legacyCalls.incrementAndGet();
        AtomicInteger repositoryOnlyCalls = new AtomicInteger();
        RepositoryListener repositoryOnly = new RepositoryListener() {
            @Override
            public void artifactResolving(RepositoryEvent event) {
                repositoryOnlyCalls.incrementAndGet();
            }
        };
        context.session.registerListener(combined);
        context.session.registerListener(legacy);
        context.session.registerListener(repositoryOnly);
        Session derived = context.session.withRemoteRepositories(List.of());
        assertIterableEquals(context.session.getListeners(), derived.getListeners());
        assertThrows(
                UnsupportedOperationException.class,
                () -> derived.getListeners().clear());

        context.execution(org.apache.maven.execution.ExecutionEvent.Type.SessionStarted);
        context.repository();
        assertEquals(List.of(ExecutionEventType.SESSION_STARTED), combined.executions);
        assertEquals(1, combined.repositoryCalls);
        assertEquals(1, repositoryOnlyCalls.get());
        assertEquals(1, legacyCalls.get());

        derived.unregisterListener(combined);
        derived.unregisterListener(legacy);
        derived.unregisterListener(repositoryOnly);
        context.execution(org.apache.maven.execution.ExecutionEvent.Type.SessionStarted);
        context.repository();
        assertTrue(context.session.getListeners().isEmpty());
        assertEquals(1, combined.repositoryCalls);
        assertEquals(1, legacyCalls.get());
    }

    @Test
    void isolatesExecutionListenerFailures() throws Exception {
        TestContext context = new TestContext();
        context.session.registerListener(new ExecutionListener() {
            @Override
            public void sessionStarted(ExecutionEvent event) {
                throw new IllegalStateException("listener failure");
            }
        });
        AtomicInteger calls = new AtomicInteger();
        context.session.registerListener(event -> calls.incrementAndGet());
        context.execution(org.apache.maven.execution.ExecutionEvent.Type.SessionStarted);
        assertEquals(1, calls.get());
    }

    @Test
    void forwardsSuccessfulDependencyResolution() throws Exception {
        TestContext context = new TestContext(lookup());
        RecordingResolutionListener listener = new RecordingResolutionListener();
        context.session.registerListener(listener);
        Dependency dependency = new Dependency(new DefaultArtifact("g:a:1"), "compile");
        DependencyResolutionResult result = result(List.of(dependency), List.of(), List.of());

        EventSpyImpl spy = new EventSpyImpl();
        DependencyResolutionRequest request = context.resolution();
        spy.onEvent(request);
        spy.onEvent(result);

        assertEquals(
                List.of(DependencyResolutionEventType.STARTED, DependencyResolutionEventType.SUCCEEDED),
                listener.types);
        DependencyResolutionEvent event = listener.events.get(1);
        assertSame(context.session, event.session());
        assertNotNull(event.project());
        assertEquals(1, event.resolvedDependencies().size());
        assertEquals("a", event.resolvedDependencies().get(0).getArtifactId());
        assertTrue(event.unresolvedDependencies().isEmpty());
        assertTrue(event.exception().isEmpty());
        assertTrue(listener.events.get(0).resolvedDependencies().isEmpty());
    }

    @Test
    void forwardsFailedDependencyResolution() throws Exception {
        TestContext context = new TestContext(lookup());
        RecordingResolutionListener listener = new RecordingResolutionListener();
        context.session.registerListener(listener);
        Dependency dependency = new Dependency(new DefaultArtifact("g:a:1"), "compile");
        Exception failure = new Exception("not found");
        DependencyResolutionResult result = result(List.of(), List.of(dependency), List.of());
        when(result.getResolutionErrors(dependency)).thenReturn(List.of(failure));

        EventSpyImpl spy = new EventSpyImpl();
        spy.onEvent(context.resolution());
        spy.onEvent(result);

        assertEquals(
                List.of(DependencyResolutionEventType.STARTED, DependencyResolutionEventType.FAILED), listener.types);
        DependencyResolutionEvent event = listener.events.get(1);
        assertEquals(1, event.unresolvedDependencies().size());
        assertSame(failure, event.exception().orElseThrow());
    }

    @Test
    void ignoresDependencyResolutionResultWithoutRequest() throws Exception {
        TestContext context = new TestContext();
        RecordingResolutionListener listener = new RecordingResolutionListener();
        context.session.registerListener(listener);
        new EventSpyImpl().onEvent(result(List.of(), List.of(), List.of()));
        assertTrue(listener.types.isEmpty());
    }

    /**
     * Pins the pairing only: the requests are strongly referenced by the test, so this does not exercise
     * the weak reference that keeps an abandoned request from being retained by an idle thread.
     */
    @Test
    void matchesResultWithTheLatestRequestWhenAnEarlierOneGotNoResult() throws Exception {
        TestContext context = new TestContext(lookup());
        RecordingResolutionListener listener = new RecordingResolutionListener();
        context.session.registerListener(listener);

        EventSpyImpl spy = new EventSpyImpl();
        DependencyResolutionRequest abandoned = context.resolution("abandoned");
        DependencyResolutionRequest current = context.resolution("current");
        spy.onEvent(abandoned);
        spy.onEvent(current);
        spy.onEvent(result(List.of(), List.of(), List.of()));

        assertEquals(3, listener.events.size());
        assertEquals("current", listener.events.get(2).project().getArtifactId());
    }

    @Test
    void ignoresDependencyResolutionOfProjectWithoutBaseDirectory() throws Exception {
        TestContext context = new TestContext(lookup());
        RecordingResolutionListener listener = new RecordingResolutionListener();
        context.session.registerListener(listener);
        DependencyResolutionRequest request = context.resolution();
        when(request.getMavenProject()).thenReturn(mock(MavenProject.class));

        EventSpyImpl spy = new EventSpyImpl();
        spy.onEvent(request);
        spy.onEvent(result(List.of(), List.of(), List.of()));

        assertTrue(listener.types.isEmpty());
    }

    @Test
    void rejectsNullRegistrationAndRemoval() {
        TestContext context = new TestContext();
        assertThrows(NullPointerException.class, () -> context.session.registerListener(null));
        assertThrows(NullPointerException.class, () -> context.session.unregisterListener(null));
    }

    @Test
    void supportsCombinedListenerWithoutLegacyOverride() throws Exception {
        TestContext context = new TestContext();
        class CombinedListener implements ExecutionListener, RepositoryListener {
            int executionCalls;
            int repositoryCalls;

            @Override
            public void onEvent(Event event) {
                ExecutionListener.super.onEvent(event);
                RepositoryListener.super.onEvent(event);
            }

            @Override
            public void sessionStarted(ExecutionEvent event) {
                executionCalls++;
            }

            @Override
            public void artifactResolving(RepositoryEvent event) {
                repositoryCalls++;
            }
        }
        CombinedListener listener = new CombinedListener();
        context.session.registerListener(listener);
        context.execution(org.apache.maven.execution.ExecutionEvent.Type.SessionStarted);
        context.repository();
        assertEquals(1, listener.executionCalls);
        assertEquals(1, listener.repositoryCalls);
    }

    @Test
    void supportsConcurrentRegistrationAndRemovalThroughDerivedSession() {
        TestContext context = new TestContext();
        Session derived = context.session.withRemoteRepositories(List.of());
        List<Listener> listeners = java.util.stream.IntStream.range(0, 100)
                .mapToObj(i -> (Listener) new ExecutionListener() {})
                .toList();
        listeners.parallelStream().forEach(context.session::registerListener);
        assertEquals(100, derived.getListeners().size());
        listeners.parallelStream().forEach(derived::unregisterListener);
        assertTrue(context.session.getListeners().isEmpty());
    }

    @Test
    void capturesExecutionDetailsWhenEventIsCreated() {
        TestContext context = new TestContext();
        org.apache.maven.execution.ExecutionEvent source = mock(org.apache.maven.execution.ExecutionEvent.class);
        Exception failure = new Exception("original failure");
        when(source.getException()).thenReturn(failure);
        ExecutionEvent event = new DefaultEvent(context.session, source, EventType.PROJECT_FAILED);
        when(source.getException()).thenReturn(new Exception("changed failure"));
        assertSame(failure, event.exception().orElseThrow());
        assertSame(failure, event.getException().orElseThrow());
    }

    /** A lookup which answers every service with a stub, enough to convert dependencies. */
    private static Lookup lookup() {
        Lookup lookup = mock(Lookup.class);
        when(lookup.lookup(any(Class.class)))
                .thenAnswer(invocation -> mock(invocation.<Class<?>>getArgument(0), RETURNS_DEEP_STUBS));
        return lookup;
    }

    private static DependencyResolutionResult result(
            List<Dependency> resolved, List<Dependency> unresolved, List<Exception> collectionErrors) {
        DependencyResolutionResult result = mock(DependencyResolutionResult.class);
        when(result.getResolvedDependencies()).thenReturn(resolved);
        when(result.getUnresolvedDependencies()).thenReturn(unresolved);
        when(result.getCollectionErrors()).thenReturn(collectionErrors);
        return result;
    }

    private static class TestContext {
        final DefaultRepositorySystemSession resolver = new DefaultRepositorySystemSession(h -> false);
        final MavenSession maven = new MavenSession(null, resolver, new DefaultMavenExecutionRequest(), null);
        final DefaultSession session;

        TestContext() {
            this(null);
        }

        TestContext(Lookup lookup) {
            session = new DefaultSession(maven, mock(RepositorySystem.class), List.of(), null, lookup, null);
            maven.setSession(session);
            resolver.getData().set(InternalSession.class, session);
        }

        void execution(org.apache.maven.execution.ExecutionEvent.Type type) throws Exception {
            org.apache.maven.execution.ExecutionEvent event = mock(org.apache.maven.execution.ExecutionEvent.class);
            when(event.getSession()).thenReturn(maven);
            when(event.getType()).thenReturn(type);
            new EventSpyImpl().onEvent(event);
        }

        DependencyResolutionRequest resolution() {
            return resolution("a");
        }

        DependencyResolutionRequest resolution(String artifactId) {
            DependencyResolutionRequest request = mock(DependencyResolutionRequest.class);
            when(request.getRepositorySession()).thenReturn(resolver);
            MavenProject project = new MavenProject();
            project.setArtifactId(artifactId);
            project.setFile(new File("target/pom.xml").getAbsoluteFile());
            when(request.getMavenProject()).thenReturn(project);
            return request;
        }

        void repository() {
            new MavenRepositoryListener()
                    .artifactResolving(new org.eclipse.aether.RepositoryEvent.Builder(
                                    resolver, org.eclipse.aether.RepositoryEvent.EventType.ARTIFACT_RESOLVING)
                            .build());
        }
    }

    private static class RecordingResolutionListener implements DependencyResolutionListener {
        final List<DependencyResolutionEventType> types = new ArrayList<>();
        final List<DependencyResolutionEvent> events = new ArrayList<>();

        @Override
        public void dependencyResolutionStarted(DependencyResolutionEvent event) {
            record(event);
        }

        @Override
        public void dependencyResolutionSucceeded(DependencyResolutionEvent event) {
            record(event);
        }

        @Override
        public void dependencyResolutionFailed(DependencyResolutionEvent event) {
            record(event);
        }

        private void record(DependencyResolutionEvent event) {
            types.add(event.type());
            events.add(event);
        }
    }

    private static class RecordingListener implements ExecutionListener, RepositoryListener {
        final List<ExecutionEventType> executions = new ArrayList<>();
        int repositoryCalls;

        @Override
        public void onEvent(Event event) {
            ExecutionListener.super.onEvent(event);
            RepositoryListener.super.onEvent(event);
        }

        @Override
        public void artifactResolving(RepositoryEvent event) {
            repositoryCalls++;
        }

        @Override
        public void projectDiscoveryStarted(ExecutionEvent event) {
            assertEquals(ExecutionEventType.PROJECT_DISCOVERY_STARTED, event.type());
            executions.add(ExecutionEventType.PROJECT_DISCOVERY_STARTED);
        }

        @Override
        public void sessionStarted(ExecutionEvent event) {
            assertEquals(ExecutionEventType.SESSION_STARTED, event.type());
            executions.add(ExecutionEventType.SESSION_STARTED);
        }

        @Override
        public void sessionEnded(ExecutionEvent event) {
            assertEquals(ExecutionEventType.SESSION_ENDED, event.type());
            executions.add(ExecutionEventType.SESSION_ENDED);
        }

        @Override
        public void projectSkipped(ExecutionEvent event) {
            assertEquals(ExecutionEventType.PROJECT_SKIPPED, event.type());
            executions.add(ExecutionEventType.PROJECT_SKIPPED);
        }

        @Override
        public void projectStarted(ExecutionEvent event) {
            assertEquals(ExecutionEventType.PROJECT_STARTED, event.type());
            executions.add(ExecutionEventType.PROJECT_STARTED);
        }

        @Override
        public void projectSucceeded(ExecutionEvent event) {
            assertEquals(ExecutionEventType.PROJECT_SUCCEEDED, event.type());
            executions.add(ExecutionEventType.PROJECT_SUCCEEDED);
        }

        @Override
        public void projectFailed(ExecutionEvent event) {
            assertEquals(ExecutionEventType.PROJECT_FAILED, event.type());
            executions.add(ExecutionEventType.PROJECT_FAILED);
        }

        @Override
        public void mojoSkipped(ExecutionEvent event) {
            assertEquals(ExecutionEventType.MOJO_SKIPPED, event.type());
            executions.add(ExecutionEventType.MOJO_SKIPPED);
        }

        @Override
        public void mojoStarted(ExecutionEvent event) {
            assertEquals(ExecutionEventType.MOJO_STARTED, event.type());
            executions.add(ExecutionEventType.MOJO_STARTED);
        }

        @Override
        public void mojoSucceeded(ExecutionEvent event) {
            assertEquals(ExecutionEventType.MOJO_SUCCEEDED, event.type());
            executions.add(ExecutionEventType.MOJO_SUCCEEDED);
        }

        @Override
        public void mojoFailed(ExecutionEvent event) {
            assertEquals(ExecutionEventType.MOJO_FAILED, event.type());
            executions.add(ExecutionEventType.MOJO_FAILED);
        }

        @Override
        public void forkStarted(ExecutionEvent event) {
            assertEquals(ExecutionEventType.FORK_STARTED, event.type());
            executions.add(ExecutionEventType.FORK_STARTED);
        }

        @Override
        public void forkSucceeded(ExecutionEvent event) {
            assertEquals(ExecutionEventType.FORK_SUCCEEDED, event.type());
            executions.add(ExecutionEventType.FORK_SUCCEEDED);
        }

        @Override
        public void forkFailed(ExecutionEvent event) {
            assertEquals(ExecutionEventType.FORK_FAILED, event.type());
            executions.add(ExecutionEventType.FORK_FAILED);
        }

        @Override
        public void forkedProjectStarted(ExecutionEvent event) {
            assertEquals(ExecutionEventType.FORKED_PROJECT_STARTED, event.type());
            executions.add(ExecutionEventType.FORKED_PROJECT_STARTED);
        }

        @Override
        public void forkedProjectSucceeded(ExecutionEvent event) {
            assertEquals(ExecutionEventType.FORKED_PROJECT_SUCCEEDED, event.type());
            executions.add(ExecutionEventType.FORKED_PROJECT_SUCCEEDED);
        }

        @Override
        public void forkedProjectFailed(ExecutionEvent event) {
            assertEquals(ExecutionEventType.FORKED_PROJECT_FAILED, event.type());
            executions.add(ExecutionEventType.FORKED_PROJECT_FAILED);
        }
    }
}
