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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.maven.api.toolchain.ToolchainModel;
import org.apache.maven.execution.MavenSession;
import org.slf4j.Logger;

/**
 * TODO: refactor this, component extending component is bad practice.
 *
 * @author mkleint
 * @author Robert Scholte
 */
@Named
@Singleton
public class DefaultToolchainManagerPrivate extends DefaultToolchainManager implements ToolchainManagerPrivate {
    @Inject
    public DefaultToolchainManagerPrivate(Map<String, ToolchainFactory> factories) {
        super(factories);
    }

    /**
     * Ctor needed for UT.
     */
    DefaultToolchainManagerPrivate(Map<String, ToolchainFactory> factories, Logger logger) {
        super(factories, logger);
    }

    @Override
    public ToolchainPrivate[] getToolchainsForType(String type, MavenSession session)
            throws MisconfiguredToolchainException {
        List<ToolchainPrivate> toRet = new ArrayList<>();

        ToolchainFactory fact = factories.get(type);
        if (fact == null) {
            logger.error("Missing toolchain factory for type: " + type + ". Possibly caused by misconfigured project.");
        } else {
            List<ToolchainModel> availableToolchains =
                    org.apache.maven.toolchain.model.ToolchainModel.toolchainModelToApiV4(
                            session.getRequest().getToolchains().get(type));

            if (availableToolchains != null) {
                for (ToolchainModel toolchainModel : availableToolchains) {
                    org.apache.maven.toolchain.model.ToolchainModel tm =
                            new org.apache.maven.toolchain.model.ToolchainModel(toolchainModel);
                    toRet.add(fact.createToolchain(tm));
                }
            }

            // add default toolchain
            ToolchainPrivate tool = fact.createDefaultToolchain();
            if (tool != null) {
                toRet.add(tool);
            }
        }

        return toRet.toArray(new ToolchainPrivate[0]);
    }

    @Override
    public void storeToolchainToBuildContext(ToolchainPrivate toolchain, MavenSession session) {
        Map<String, Object> context = retrieveContext(session);
        context.put(getStorageKey(toolchain.getType()), toolchain.getModel());
    }
}
