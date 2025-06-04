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
package org.apache.maven.project;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.maven.api.Lifecycle;
import org.apache.maven.api.Packaging;
import org.apache.maven.api.Type;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.annotations.Nullable;
import org.apache.maven.api.di.Inject;
import org.apache.maven.api.di.Named;
import org.apache.maven.api.di.Priority;
import org.apache.maven.api.di.Singleton;
import org.apache.maven.api.model.Plugin;
import org.apache.maven.api.model.PluginContainer;
import org.apache.maven.api.model.PluginExecution;
import org.apache.maven.api.services.LifecycleRegistry;
import org.apache.maven.api.services.PackagingRegistry;
import org.apache.maven.impl.model.DefaultLifecycleBindingsInjector;

import static org.apache.maven.api.Lifecycle.DEFAULT;

@Singleton
@Named
@Priority(5)
public class EmptyLifecycleBindingsInjector extends DefaultLifecycleBindingsInjector {

    private static LifecycleRegistry lifecycleRegistry;
    private static PackagingRegistry packagingRegistry;

    private static final LifecycleRegistry EMPTY_LIFECYCLE_REGISTRY = new LifecycleRegistry() {

        @Override
        public Iterator<Lifecycle> iterator() {
            return Collections.emptyIterator();
        }

        @Override
        public Optional<Lifecycle> lookup(String id) {
            return Optional.empty();
        }

        @Override
        public List<String> computePhases(Lifecycle lifecycle) {
            return List.of();
        }
    };

    private static final PackagingRegistry EMPTY_PACKAGING_REGISTRY = new PackagingRegistry() {
        @Override
        public Optional<Packaging> lookup(String id) {
            return Optional.of(new Packaging() {
                @Override
                public String id() {
                    return id;
                }

                @Override
                @Nullable
                public Type type() {
                    return null;
                }

                @Override
                public Map<String, PluginContainer> plugins() {
                    if ("JAR".equals(id)) {
                        return Map.of(
                                DEFAULT,
                                PluginContainer.newBuilder()
                                        .plugins(List.of(
                                                newPlugin("maven-compiler-plugin", "compile", "testCompile"),
                                                newPlugin("maven-resources-plugin", "resources", "testResources"),
                                                newPlugin("maven-surefire-plugin", "test"),
                                                newPlugin("maven-jar-plugin", "jar"),
                                                newPlugin("maven-install-plugin", "install"),
                                                newPlugin("maven-deploy-plugin", "deploy")))
                                        .build());
                    } else {
                        return Map.of();
                    }
                }
            });
        }
    };

    @Inject
    public EmptyLifecycleBindingsInjector(LifecycleRegistry lifecycleRegistry, PackagingRegistry packagingRegistry) {
        super(new WrapperLifecycleRegistry(), new WrapperPackagingRegistry());
        EmptyLifecycleBindingsInjector.lifecycleRegistry = lifecycleRegistry;
        EmptyLifecycleBindingsInjector.packagingRegistry = packagingRegistry;
    }

    public static void useEmpty() {
        lifecycleRegistry = EMPTY_LIFECYCLE_REGISTRY;
        packagingRegistry = EMPTY_PACKAGING_REGISTRY;
    }

    private static Plugin newPlugin(String artifactId, String... goals) {
        return Plugin.newBuilder()
                .groupId("org.apache.maven.plugins")
                .artifactId(artifactId)
                .executions(Arrays.stream(goals)
                        .map(goal -> PluginExecution.newBuilder()
                                .id("default-" + goal)
                                .goals(List.of(goal))
                                .build())
                        .toList())
                .build();
    }

    static class WrapperLifecycleRegistry implements LifecycleRegistry {
        @Override
        @Nonnull
        public Optional<Lifecycle> lookup(String id) {
            return getDelegate().lookup(id);
        }

        @Override
        public Iterator<Lifecycle> iterator() {
            return getDelegate().iterator();
        }

        protected LifecycleRegistry getDelegate() {
            return lifecycleRegistry;
        }

        @Override
        public List<String> computePhases(Lifecycle lifecycle) {
            return List.of();
        }
    }

    static class WrapperPackagingRegistry implements PackagingRegistry {
        @Override
        public Optional<Packaging> lookup(String id) {
            return getDelegate().lookup(id);
        }

        private PackagingRegistry getDelegate() {
            return packagingRegistry;
        }
    }
}
