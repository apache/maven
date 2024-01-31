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
package org.apache.maven.artifact.handler.manager;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.maven.api.Type;
import org.apache.maven.api.services.TypeRegistry;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.eventspy.AbstractEventSpy;
import org.apache.maven.execution.ExecutionEvent;

import static java.util.Objects.requireNonNull;

/**
 */
@Named
@Singleton
public class DefaultArtifactHandlerManager extends AbstractEventSpy implements ArtifactHandlerManager {
    private final TypeRegistry typeRegistry;

    private final ConcurrentHashMap<String, ArtifactHandler> allHandlers;

    @Inject
    public DefaultArtifactHandlerManager(TypeRegistry typeRegistry) {
        this.typeRegistry = requireNonNull(typeRegistry, "null typeRegistry");
        this.allHandlers = new ConcurrentHashMap<>();
    }

    @Override
    public void onEvent(Object event) {
        if (event instanceof ExecutionEvent) {
            ExecutionEvent executionEvent = (ExecutionEvent) event;
            if (executionEvent.getType() == ExecutionEvent.Type.SessionEnded) {
                allHandlers.clear();
            }
        }
    }

    public ArtifactHandler getArtifactHandler(String id) {
        return allHandlers.computeIfAbsent(id, k -> {
            Type type = typeRegistry.require(id);
            return new DefaultArtifactHandler(
                    id,
                    type.getExtension(),
                    type.getClassifier(),
                    null,
                    null,
                    type.isIncludesDependencies(),
                    type.getLanguage().id(),
                    type.isBuildPathConstituent());
        });

        // Note: here, type decides is artifact added to "build path" (for example during resolution)
        // and "build path" is intermediate data that is used to create actual Java classpath/modulepath
        // but to create those, proper filtering should happen via Type properties.
    }

    public void addHandlers(Map<String, ArtifactHandler> handlers) {
        throw new UnsupportedOperationException("Adding handlers programmatically is not supported anymore");
    }

    @Deprecated
    public Set<String> getHandlerTypes() {
        throw new UnsupportedOperationException("Querying handlers programmatically is not supported anymore");
    }
}
