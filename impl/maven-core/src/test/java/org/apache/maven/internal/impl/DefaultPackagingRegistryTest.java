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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.maven.api.Packaging;
import org.apache.maven.api.Type;
import org.apache.maven.api.services.Lookup;
import org.apache.maven.api.services.TypeRegistry;
import org.apache.maven.api.spi.PackagingProvider;
import org.apache.maven.lifecycle.mapping.LifecycleMapping;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DefaultPackagingRegistryTest {

    @Test
    void findsProviderVisibleThroughProjectLookup() {
        Lookup lookup = mock(Lookup.class);
        TypeRegistry types = mock(TypeRegistry.class);
        Packaging packaging = packaging("app-client");
        PackagingProvider provider = () -> List.of(packaging);
        when(lookup.lookupList(PackagingProvider.class)).thenReturn(List.of(provider));

        DefaultPackagingRegistry registry = new DefaultPackagingRegistry(lookup, types, List.of());

        assertSame(packaging, registry.lookup("APP-CLIENT").orElseThrow());
    }

    @Test
    void doesNotRetainProviderFromAnotherProjectLookup() {
        Lookup lookup = mock(Lookup.class);
        Packaging packaging = packaging("app-client");
        PackagingProvider provider = () -> List.of(packaging);
        AtomicReference<List<PackagingProvider>> visible = new AtomicReference<>(List.of(provider));
        when(lookup.lookupList(PackagingProvider.class)).thenAnswer(invocation -> visible.get());

        DefaultPackagingRegistry registry = new DefaultPackagingRegistry(lookup, mock(TypeRegistry.class), List.of());

        assertSame(packaging, registry.lookup("app-client").orElseThrow());
        visible.set(List.of());
        assertTrue(registry.lookup("app-client").isEmpty());
    }

    @Test
    void findsProviderInjectedWhenRegistryWasConstructed() {
        Lookup lookup = mock(Lookup.class);
        Packaging packaging = packaging("app-client");
        PackagingProvider provider = () -> List.of(packaging);

        DefaultPackagingRegistry registry =
                new DefaultPackagingRegistry(lookup, mock(TypeRegistry.class), List.of(provider));

        assertSame(packaging, registry.lookup("app-client").orElseThrow());
    }

    @Test
    void retainsLegacyLifecycleMappingFallback() {
        Lookup lookup = mock(Lookup.class);
        TypeRegistry types = mock(TypeRegistry.class);
        LifecycleMapping mapping = mock(LifecycleMapping.class);
        Type type = mock(Type.class);
        when(lookup.lookupOptional(LifecycleMapping.class, "jar")).thenReturn(Optional.of(mapping));
        when(mapping.getLifecycles()).thenReturn(Map.of());
        when(types.lookup("jar")).thenReturn(Optional.of(type));

        DefaultPackagingRegistry registry = new DefaultPackagingRegistry(lookup, types, List.of());
        Packaging packaging = registry.lookup("jar").orElseThrow();

        assertEquals("jar", packaging.id());
        assertSame(type, packaging.type());
        assertTrue(packaging.plugins().isEmpty());
    }

    private static Packaging packaging(String id) {
        Packaging packaging = mock(Packaging.class);
        when(packaging.id()).thenReturn(id);
        return packaging;
    }
}
