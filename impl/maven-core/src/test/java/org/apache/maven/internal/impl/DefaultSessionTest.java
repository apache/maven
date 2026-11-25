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

import java.nio.file.Paths;
import java.util.Collections;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.maven.api.Packaging;
import org.apache.maven.api.Project;
import org.apache.maven.api.Session;
import org.apache.maven.api.services.RequestTrace;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.impl.InternalSession;
import org.apache.maven.impl.RequestTraceHelper;
import org.apache.maven.model.root.RootLocator;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class DefaultSessionTest {

    @Test
    void testContextPropagationAndNesting() {
        DefaultSession session = newSession();
        RequestTrace context = new RequestTrace("context");
        InternalSession contextualSession = InternalSession.from(session.withContext(context));

        assertNull(session.getCurrentTrace());
        assertSame(context, contextualSession.getCurrentTrace());

        RequestTraceHelper.ResolverTrace nested = RequestTraceHelper.enter(contextualSession, "nested");
        assertSame(context, nested.mvnTrace().parent());
        assertEquals("context", nested.context());
        assertSame(nested.mvnTrace(), contextualSession.getCurrentTrace());

        RequestTraceHelper.exit(nested);
        assertSame(context, contextualSession.getCurrentTrace());
        assertNull(session.getCurrentTrace());
    }

    @Test
    void testContextValidation() {
        DefaultSession session = newSession();

        assertThrows(NullPointerException.class, () -> session.withContext(null));
        assertThrows(NullPointerException.class, () -> session.withContext(new RequestTrace(null, null, null)));
    }

    @Test
    void testContextIsIsolatedAcrossThreads() throws Exception {
        RequestTrace context = new RequestTrace("context");
        InternalSession session = InternalSession.from(newSession().withContext(context));
        CyclicBarrier barrier = new CyclicBarrier(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<RequestTrace> first = executor.submit(() -> enterAndReadTrace(session, barrier, "first"));
            Future<RequestTrace> second = executor.submit(() -> enterAndReadTrace(session, barrier, "second"));

            RequestTrace firstTrace = first.get();
            RequestTrace secondTrace = second.get();
            assertEquals("first", firstTrace.data());
            assertEquals("second", secondTrace.data());
            assertSame(context, firstTrace.parent());
            assertSame(context, secondTrace.parent());
            assertSame(context, session.getCurrentTrace());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void testActiveTraceIsVisibleAcrossSessionsSharingResolverSession() {
        DefaultSession parent = newSession();
        InternalSession derived = InternalSession.from(parent.withContext(new RequestTrace("context")));
        RequestTraceHelper.ResolverTrace nested = RequestTraceHelper.enter(derived, "nested");

        try {
            assertSame(nested.mvnTrace(), parent.getCurrentTrace());
            assertSame(
                    nested.mvnTrace(), InternalSession.from(parent.getSession()).getCurrentTrace());
        } finally {
            RequestTraceHelper.exit(nested);
        }

        assertNull(parent.getCurrentTrace());
        assertEquals("context", derived.getCurrentTrace().context());
    }

    @Test
    void testRepositoryDerivationPreservesContext() {
        RequestTrace context = new RequestTrace("context");
        Session derived = newSession().withContext(context).withRemoteRepositories(Collections.emptyList());

        assertSame(context, InternalSession.from(derived).getCurrentTrace());
    }

    @Test
    void testRootDirectoryWithNull() {
        RepositorySystemSession rss = new DefaultRepositorySystemSession(h -> false);
        DefaultMavenExecutionRequest mer = new DefaultMavenExecutionRequest();
        MavenSession ms = new MavenSession(null, rss, mer, null);
        DefaultSession session =
                new DefaultSession(ms, mock(RepositorySystem.class), Collections.emptyList(), null, null, null);

        assertEquals(
                RootLocator.UNABLE_TO_FIND_ROOT_PROJECT_MESSAGE,
                assertThrows(IllegalStateException.class, session::getRootDirectory)
                        .getMessage());
    }

    @Test
    void testRootDirectory() {
        RepositorySystemSession rss = new DefaultRepositorySystemSession(h -> false);
        DefaultMavenExecutionRequest mer = new DefaultMavenExecutionRequest();
        MavenSession ms = new MavenSession(null, rss, mer, null);
        ms.getRequest().setRootDirectory(Paths.get("myRootDirectory"));
        DefaultSession session =
                new DefaultSession(ms, mock(RepositorySystem.class), Collections.emptyList(), null, null, null);

        assertEquals(Paths.get("myRootDirectory"), session.getRootDirectory());
    }

    @Test
    void projectsWithTheSameGavKeepDistinctIdentity() {
        DefaultSession session = projectSession();
        MavenProject first = project("first");
        MavenProject second = project("second");

        Project firstSnapshot = session.getProject(first);
        Project secondSnapshot = session.getProject(second);

        assertNotSame(firstSnapshot, secondSnapshot);
        assertSame(first, session.getMavenProject(firstSnapshot));
        assertSame(second, session.getMavenProject(secondSnapshot));
        assertSame(firstSnapshot, session.getProject(first));
        assertSame(secondSnapshot, session.getProject(second));
    }

    @Test
    void derivedSessionsShareProjectIdentity() {
        DefaultSession session = projectSession();
        MavenProject legacyProject = project("derived");
        Project snapshot = session.getProject(legacyProject);

        InternalMavenSession derived = (InternalMavenSession)
                session.withContext(new RequestTrace("derived")).withRemoteRepositories(Collections.emptyList());

        assertSame(snapshot, derived.getProject(legacyProject));
        assertSame(legacyProject, derived.getMavenProject(snapshot));
    }

    @Test
    void updatingOneSameGavProjectReplacesOnlyItsSnapshot() {
        DefaultSession session = projectSession();
        MavenProject first = project("first");
        MavenProject second = project("second");
        Project originalFirst = session.getProject(first);
        Project originalSecond = session.getProject(second);

        first.setVersion("2.0");
        Project updatedFirst = session.updateProject(originalFirst);

        assertEquals("1.0", originalFirst.version());
        assertEquals("2.0", updatedFirst.version());
        assertSame(updatedFirst, session.getProject(first));
        assertSame(originalSecond, session.getProject(second));
    }

    @Test
    void forkedProjectDoesNotShareItsExecutionProjectsHolder() {
        DefaultSession session = projectSession();
        MavenProject executionProject = project("execution");
        MavenProject fork = executionProject.clone();
        fork.setFile(Paths.get("target", "fork", "pom.xml").toAbsolutePath().toFile());
        fork.setExecutionProject(executionProject);

        Project executionSnapshot = session.getProject(executionProject);
        Project forkSnapshot = session.getProject(fork);

        assertNotSame(executionSnapshot, forkSnapshot);
        assertSame(executionProject, session.getMavenProject(executionSnapshot));
        assertSame(fork, session.getMavenProject(forkSnapshot));
    }

    private static RequestTrace enterAndReadTrace(InternalSession session, CyclicBarrier barrier, String data)
            throws Exception {
        RequestTraceHelper.ResolverTrace trace = RequestTraceHelper.enter(session, data);
        try {
            barrier.await();
            return session.getCurrentTrace();
        } finally {
            RequestTraceHelper.exit(trace);
        }
    }

    private static DefaultSession newSession() {
        RepositorySystemSession rss = new DefaultRepositorySystemSession(h -> false);
        MavenSession mavenSession = new MavenSession(null, rss, new DefaultMavenExecutionRequest(), null);
        DefaultSession session = new DefaultSession(
                mavenSession, mock(RepositorySystem.class), Collections.emptyList(), null, null, null);
        InternalSession.associate(rss, session);
        return session;
    }

    private static DefaultSession projectSession() {
        DefaultSession session = spy(newSession());
        Packaging packaging = mock(Packaging.class);
        when(packaging.producesMainArtifact()).thenReturn(false);
        doReturn(packaging).when(session).requirePackaging(anyString());
        return session;
    }

    private static MavenProject project(String directory) {
        MavenProject project = new MavenProject();
        project.setGroupId("group");
        project.setArtifactId("artifact");
        project.setVersion("1.0");
        project.setPackaging("pom");
        project.setFile(
                Paths.get("target", directory, "pom.xml").toAbsolutePath().toFile());
        return project;
    }
}
