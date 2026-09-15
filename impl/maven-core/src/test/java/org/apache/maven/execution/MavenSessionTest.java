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
package org.apache.maven.execution;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.eclipse.aether.RepositorySystemSession;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link MavenSession}.
 */
class MavenSessionTest {

    @Test
    void testCurrentProjectIsExecutionRootRegardlessOfReactorOrder() {
        MavenSession session = createSessionWithoutContainer();
        MavenProject dependency = new MavenProject();
        MavenProject root = new MavenProject();
        root.setExecutionRoot(true);

        session.setProjects(List.of(dependency, root));
        assertSame(root, session.getCurrentProject());
        assertSame(root, session.getTopLevelProject());
        assertSame(dependency, session.getProjects().get(0));
        assertSame(root, session.getProjects().get(1));

        session.setProjects(List.of(root, dependency));
        assertSame(root, session.getCurrentProject());
        assertSame(root, session.getTopLevelProject());
    }

    @Test
    void testCurrentProjectFallsBackToFirstSelectedProject() {
        MavenSession session = createSessionWithoutContainer();
        MavenProject root = new MavenProject();
        root.setExecutionRoot(true);
        MavenProject selected = new MavenProject();
        MavenProject dependency = new MavenProject();
        session.setAllProjects(List.of(root, selected, dependency));

        session.setProjects(List.of(selected, dependency));
        assertSame(selected, session.getCurrentProject());
        assertSame(selected, session.getTopLevelProject());

        session.setProjects(List.of(dependency, selected));
        assertSame(dependency, session.getCurrentProject());
        assertSame(dependency, session.getTopLevelProject());
    }

    @Test
    void testSetProjectsResetsCurrentProject() {
        MavenSession session = createSessionWithoutContainer();
        assertNull(session.getCurrentProject());
        MavenProject root = new MavenProject();
        root.setExecutionRoot(true);
        MavenProject module = new MavenProject();
        session.setProjects(List.of(module, root));

        session.setCurrentProject(module);
        assertSame(module, session.getCurrentProject());
        assertSame(root, session.getTopLevelProject());
        session.setCurrentProject(null);
        assertNull(session.getCurrentProject());

        session.setProjects(List.of(module, root));
        assertSame(root, session.getCurrentProject());
        session.setProjects(List.of());
        assertNull(session.getCurrentProject());
        assertNull(session.getTopLevelProject());
        assertEquals(List.of(), session.getProjects());

        session.setProjects(List.of(module));
        assertSame(module, session.getCurrentProject());
        assertSame(module, session.getTopLevelProject());
    }

