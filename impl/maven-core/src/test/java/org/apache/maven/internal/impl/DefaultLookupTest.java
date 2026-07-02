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

import java.util.NoSuchElementException;
import java.util.Optional;

import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DefaultLookupTest {

    private PlexusContainer container;
    private DefaultLookup lookup;

    @BeforeEach
    void setUp() {
        container = mock(PlexusContainer.class);
        lookup = new DefaultLookup(container);
    }

    /**
     * Regression guard for the NPE fix (#12340): when the container returns {@code null} for a type
     * lookup, {@code lookupOptional} must wrap it with {@link Optional#ofNullable} and return an empty
     * {@link Optional} rather than throwing a {@link NullPointerException} from {@code Optional.of}.
     */
    @Test
    void lookupOptionalByTypeReturnsEmptyWhenContainerReturnsNull() throws Exception {
        when(container.lookup(String.class)).thenReturn(null);

        Optional<String> result = lookup.lookupOptional(String.class);

        assertTrue(result.isEmpty(), "expected empty Optional when container returns null");
    }

    /**
     * Same regression guard as above for the {@code (Class, String)} overload.
     */
    @Test
    void lookupOptionalByTypeAndNameReturnsEmptyWhenContainerReturnsNull() throws Exception {
        when(container.lookup(String.class, "hint")).thenReturn(null);

        Optional<String> result = lookup.lookupOptional(String.class, "hint");

        assertTrue(result.isEmpty(), "expected empty Optional when container returns null");
    }

    @Test
    void lookupOptionalByTypeReturnsValueWhenPresent() throws Exception {
        String value = "component";
        when(container.lookup(String.class)).thenReturn(value);

        Optional<String> result = lookup.lookupOptional(String.class);

        assertTrue(result.isPresent());
        assertSame(value, result.get());
    }

    @Test
    void lookupOptionalByTypeAndNameReturnsValueWhenPresent() throws Exception {
        String value = "component";
        when(container.lookup(String.class, "hint")).thenReturn(value);

        Optional<String> result = lookup.lookupOptional(String.class, "hint");

        assertTrue(result.isPresent());
        assertSame(value, result.get());
    }

    /**
     * A {@link ComponentLookupException} whose cause is a {@link NoSuchElementException} signals an
     * absent component and must be translated into an empty {@link Optional}.
     */
    @Test
    void lookupOptionalReturnsEmptyOnNoSuchElementCause() throws Exception {
        ComponentLookupException cle =
                new ComponentLookupException(new NoSuchElementException(), String.class.getName(), "");
        assertSame(NoSuchElementException.class, cle.getCause().getClass(), "test fixture precondition");
        when(container.lookup(String.class)).thenThrow(cle);

        Optional<String> result = lookup.lookupOptional(String.class);

        assertFalse(result.isPresent(), "expected empty Optional when component is absent");
    }

    /**
     * Same as above for the {@code (Class, String)} overload, which has its own catch/translate logic.
     */
    @Test
    void lookupOptionalByTypeAndNameReturnsEmptyOnNoSuchElementCause() throws Exception {
        ComponentLookupException cle =
                new ComponentLookupException(new NoSuchElementException(), String.class.getName(), "hint");
        assertSame(NoSuchElementException.class, cle.getCause().getClass(), "test fixture precondition");
        when(container.lookup(String.class, "hint")).thenThrow(cle);

        Optional<String> result = lookup.lookupOptional(String.class, "hint");

        assertFalse(result.isPresent(), "expected empty Optional when component is absent");
    }
}
