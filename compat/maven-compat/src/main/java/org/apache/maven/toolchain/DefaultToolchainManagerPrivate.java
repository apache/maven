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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import java.util.List;
import java.util.Map;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.internal.impl.DefaultToolchainManager;
import org.apache.maven.toolchain.model.ToolchainModel;

@Named
@Singleton
public class DefaultToolchainManagerPrivate implements ToolchainManagerPrivate {

    private final DefaultToolchainManager delegate;

    @Inject
    public DefaultToolchainManagerPrivate(DefaultToolchainManager delegate) {
        this.delegate = delegate;
    }

    @Override
    public ToolchainPrivate[] getToolchainsForType(String type, MavenSession session)
            throws MisconfiguredToolchainException {
        try {
            List<org.apache.maven.api.Toolchain> toolchains = delegate.getToolchains(session.getSession(), type);
            return toolchains.stream()
                    .map(tc -> (ToolchainPrivate) new ToolchainWrapper(tc))
                    .toArray(ToolchainPrivate[]::new);
        } catch (org.apache.maven.api.services.ToolchainManagerException e) {
            throw new MisconfiguredToolchainException(e.getMessage(), e);
        }
    }

    @Override
    public void storeToolchainToBuildContext(ToolchainPrivate toolchain, MavenSession session) {
        delegate.storeToolchainToBuildContext(session.getSession(), ((ToolchainWrapper) toolchain).delegate);
    }

    private static class ToolchainWrapper implements ToolchainPrivate {
        private final org.apache.maven.api.Toolchain delegate;

        ToolchainWrapper(org.apache.maven.api.Toolchain delegate) {
            this.delegate = delegate;
        }

        @Override
        public String getType() {
            return delegate.getType();
        }

        @Override
        public ToolchainModel getModel() {
            return new ToolchainModel(delegate.getModel());
        }

        @Override
        public boolean matchesRequirements(Map<String, String> requirements) {
            return delegate.matchesRequirements(requirements);
        }

        @Override
        public String findTool(String toolName) {
            return "";
        }
    }
}
