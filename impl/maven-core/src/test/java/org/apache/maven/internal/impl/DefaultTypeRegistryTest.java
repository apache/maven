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
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.maven.api.Language;
import org.apache.maven.api.PathType;
import org.apache.maven.api.Type;
import org.apache.maven.api.services.LanguageRegistry;
import org.apache.maven.api.services.Lookup;
import org.apache.maven.api.spi.TypeProvider;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.handler.manager.LegacyArtifactHandlerManager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DefaultTypeRegistryTest {

    @Test
    void findsTypeFromDynamicProviderLookup() {
        Lookup lookup = mock(Lookup.class);
        Type customType = stubType("app-client");
        TypeProvider provider = () -> List.of(customType);
        when(lookup.lookupList(TypeProvider.class)).thenReturn(List.of(provider));

        DefaultTypeRegistry registry =
                new DefaultTypeRegistry(lookup, mock(LanguageRegistry.class), mock(LegacyArtifactHandlerManager.class));

        assertSame(customType, registry.require("app-client"));
    }

    @Test
    void doesNotRetainDynamicTypeAcrossProjects() {
        Lookup lookup = mock(Lookup.class);
        LanguageRegistry langReg = mock(LanguageRegistry.class);
        LegacyArtifactHandlerManager handlerMgr = mock(LegacyArtifactHandlerManager.class);
        ArtifactHandler fallbackHandler = stubHandler();
        when(handlerMgr.getArtifactHandler(anyString())).thenReturn(fallbackHandler);
        when(langReg.lookup(anyString())).thenReturn(java.util.Optional.of(Language.NONE));

        Type customType = stubType("app-client");
        TypeProvider provider = () -> List.of(customType);
        AtomicReference<List<TypeProvider>> visible = new AtomicReference<>(List.of(provider));
        when(lookup.lookupList(TypeProvider.class)).thenAnswer(inv -> visible.get());

        DefaultTypeRegistry registry = new DefaultTypeRegistry(lookup, langReg, handlerMgr);

        assertSame(customType, registry.require("app-client"));
        visible.set(List.of());
        Type fallback = registry.require("app-client");
        assertEquals("app-client", fallback.id());
        assertNotSame(customType, fallback);
    }

    @Test
    void fallsBackToLegacyArtifactHandler() {
        Lookup lookup = mock(Lookup.class);
        LanguageRegistry langReg = mock(LanguageRegistry.class);
        LegacyArtifactHandlerManager handlerMgr = mock(LegacyArtifactHandlerManager.class);
        ArtifactHandler handler = stubHandler();
        when(lookup.lookupList(TypeProvider.class)).thenReturn(List.of());
        when(handlerMgr.getArtifactHandler("jar")).thenReturn(handler);
        when(langReg.lookup("java")).thenReturn(java.util.Optional.of(Language.JAVA_FAMILY));

        DefaultTypeRegistry registry = new DefaultTypeRegistry(lookup, langReg, handlerMgr);
        Type type = registry.require("jar");

        assertEquals("jar", type.id());
    }

    private static Type stubType(String id) {
        return new Type() {
            @Override
            public String id() {
                return id;
            }

            @Override
            public Language getLanguage() {
                return Language.JAVA_FAMILY;
            }

            @Override
            public String getExtension() {
                return "jar";
            }

            @Override
            public String getClassifier() {
                return null;
            }

            @Override
            public boolean isIncludesDependencies() {
                return false;
            }

            @Override
            public Set<PathType> getPathTypes() {
                return Set.of();
            }
        };
    }

    private static ArtifactHandler stubHandler() {
        ArtifactHandler handler = mock(ArtifactHandler.class);
        when(handler.getLanguage()).thenReturn("java");
        when(handler.getExtension()).thenReturn("jar");
        when(handler.getClassifier()).thenReturn(null);
        when(handler.isIncludesDependencies()).thenReturn(false);
        when(handler.isAddedToClasspath()).thenReturn(true);
        return handler;
    }
}
