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
import org.apache.maven.impl.MappedList;
import org.apache.maven.toolchain.model.ToolchainModel;

@Named
@Singleton
public class DefaultToolchainManager implements ToolchainManager {

    private final org.apache.maven.internal.impl.DefaultToolchainManager delegate;

    @Inject
    public DefaultToolchainManager(org.apache.maven.internal.impl.DefaultToolchainManager delegate) {
        this.delegate = delegate;
    }

    @Override
    public Toolchain getToolchainFromBuildContext(String type, MavenSession session) {
        return delegate.getToolchainFromBuildContext(session.getSession(), type)
                .map(tc -> (Toolchain) new ToolchainWrapper(tc))
                .orElse(null);
    }

    @Override
    public List<Toolchain> getToolchains(MavenSession session, String type, Map<String, String> requirements) {
        return new MappedList<>(
                delegate.getToolchains(session.getSession(), type, requirements), tc -> new ToolchainWrapper(tc));
    }

    private static class ToolchainWrapper implements Toolchain, ToolchainPrivate {
        private final org.apache.maven.api.Toolchain delegate;

        ToolchainWrapper(org.apache.maven.api.Toolchain delegate) {
            this.delegate = delegate;
        }

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
