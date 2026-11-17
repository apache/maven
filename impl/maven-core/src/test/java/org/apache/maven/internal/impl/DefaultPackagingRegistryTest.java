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

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import org.apache.maven.api.Language;
import org.apache.maven.api.Packaging;
import org.apache.maven.api.Type;
import org.apache.maven.api.model.PluginContainer;
import org.apache.maven.api.services.Lookup;
import org.apache.maven.api.services.TypeRegistry;
import org.apache.maven.lifecycle.mapping.LifecycleMapping;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class DefaultPackagingRegistryTest {

    @Test
    void defaultProducesMainArtifactIsTrue() {
        Packaging custom = new Packaging() {
            @Override
            public String id() {
                return "custom";
            }

            @Override
            public Type type() {
                return Mockito.mock(Type.class);
            }

            @Override
            public Language language() {
                return Language.JAVA_FAMILY;
            }

            @Override
            public Map<String, PluginContainer> plugins() {
                return Collections.emptyMap();
            }
        };
        assertTrue(custom.producesMainArtifact());
    }

    @Test
    void pomPackagingDoesNotProduceMainArtifact() {
        Lookup lookup = Mockito.mock(Lookup.class);
        TypeRegistry typeRegistry = Mockito.mock(TypeRegistry.class);
        LifecycleMapping lifecycleMapping = Mockito.mock(LifecycleMapping.class);
        when(lifecycleMapping.getLifecycles()).thenReturn(Collections.emptyMap());
        Type pomType = Mockito.mock(Type.class);
        when(pomType.id()).thenReturn(Type.POM);

        when(lookup.lookupOptional(LifecycleMapping.class, "pom")).thenReturn(Optional.of(lifecycleMapping));
        when(typeRegistry.lookup("pom")).thenReturn(Optional.of(pomType));

        DefaultPackagingRegistry registry = new DefaultPackagingRegistry(lookup, typeRegistry, Collections.emptyList());
        Optional<Packaging> packaging = registry.lookup("pom");

        assertTrue(packaging.isPresent());
        assertFalse(packaging.get().producesMainArtifact());
    }

    @Test
    void bomPackagingDoesNotProduceMainArtifact() {
        Lookup lookup = Mockito.mock(Lookup.class);
        TypeRegistry typeRegistry = Mockito.mock(TypeRegistry.class);
        LifecycleMapping lifecycleMapping = Mockito.mock(LifecycleMapping.class);
        when(lifecycleMapping.getLifecycles()).thenReturn(Collections.emptyMap());
        Type bomType = Mockito.mock(Type.class);
        when(bomType.id()).thenReturn(Type.BOM);

        when(lookup.lookupOptional(LifecycleMapping.class, "bom")).thenReturn(Optional.of(lifecycleMapping));
        when(typeRegistry.lookup("bom")).thenReturn(Optional.of(bomType));

        DefaultPackagingRegistry registry = new DefaultPackagingRegistry(lookup, typeRegistry, Collections.emptyList());
        Optional<Packaging> packaging = registry.lookup("bom");

        assertTrue(packaging.isPresent());
        assertFalse(packaging.get().producesMainArtifact());
    }

    @Test
    void jarPackagingProducesMainArtifact() {
        Lookup lookup = Mockito.mock(Lookup.class);
        TypeRegistry typeRegistry = Mockito.mock(TypeRegistry.class);
        LifecycleMapping lifecycleMapping = Mockito.mock(LifecycleMapping.class);
        when(lifecycleMapping.getLifecycles()).thenReturn(Collections.emptyMap());
        Type jarType = Mockito.mock(Type.class);
        when(jarType.id()).thenReturn(Type.JAR);

        when(lookup.lookupOptional(LifecycleMapping.class, "jar")).thenReturn(Optional.of(lifecycleMapping));
        when(typeRegistry.lookup("jar")).thenReturn(Optional.of(jarType));

        DefaultPackagingRegistry registry = new DefaultPackagingRegistry(lookup, typeRegistry, Collections.emptyList());
        Optional<Packaging> packaging = registry.lookup("jar");

        assertTrue(packaging.isPresent());
        assertTrue(packaging.get().producesMainArtifact());
    }
}
