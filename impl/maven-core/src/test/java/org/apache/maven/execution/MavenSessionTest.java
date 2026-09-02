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

import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.eclipse.aether.RepositorySystemSession;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link MavenSession}.
 */
class MavenSessionTest {

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
