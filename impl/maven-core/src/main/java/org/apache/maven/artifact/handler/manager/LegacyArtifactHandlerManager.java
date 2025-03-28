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
import java.util.concurrent.ConcurrentHashMap;

import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.eventspy.AbstractEventSpy;
import org.apache.maven.execution.ExecutionEvent;

import static java.util.Objects.requireNonNull;

/**
 */
@Named
@Singleton
public class LegacyArtifactHandlerManager extends AbstractEventSpy {
    private final Map<String, ArtifactHandler> artifactHandlers;

    private final Map<String, ArtifactHandler> allHandlers = new ConcurrentHashMap<>();

    @Inject
    public LegacyArtifactHandlerManager(Map<String, ArtifactHandler> artifactHandlers) {
        this.artifactHandlers = requireNonNull(artifactHandlers);
    }

    @Override
    public void onEvent(Object event) {
        if (event instanceof ExecutionEvent executionEvent) {
            if (executionEvent.getType() == ExecutionEvent.Type.SessionEnded) {
                allHandlers.clear();
            }
        }
    }

    public ArtifactHandler getArtifactHandler(String type) {
        requireNonNull(type, "null type");
        ArtifactHandler handler = allHandlers.get(type);
        if (handler == null) {
            handler = artifactHandlers.get(type);
            if (handler == null) {
                handler = new DefaultArtifactHandler(type);
            } else {
                allHandlers.put(type, handler);
            }
        }
        return handler;
    }
}
