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

import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.di.Inject;
import org.apache.maven.api.services.ToolchainFactoryException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.impl.MappedList;
import org.apache.maven.toolchain.model.ToolchainModel;
import org.slf4j.Logger;

// @Named
// @Singleton
// @Priority(10)
public class DefaultToolchainManagerOld implements ToolchainManager, ToolchainManagerPrivate {

    private final org.apache.maven.impl.DefaultToolchainManager delegate;

    @Inject
    public DefaultToolchainManagerOld(
            Map<String, ToolchainFactory> v3Factories,
            Map<String, org.apache.maven.api.services.ToolchainFactory> v4Factories) {
        this(v3Factories, v4Factories, null);
    }

    protected DefaultToolchainManagerOld(
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
                            .map(DefaultToolchainManagerOld.this::getToolchainV4);
                }
            });
        }
        allFactories.putAll(v4Factories);
        this.delegate = new org.apache.maven.impl.DefaultToolchainManager(allFactories, logger) {};
    }

    @Override
    public Toolchain getToolchainFromBuildContext(String type, MavenSession session) {
        return delegate.getToolchainFromBuildContext(session.getSession(), type)
                .map(this::getToolchainV3)
                .orElse(null);
    }

    @Override
    public List<Toolchain> getToolchains(MavenSession session, String type, Map<String, String> requirements) {
        return new MappedList<>(delegate.getToolchains(session.getSession(), type, requirements), this::getToolchainV3);
    }

    @Override
    public ToolchainPrivate[] getToolchainsForType(String type, MavenSession session)
            throws MisconfiguredToolchainException {
        try {
            List<org.apache.maven.api.Toolchain> toolchains = delegate.getToolchains(session.getSession(), type);
            return toolchains.stream().map(this::getToolchainV3).toArray(ToolchainPrivate[]::new);
        } catch (org.apache.maven.api.services.ToolchainManagerException e) {
            throw new MisconfiguredToolchainException(e.getMessage(), e);
        }
    }

    @Override
    public void storeToolchainToBuildContext(ToolchainPrivate toolchain, MavenSession session) {
        org.apache.maven.api.Toolchain tc = getToolchainV4(toolchain);
        delegate.storeToolchainToBuildContext(session.getSession(), tc);
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
