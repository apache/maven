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
package org.apache.maven.toolchain;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.maven.api.Session;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.annotations.Nullable;
import org.apache.maven.api.di.Inject;
import org.apache.maven.api.di.Named;
import org.apache.maven.api.di.Priority;
import org.apache.maven.api.di.Provides;
import org.apache.maven.api.di.Singleton;
import org.apache.maven.api.di.Typed;
import org.apache.maven.api.services.Lookup;
import org.apache.maven.api.services.ToolchainFactoryException;
import org.apache.maven.api.services.ToolchainManagerException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.impl.MappedList;
import org.apache.maven.toolchain.model.ToolchainModel;
import org.slf4j.Logger;

@Named
@Singleton
@Deprecated(since = "4.0.0")
class ToolchainManagerFactory {

    private final Lookup lookup;
    private final Logger logger;

    @Inject
    ToolchainManagerFactory(Lookup lookup) {
        this(lookup, null);
    }

    protected ToolchainManagerFactory(Lookup lookup, Logger logger) {
        this.lookup = lookup;
        this.logger = logger;
    }

    @Provides
    @Typed({ToolchainManager.class, ToolchainManagerPrivate.class})
    @Named // qualifier is required for SiduDIBridge to work
    DefaultToolchainManagerV3 v3Manager() {
        return new DefaultToolchainManagerV3();
    }

    @Provides
    @Priority(10)
    @Typed(org.apache.maven.api.services.ToolchainManager.class)
    DefaultToolchainManagerV4 v4Manager() {
        return new DefaultToolchainManagerV4();
    }

    private org.apache.maven.impl.DefaultToolchainManager getDelegate() {
        return getToolchainManager(lookup, logger);
    }

    private org.apache.maven.impl.DefaultToolchainManager getToolchainManager(Lookup lookup, Logger logger) {
        return getToolchainManager(
                lookup.lookupMap(ToolchainFactory.class),
                lookup.lookupMap(org.apache.maven.api.services.ToolchainFactory.class),
                logger);
    }

    private org.apache.maven.impl.DefaultToolchainManager getToolchainManager(
            Map<String, ToolchainFactory> v3Factories,
            Map<String, org.apache.maven.api.services.ToolchainFactory> v4Factories,
            Logger logger) {
        Map<String, org.apache.maven.api.services.ToolchainFactory> allFactories = new HashMap<>();
        for (Map.Entry<String, ToolchainFactory> entry : v3Factories.entrySet()) {
            ToolchainFactory v3Factory = entry.getValue();
            allFactories.put(entry.getKey(), new org.apache.maven.api.services.ToolchainFactory() {
                @Nonnull
                @Override
                public org.apache.maven.api.Toolchain createToolchain(
                        @Nonnull org.apache.maven.api.toolchain.ToolchainModel model) throws ToolchainFactoryException {
                    try {
                        return getToolchainV4(v3Factory.createToolchain(new ToolchainModel(model)));
                    } catch (MisconfiguredToolchainException e) {
                        throw new RuntimeException(e);
                    }
                }

                @Nonnull
                @Override
                public Optional<org.apache.maven.api.Toolchain> createDefaultToolchain()
                        throws ToolchainFactoryException {
                    return Optional.ofNullable(v3Factory.createDefaultToolchain())
                            .map(ToolchainManagerFactory.this::getToolchainV4);
                }
            });
        }
        allFactories.putAll(v4Factories);
        return new org.apache.maven.impl.DefaultToolchainManager(allFactories, logger) {};
    }

    class DefaultToolchainManagerV4 implements org.apache.maven.api.services.ToolchainManager {
        @Nonnull
        @Override
        public List<org.apache.maven.api.Toolchain> getToolchains(
                @Nonnull Session session, @Nonnull String type, @Nullable Map<String, String> requirements)
                throws ToolchainManagerException {
            return getDelegate().getToolchains(session, type, requirements);
        }

