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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.api.Lifecycle;
import org.apache.maven.api.services.LifecycleRegistry;
import org.apache.maven.api.spi.LifecycleProvider;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static org.apache.maven.internal.impl.Lifecycles.phase;
import static org.apache.maven.internal.impl.Lifecycles.plugin;

/**
 * TODO: this is session scoped as SPI can contribute.
 */
@Named
@Singleton
public class DefaultLifecycleRegistry
        extends ExtensibleEnumRegistries.DefaultExtensibleEnumRegistry<Lifecycle, LifecycleProvider>
        implements LifecycleRegistry {

    public DefaultLifecycleRegistry() {
        super(Collections.emptyList());
    }

    @Inject
    public DefaultLifecycleRegistry(
            List<LifecycleProvider> providers, Map<String, org.apache.maven.lifecycle.Lifecycle> lifecycles) {
        super(
                concat(providers, new LifecycleWrapperProvider(lifecycles)),
                new CleanLifecycle(),
                new DefaultLifecycle(),
                new SiteLifecycle(),
                new WrapperLifecycle());
        // validate lifecycle
        for (Lifecycle lifecycle : this) {
            Set<String> set = new HashSet<>();
            lifecycle.phases().forEach(phase -> {
                if (!set.add(phase.name())) {
                    throw new IllegalArgumentException(
                            "Found duplicated phase '" + phase.name() + "' in '" + lifecycle.id() + "' lifecycle");
                }
            });
        }
    }

    @Override
    public Iterator<Lifecycle> iterator() {
        return values.values().iterator();
    }

    @Override
    public Stream<Lifecycle> stream() {
        return values.values().stream();
    }

    static <T> List<T> concat(List<T> l, T t) {
        List<T> nl = new ArrayList<>(l.size() + 1);
        nl.addAll(l);
        nl.add(t);
        return nl;
    }

    @Override
    public List<String> computePhases(Lifecycle lifecycle) {
        return lifecycle.phases().stream().map(Lifecycle.Phase::name).toList();
    }

    static class LifecycleWrapperProvider implements LifecycleProvider {
        private final Map<String, org.apache.maven.lifecycle.Lifecycle> lifecycles;

        @Inject
        LifecycleWrapperProvider(Map<String, org.apache.maven.lifecycle.Lifecycle> lifecycles) {
            this.lifecycles = lifecycles;
        }

        @Override
        public Collection<Lifecycle> provides() {
            return lifecycles.values().stream().map(this::wrap).collect(Collectors.toList());
        }

        private Lifecycle wrap(org.apache.maven.lifecycle.Lifecycle lifecycle) {
            return new Lifecycle() {
                @Override
                public String id() {
                    return lifecycle.getId();
                }

                @Override
                public Collection<Phase> phases() {
                    // TODO: implement
                    throw new UnsupportedOperationException();
                }
            };
        }
    }

    static class CleanLifecycle implements Lifecycle {

        private static final String MAVEN_CLEAN_PLUGIN_VERSION = "3.2.0";

        @Override
        public String id() {
            return Lifecycle.CLEAN;
        }

        @Override
        public Collection<Phase> phases() {
            return asList(
                    phase("pre-clean"),
                    phase(
                            "clean",
                            plugin(
                                    "org.apache.maven.plugins:maven-clean-plugin:" + MAVEN_CLEAN_PLUGIN_VERSION
                                            + ":clean",
                                    "clean")),
                    phase("post-clean"));
        }
    }

    static class DefaultLifecycle implements Lifecycle {
        @Override
        public String id() {
            return Lifecycle.DEFAULT;
        }

        @Override
        public Collection<Phase> phases() {
            return asList(
                    phase("validate"),
                    phase("initialize"),
                    phase("generate-sources"),
                    phase("process-sources"),
                    phase("generate-resources"),
                    phase("process-resources"),
                    phase("compile"),
                    phase("process-classes"),
                    phase("generate-test-sources"),
                    phase("process-test-sources"),
                    phase("generate-test-resources"),
                    phase("process-test-resources"),
                    phase("test-compile"),
                    phase("process-test-classes"),
                    phase("test"),
                    phase("prepare-package"),
                    phase("package"),
                    phase("pre-integration-test"),
                    phase("integration-test"),
                    phase("post-integration-test"),
                    phase("verify"),
                    phase("install"),
                    phase("deploy"));
        }
    }

    static class SiteLifecycle implements Lifecycle {

        private static final String MAVEN_SITE_PLUGIN_VERSION = "3.12.1";

        @Override
        public String id() {
            return Lifecycle.SITE;
        }

        @Override
        public Collection<Phase> phases() {
            return asList(
                    phase("pre-site"),
                    phase(
                            "site",
                            plugin(
                                    "org.apache.maven.plugins:maven-site-plugin:" + MAVEN_SITE_PLUGIN_VERSION + ":site",
                                    "site")),
                    phase("post-site"),
                    phase(
                            "site-deploy",
                            plugin(
                                    "org.apache.maven.plugins:maven-site-plugin:" + MAVEN_SITE_PLUGIN_VERSION
                                            + ":deploy",
                                    "site-deploy")));
        }
    }

    static class WrapperLifecycle implements Lifecycle {

        private static final String MAVEN_WRAPPER_PLUGIN_VERSION = "3.2.0";

        @Override
        public String id() {
            return WRAPPER;
        }

        @Override
        public Collection<Phase> phases() {
            return singleton(phase(
                    "wrapper",
                    plugin(
                            "org.apache.maven.plugins:maven-wrapper-plugin:" + MAVEN_WRAPPER_PLUGIN_VERSION
                                    + ":wrapper",
                            "wrapper")));
        }
    }
}
