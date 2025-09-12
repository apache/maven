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

import java.util.Map;

import org.apache.maven.api.di.Named;
import org.apache.maven.api.di.Priority;
import org.apache.maven.api.di.Provides;
import org.apache.maven.api.di.Singleton;
import org.apache.maven.api.plugin.testing.stubs.SessionMock;
import org.apache.maven.api.services.DependencyResolver;
import org.apache.maven.api.services.OsService;
import org.apache.maven.api.services.ToolchainManager;
import org.apache.maven.impl.DefaultToolchainManager;
import org.apache.maven.impl.InternalSession;
import org.apache.maven.impl.model.DefaultOsService;
import org.mockito.Mockito;

import static org.apache.maven.api.plugin.testing.MojoExtension.getBasedir;

@Named
public class TestProviders {

    @Provides
    @Singleton
    @SuppressWarnings("unused")
    private static InternalSession getMockSession(
            DependencyResolver dependencyResolver, ToolchainManager toolchainManager, OsService osService) {

        InternalSession session = SessionMock.getMockSession(getBasedir());
        Mockito.when(session.getService(DependencyResolver.class)).thenReturn(dependencyResolver);
        Mockito.when(session.getService(ToolchainManager.class)).thenReturn(toolchainManager);
        Mockito.when(session.getService(OsService.class)).thenReturn(osService);
        return session;
    }

    @Provides
    @Priority(100)
    @Singleton
    @SuppressWarnings("unused")
    private static DependencyResolver getMockDependencyResolver() {
        return Mockito.mock(DependencyResolver.class);
    }

    @Provides
    @Singleton
    @SuppressWarnings("unused")
    private static ToolchainManager getToolchainManager() {
        return new DefaultToolchainManager(Map.of());
    }

    @Provides
    @Singleton
    @Priority(100)
    @SuppressWarnings("unused")
    private static OsService getOsService() {
        return new DefaultOsService();
    }
}