    @Test
    void testCurrentProjectIsThreadLocal() throws Exception {
        MavenSession session = createSessionWithoutContainer();
        MavenProject root = new MavenProject();
        root.setExecutionRoot(true);
        MavenProject module = new MavenProject();
        session.setProjects(List.of(module, root));
        session.setCurrentProject(module);

        var executor = Executors.newSingleThreadExecutor();
        try {
            executor.submit(() -> {
                        assertSame(root, session.getCurrentProject());
                        session.setCurrentProject(null);
                        assertNull(session.getCurrentProject());
                    })
                    .get(10, TimeUnit.SECONDS);
            assertSame(module, session.getCurrentProject());

            session.setProjects(List.of(module, root));
            assertSame(root, session.getCurrentProject());
            assertSame(root, executor.submit(session::getCurrentProject).get(10, TimeUnit.SECONDS));
            session.setProjects(List.of());
            assertNull(executor.submit(session::getCurrentProject).get(10, TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void testCloneKeepsRuntimeCurrentProject() throws Exception {
        MavenSession session = createSessionWithoutContainer();
        MavenProject root = new MavenProject();
        root.setExecutionRoot(true);
        MavenProject module = new MavenProject();
        session.setProjects(List.of(module, root));
        session.setCurrentProject(module);

        MavenSession clone = session.clone();
        session.setCurrentProject(root);
        assertSame(module, clone.getCurrentProject());
        var executor = Executors.newSingleThreadExecutor();
        try {
            assertSame(module, executor.submit(clone::getCurrentProject).get(10, TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
        }
        clone.setCurrentProject(null);
        assertSame(root, session.getCurrentProject());
    }

    /**
     * Creates a MavenSession without a PlexusContainer (Maven 4 style).
     */
    private MavenSession createSessionWithoutContainer() {
        RepositorySystemSession repoSession = mock(RepositorySystemSession.class);
        MavenExecutionRequest request = new DefaultMavenExecutionRequest();
        MavenExecutionResult result = new DefaultMavenExecutionResult();
        return new MavenSession(repoSession, request, result);
    }

    /**
     * Creates a MavenSession with a PlexusContainer (Maven 3 style).
     */
    @SuppressWarnings("deprecation")
    private MavenSession createSessionWithContainer(PlexusContainer container) {
        RepositorySystemSession repoSession = mock(RepositorySystemSession.class);
        MavenExecutionRequest request = new DefaultMavenExecutionRequest();
        MavenExecutionResult result = new DefaultMavenExecutionResult();
        return new MavenSession(container, repoSession, request, result);
    }

    @Test
    @SuppressWarnings("deprecation")
    void testLookupWithoutContainerThrowsUnsupportedOperationException() {
        MavenSession session = createSessionWithoutContainer();

        UnsupportedOperationException exception =
                assertThrows(UnsupportedOperationException.class, () -> session.lookup("role"));
        assertEquals(
                "PlexusContainer is not available in this session. "
                        + "Plugins should use JSR 330 (@Inject) injection instead of "
                        + "MavenSession.lookup(). The MavenSession Plexus lookup methods are deprecated.",
                exception.getMessage());
    }

    @Test
    @SuppressWarnings("deprecation")
    void testLookupWithRoleHintWithoutContainerThrowsUnsupportedOperationException() {
        MavenSession session = createSessionWithoutContainer();

        assertThrows(UnsupportedOperationException.class, () -> session.lookup("role", "hint"));
    }

    @Test
    @SuppressWarnings("deprecation")
    void testLookupListWithoutContainerThrowsUnsupportedOperationException() {
        MavenSession session = createSessionWithoutContainer();

        assertThrows(UnsupportedOperationException.class, () -> session.lookupList("role"));
    }

    @Test
    @SuppressWarnings("deprecation")
    void testLookupMapWithoutContainerThrowsUnsupportedOperationException() {
        MavenSession session = createSessionWithoutContainer();

        assertThrows(UnsupportedOperationException.class, () -> session.lookupMap("role"));
    }

    @Test
    @SuppressWarnings("deprecation")
    void testGetContainerWithoutContainerReturnsNull() {
        MavenSession session = createSessionWithoutContainer();

        assertNull(session.getContainer());
    }

    @Test
    @SuppressWarnings("deprecation")
    void testLookupWithContainerDelegates() throws ComponentLookupException {
        PlexusContainer container = mock(PlexusContainer.class);
        Object expected = new Object();
        when(container.lookup("role")).thenReturn(expected);

        MavenSession session = createSessionWithContainer(container);

        assertEquals(expected, session.lookup("role"));
    }

    @Test
    @SuppressWarnings("deprecation")
    void testLookupWithRoleHintWithContainerDelegates() throws ComponentLookupException {
        PlexusContainer container = mock(PlexusContainer.class);
        Object expected = new Object();
        when(container.lookup("role", "hint")).thenReturn(expected);

        MavenSession session = createSessionWithContainer(container);

        assertEquals(expected, session.lookup("role", "hint"));
    }

    @Test
    @SuppressWarnings("deprecation")
    void testLookupListWithContainerDelegates() throws ComponentLookupException {
        PlexusContainer container = mock(PlexusContainer.class);
        List<Object> expected = Collections.singletonList(new Object());
        when(container.lookupList("role")).thenReturn(expected);

        MavenSession session = createSessionWithContainer(container);

        assertEquals(expected, session.lookupList("role"));
    }

    @Test
    @SuppressWarnings("deprecation")
    void testLookupMapWithContainerDelegates() throws ComponentLookupException {
        PlexusContainer container = mock(PlexusContainer.class);
        Map<String, Object> expected = Collections.singletonMap("key", new Object());
        when(container.lookupMap("role")).thenReturn(expected);

        MavenSession session = createSessionWithContainer(container);

        assertEquals(expected, session.lookupMap("role"));
    }
}
