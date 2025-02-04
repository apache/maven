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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.maven.api.Project;
import org.apache.maven.api.Session;
import org.apache.maven.api.Toolchain;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.annotations.Nullable;
import org.apache.maven.api.services.Lookup;
import org.apache.maven.api.services.ToolchainFactory;
import org.apache.maven.api.services.ToolchainFactoryException;
import org.apache.maven.api.services.ToolchainManager;
import org.apache.maven.api.services.ToolchainManagerException;
import org.apache.maven.api.toolchain.ToolchainModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class DefaultToolchainManager implements ToolchainManager {
    private final Map<String, ToolchainFactory> factories;
    private final Logger logger;

    @Inject
    public DefaultToolchainManager(Map<String, ToolchainFactory> factories) {
        this.factories = factories;
        this.logger = LoggerFactory.getLogger(DefaultToolchainManager.class);
    }

    protected DefaultToolchainManager(Map<String, ToolchainFactory> factories, Logger logger) {
        this.factories = factories;
        this.logger = logger;
    }

    @Nonnull
    @Override
    public List<Toolchain> getToolchains(
            @Nonnull Session session, @Nonnull String type, @Nullable Map<String, String> requirements)
            throws ToolchainManagerException {
        ToolchainFactory factory = factories.get(Objects.requireNonNull(type, "type"));
        if (factory == null) {
            logger.error("Missing toolchain factory for type: " + type + ". Possibly caused by misconfigured project.");
            return List.of();
        }
        return Stream.concat(
                        session.getToolchains().stream()
                                .filter(model -> Objects.equals(type, model.getType()))
                                .map(this::createToolchain)
                                .flatMap(Optional::stream),
                        factory.createDefaultToolchain().stream())
                .filter(toolchain -> requirements == null || toolchain.matchesRequirements(requirements))
                .toList();
    }

    @Nonnull
    @Override
    public Optional<Toolchain> getToolchainFromBuildContext(@Nonnull Session session, @Nonnull String type)
            throws ToolchainManagerException {
        Map<String, Object> context = retrieveContext(session);
        ToolchainModel model = (ToolchainModel) context.get("toolchain-" + type);
        return model != null ? createToolchain(model) : Optional.empty();
    }

    @Override
    public void storeToolchainToBuildContext(@Nonnull Session session, @Nonnull Toolchain toolchain) {
        Map<String, Object> context = retrieveContext(session);
        context.put("toolchain-" + toolchain.getType(), toolchain.getModel());
    }

    private Optional<Toolchain> createToolchain(ToolchainModel model) {
        String type = Objects.requireNonNull(model.getType(), "model.getType()");
        ToolchainFactory factory = factories.get(type);
        if (factory != null) {
            try {
                return Optional.of(factory.createToolchain(model));
            } catch (ToolchainFactoryException e) {
                throw new ToolchainManagerException("Error creating toolchain of type " + type, e);
            }
        } else {
            logger.error("Missing toolchain factory for type: " + type + ". Possibly caused by misconfigured project.");
        }
        return Optional.empty();
    }

    protected Map<String, Object> retrieveContext(Session session) {
        Optional<Project> current = session.getService(Lookup.class).lookupOptional(Project.class);
        return current.map(session::getPluginContext).orElseGet(HashMap::new);
    }
}
