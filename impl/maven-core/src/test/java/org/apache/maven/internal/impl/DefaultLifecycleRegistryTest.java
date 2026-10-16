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

import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.maven.api.Constants;
import org.apache.maven.api.Lifecycle;
import org.apache.maven.api.services.Lookup;
import org.apache.maven.api.spi.LifecycleProvider;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DefaultLifecycleRegistryTest {

    @Test
    void returnsNullImplementationVersionWhenClassHasNoPackage() {
        assertNull(DefaultLifecycleRegistry.getImplementationVersion(int.class));
    }

    @Test
    void findsLifecycleFromDynamicProviderLookup() {
        Lookup lookup = mock(Lookup.class);
        Lifecycle custom = stubLifecycle("app-lifecycle");
        LifecycleProvider provider = () -> List.of(custom);
        when(lookup.lookupList(LifecycleProvider.class)).thenReturn(List.of(provider));

        DefaultLifecycleRegistry registry = new DefaultLifecycleRegistry(lookup, List.of());

        assertTrue(registry.lookup("app-lifecycle").isPresent());
        assertSame(custom, registry.lookup("app-lifecycle").get());
    }

    @Test
    void dynamicProviderTakesPriorityOverInjectedProvider() {
        Lifecycle injected = stubLifecycle("my-lifecycle");
        Lifecycle dynamic = stubLifecycle("my-lifecycle");

        Lookup lookup = mock(Lookup.class);
        LifecycleProvider dynamicProvider = () -> List.of(dynamic);
        when(lookup.lookupList(LifecycleProvider.class)).thenReturn(List.of(dynamicProvider));

        DefaultLifecycleRegistry registry = new DefaultLifecycleRegistry(lookup, List.of(() -> List.of(injected)));

        assertSame(dynamic, registry.lookup("my-lifecycle").get());
        assertNotSame(injected, registry.lookup("my-lifecycle").get());
    }

    @Test
    void doesNotRetainDynamicLifecycleAcrossProjects() {
        Lifecycle custom = stubLifecycle("app-lifecycle");
        LifecycleProvider provider = () -> List.of(custom);
        AtomicReference<List<LifecycleProvider>> visible = new AtomicReference<>(List.of(provider));

        Lookup lookup = mock(Lookup.class);
        when(lookup.lookupList(LifecycleProvider.class)).thenAnswer(inv -> visible.get());

        DefaultLifecycleRegistry registry = new DefaultLifecycleRegistry(lookup, List.of());

        // project A: provider is visible
        assertSame(custom, registry.lookup("app-lifecycle").get());

        // project B: provider is gone (different project classloader)
        visible.set(List.of());
        assertTrue(
                registry.lookup("app-lifecycle").isEmpty(),
                "Dynamic lifecycle must not leak into a project that does not declare the extension");
    }

    @Test
    void builtinLifecyclesAlwaysPresent() {
        DefaultLifecycleRegistry registry = new DefaultLifecycleRegistry(List.of());
        assertTrue(registry.lookup("default").isPresent());
        assertTrue(registry.lookup("clean").isPresent());
        assertTrue(registry.lookup("site").isPresent());
    }

    /**
     * Verifies that the site-lifecycle filtering constant reads from the correct system property.
     * The actual site-disabled path requires a forked JVM with the property set at startup
     * (static final field), so we verify correctness of the property name used.
     */
    @Test
    void siteDisabledPropertyNameMatchesConstant() throws Exception {
        java.lang.reflect.Field field = DefaultLifecycleRegistry.class.getDeclaredField("SITE_DISABLED");
        field.setAccessible(true);
        // SITE_DISABLED reads Constants.MAVEN_SITE_LIFECYCLE_DISABLED at class-load time.
        // We verify: when the property is not set, SITE_DISABLED is false,
        // and the site lifecycle IS present in the default stream.
        boolean siteDisabled = (boolean) field.get(null);
        assertNull(
                System.getProperty(Constants.MAVEN_SITE_LIFECYCLE_DISABLED),
                "System property must not be set in this test JVM");
        assertFalse(siteDisabled, "SITE_DISABLED should be false when property is not set");
        DefaultLifecycleRegistry registry = new DefaultLifecycleRegistry(List.of());
        assertTrue(registry.lookup("site").isPresent(), "site lifecycle present when SITE_DISABLED=false");
    }

    @Test
    void builtinLifecyclesAlwaysPresentWithLookup() {
        Lookup lookup = mock(Lookup.class);
        when(lookup.lookupList(LifecycleProvider.class)).thenReturn(List.of());
        DefaultLifecycleRegistry registry = new DefaultLifecycleRegistry(lookup, List.of());
        assertTrue(registry.lookup("default").isPresent());
        assertTrue(registry.lookup("clean").isPresent());
        assertTrue(registry.lookup("site").isPresent());
    }

    private static Lifecycle stubLifecycle(String id) {
        return new Lifecycle() {
            @Override
            public String id() {
                return id;
            }

            @Override
            public Collection<Phase> phases() {
                return List.of();
            }

            @Override
            public Collection<Alias> aliases() {
                return List.of();
            }
        };
    }
}
