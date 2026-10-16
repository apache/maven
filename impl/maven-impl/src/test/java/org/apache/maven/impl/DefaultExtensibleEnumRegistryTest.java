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

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.maven.api.ExtensibleEnum;
import org.apache.maven.api.services.Lookup;
import org.apache.maven.api.spi.ExtensibleEnumProvider;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DefaultExtensibleEnumRegistryTest {

    private static final TestValue FOO = new TestValue("foo");
    private static final TestValue BAR = new TestValue("bar");
    private static final TestValue CUSTOM = new TestValue("custom");

    @Test
    void lookupFindsBuiltinValues() {
        var registry = new TestRegistry(List.of(), FOO, BAR);
        assertSame(FOO, registry.lookup("foo").orElseThrow());
        assertSame(BAR, registry.lookup("BAR").orElseThrow());
    }

    @Test
    void lookupFindsConstructorInjectedProviders() {
        TestProvider provider = () -> List.of(CUSTOM);
        var registry = new TestRegistry(List.of(provider));
        assertSame(CUSTOM, registry.lookup("custom").orElseThrow());
    }

    @Test
    void dynamicLookupFindsProjectExtensionProviders() {
        Lookup lookup = mock(Lookup.class);
        TestProvider provider = () -> List.of(CUSTOM);
        when(lookup.lookupList(TestProvider.class)).thenReturn(List.of(provider));

        var registry = new TestRegistry(lookup, List.of());
        assertSame(CUSTOM, registry.lookup("custom").orElseThrow());
    }

    @Test
    void dynamicLookupTakesPriorityOverConstructorInjected() {
        Lookup lookup = mock(Lookup.class);
        TestValue builtin = new TestValue("foo");
        TestValue override = new TestValue("foo");
        TestProvider dynamicProvider = () -> List.of(override);
        TestProvider injectedProvider = () -> List.of(builtin);
        when(lookup.lookupList(TestProvider.class)).thenReturn(List.of(dynamicProvider));

        var registry = new TestRegistry(lookup, List.of(injectedProvider));
        assertSame(override, registry.lookup("foo").orElseThrow());
    }

    @Test
    void dynamicLookupDoesNotRetainProviderAcrossProjects() {
        Lookup lookup = mock(Lookup.class);
        TestValue custom = new TestValue("custom");
        TestProvider provider = () -> List.of(custom);
        AtomicReference<List<TestProvider>> visible = new AtomicReference<>(List.of(provider));
        when(lookup.lookupList(TestProvider.class)).thenAnswer(inv -> visible.get());

        var registry = new TestRegistry(lookup, List.of());

        assertSame(custom, registry.lookup("custom").orElseThrow());
        visible.set(List.of());
        assertTrue(registry.lookup("custom").isEmpty());
    }

    @Test
    void fallsBackToBuiltinWhenDynamicLookupFindsNothing() {
        Lookup lookup = mock(Lookup.class);
        when(lookup.lookupList(TestProvider.class)).thenReturn(List.of());

        TestValue foo = new TestValue("foo");
        var registry = new TestRegistry(lookup, List.of(), foo);
        assertSame(foo, registry.lookup("foo").orElseThrow());
    }

    record TestValue(String id) implements ExtensibleEnum {}

    interface TestProvider extends ExtensibleEnumProvider<TestValue> {}

    static class TestRegistry extends ExtensibleEnumRegistries.DefaultExtensibleEnumRegistry<TestValue, TestProvider> {

        @SafeVarargs
        TestRegistry(List<TestProvider> providers, TestValue... builtins) {
            super(providers, builtins);
        }

        @SafeVarargs
        TestRegistry(Lookup lookup, List<TestProvider> providers, TestValue... builtins) {
            super(lookup, TestProvider.class, providers, builtins);
        }
    }
}