        @Nonnull
        @Override
        public Optional<org.apache.maven.api.Toolchain> getToolchainFromBuildContext(
                @Nonnull Session session, @Nonnull String type) throws ToolchainManagerException {
            return getDelegate().getToolchainFromBuildContext(session, type);
        }

        @Override
        public void storeToolchainToBuildContext(
                @Nonnull Session session, @Nonnull org.apache.maven.api.Toolchain toolchain) {
            getDelegate().storeToolchainToBuildContext(session, toolchain);
        }

        @Nonnull
        @Override
        public List<org.apache.maven.api.Toolchain> getToolchains(@Nonnull Session session, @Nonnull String type)
                throws ToolchainManagerException {
            return getDelegate().getToolchains(session, type);
        }
    }

    class DefaultToolchainManagerV3 implements ToolchainManager, ToolchainManagerPrivate {

        @Override
        public Toolchain getToolchainFromBuildContext(String type, MavenSession session) {
            return getDelegate()
                    .getToolchainFromBuildContext(session.getSession(), type)
                    .map(ToolchainManagerFactory.this::getToolchainV3)
                    .orElse(null);
        }

        @Override
        public List<Toolchain> getToolchains(MavenSession session, String type, Map<String, String> requirements) {
            return new MappedList<>(
                    getDelegate().getToolchains(session.getSession(), type, requirements),
                    ToolchainManagerFactory.this::getToolchainV3);
        }

        @Override
        public ToolchainPrivate[] getToolchainsForType(String type, MavenSession session)
                throws MisconfiguredToolchainException {
            try {
                List<org.apache.maven.api.Toolchain> toolchains =
                        getDelegate().getToolchains(session.getSession(), type);
                return toolchains.stream()
                        .map(ToolchainManagerFactory.this::getToolchainV3)
                        .toArray(ToolchainPrivate[]::new);
            } catch (org.apache.maven.api.services.ToolchainManagerException e) {
                throw new MisconfiguredToolchainException(e.getMessage(), e);
            }
        }

        @Override
        public void storeToolchainToBuildContext(ToolchainPrivate toolchain, MavenSession session) {
            org.apache.maven.api.Toolchain tc = getToolchainV4(toolchain);
            getDelegate().storeToolchainToBuildContext(session.getSession(), tc);
        }
    }

    private org.apache.maven.api.Toolchain getToolchainV4(ToolchainPrivate toolchain) {
        return toolchain instanceof ToolchainWrapperV3 v3tc ? v3tc.delegate : new ToolchainWrapperV4(toolchain);
    }

    private ToolchainPrivate getToolchainV3(org.apache.maven.api.Toolchain toolchain) {
        return toolchain instanceof ToolchainWrapperV4 v3tc ? v3tc.delegate : new ToolchainWrapperV3(toolchain);
    }

    private record ToolchainWrapperV4(ToolchainPrivate delegate) implements org.apache.maven.api.Toolchain {

        @Override
        public String getType() {
            return delegate.getType();
        }

        @Override
        public String findTool(String toolName) {
            return delegate.findTool(toolName);
        }

        @Override
        public org.apache.maven.api.toolchain.ToolchainModel getModel() {
            return delegate.getModel().getDelegate();
        }

        @Override
        public boolean matchesRequirements(Map<String, String> requirements) {
            return delegate.matchesRequirements(requirements);
        }
    }

    private record ToolchainWrapperV3(org.apache.maven.api.Toolchain delegate) implements Toolchain, ToolchainPrivate {

        @Override
        public String getType() {
            return delegate.getType();
        }

        @Override
        public String findTool(String toolName) {
            return delegate.findTool(toolName);
        }

        @Override
        public boolean matchesRequirements(Map<String, String> requirements) {
            return delegate.matchesRequirements(requirements);
        }

        @Override
        public ToolchainModel getModel() {
            return new ToolchainModel(delegate.getModel());
        }
    }
}
