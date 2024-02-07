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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.apache.maven.api.Type;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.services.LanguageRegistry;
import org.apache.maven.api.services.TypeRegistry;
import org.apache.maven.api.spi.TypeProvider;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.handler.manager.LegacyArtifactHandlerManager;
import org.apache.maven.eventspy.AbstractEventSpy;
import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.repository.internal.type.DefaultType;

import static java.util.function.Function.identity;
import static org.apache.maven.internal.impl.Utils.nonNull;

@Named
@Singleton
public class DefaultTypeRegistry extends AbstractEventSpy implements TypeRegistry {
    private final Map<String, Type> types;

    private final LanguageRegistry languageRegistry;

    private final ConcurrentHashMap<String, Type> usedTypes;

    private final LegacyArtifactHandlerManager manager;

    @Inject
    public DefaultTypeRegistry(
            List<TypeProvider> providers, LanguageRegistry languageRegistry, LegacyArtifactHandlerManager manager) {
        this.types = nonNull(providers, "providers").stream()
                .flatMap(p -> p.provides().stream())
                .collect(Collectors.toMap(Type::id, identity()));
        this.languageRegistry = nonNull(languageRegistry, "languageRegistry");
        this.usedTypes = new ConcurrentHashMap<>();
        this.manager = nonNull(manager, "artifactHandlerManager");
    }

    @Override
    public void onEvent(Object event) {
        if (event instanceof ExecutionEvent) {
            ExecutionEvent executionEvent = (ExecutionEvent) event;
            if (executionEvent.getType() == ExecutionEvent.Type.SessionEnded) {
                usedTypes.clear();
            }
        }
    }

    @Override
    public Optional<Type> lookup(String id) {
        return Optional.of(require(id));
    }

    @Override
    @Nonnull
    public Type require(String id) {
        nonNull(id, "id");
        return usedTypes.computeIfAbsent(id, i -> {
            Type type = types.get(id);
            if (type == null) {
                // Copy data as the ArtifactHandler is not immutable, but Type should be.
                ArtifactHandler handler = manager.getArtifactHandler(id);
                type = new DefaultType(
                        id,
                        languageRegistry.require(handler.getLanguage()),
                        handler.getExtension(),
                        handler.getClassifier(),
                        handler.isAddedToClasspath(),
                        handler.isIncludesDependencies());
            }
            return type;
        });
    }
}
