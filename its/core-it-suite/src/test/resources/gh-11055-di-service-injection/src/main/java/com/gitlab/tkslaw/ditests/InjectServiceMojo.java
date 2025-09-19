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
package com.gitlab.tkslaw.ditests;

import org.apache.maven.api.di.Inject;
import org.apache.maven.api.plugin.annotations.Mojo;
import org.apache.maven.api.services.ArtifactManager;
import org.apache.maven.api.services.DependencyResolver;
import org.apache.maven.api.services.OsService;
import org.apache.maven.api.services.ToolchainManager;

@Mojo(name = "inject-service")
public class InjectServiceMojo extends DITestsMojoBase {
    @Inject
    protected ArtifactManager artifactManager;

    @Inject
    protected DependencyResolver dependencyResolver;

    @Inject
    protected ToolchainManager toolchainManager;

    @Inject
    protected OsService osService;

    @Override
    public void execute() {
        super.execute();

        log.info("Logging services injected via @Inject");
        logService("artifactManager", artifactManager);
        logService("dependencyResolver", dependencyResolver);
        logService("toolchainManager", toolchainManager);
        logService("osService", osService);
    }
}
