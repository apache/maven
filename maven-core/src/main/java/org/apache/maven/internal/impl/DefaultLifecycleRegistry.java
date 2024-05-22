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
import javax.inject.Provider;
import javax.inject.Singleton;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.api.Lifecycle;
import org.apache.maven.api.model.Plugin;
import org.apache.maven.api.services.LifecycleRegistry;
import org.apache.maven.api.services.LookupException;
import org.apache.maven.api.spi.ExtensibleEnumProvider;
import org.apache.maven.api.spi.LifecycleProvider;
import org.apache.maven.lifecycle.mapping.LifecyclePhase;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static org.apache.maven.internal.impl.Lifecycles.phase;
import static org.apache.maven.internal.impl.Lifecycles.plugin;

/**
 * TODO: this is session scoped as SPI can contribute.
 */
@Named
@Singleton
public class DefaultLifecycleRegistry implements LifecycleRegistry {

    private final List<LifecycleProvider> providers;

    public DefaultLifecycleRegistry() {
        this(Collections.emptyList());
    }

    @Inject
    public DefaultLifecycleRegistry(List<LifecycleProvider> providers) {
        List<LifecycleProvider> p = new ArrayList<>(providers);
        p.add(() -> List.of(new CleanLifecycle(), new DefaultLifecycle(), new SiteLifecycle(), new WrapperLifecycle()));
        this.providers = p;
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
        return stream().toList().iterator();
    }

    @Override
    public Stream<Lifecycle> stream() {
        return providers.stream().map(ExtensibleEnumProvider::provides).flatMap(Collection::stream);
    }

    @Override
    public Optional<Lifecycle> lookup(String id) {
        return stream().filter(lf -> Objects.equals(id, lf.id())).findAny();
    }

    @Named
    @Singleton
    public static class LifecycleWrapperProvider implements LifecycleProvider {
        private final PlexusContainer container;

        @Inject
        public LifecycleWrapperProvider(PlexusContainer container) {
            this.container = container;
        }

        @Override
        public Collection<Lifecycle> provides() {
            try {
                Map<String, org.apache.maven.lifecycle.Lifecycle> all =
                        container.lookupMap(org.apache.maven.lifecycle.Lifecycle.class);
                return all.keySet().stream()
                        .filter(id -> !Lifecycle.CLEAN.equals(id)
                                && !Lifecycle.DEFAULT.equals(id)
                                && !Lifecycle.SITE.equals(id)
                                && !Lifecycle.WRAPPER.equals(id))
                        .map(id -> wrap(all.get(id)))
                        .collect(Collectors.toList());
            } catch (ComponentLookupException e) {
                throw new LookupException(e);
            }
        }

        private Lifecycle wrap(org.apache.maven.lifecycle.Lifecycle lifecycle) {
            return new Lifecycle() {
                @Override
                public String id() {
                    return lifecycle.getId();
                }

                @Override
                public Collection<Phase> phases() {
                    return lifecycle.getPhases().stream()
                            .map(name -> (Phase) new Phase() {
                                @Override
                                public String name() {
                                    return name;
                                }

                                @Override
                                public List<Plugin> plugins() {
                                    Map<String, LifecyclePhase> lfPhases = lifecycle.getDefaultLifecyclePhases();
                                    LifecyclePhase phase = lfPhases != null ? lfPhases.get(name) : null;
                                    if (phase != null) {
                                        Map<String, Plugin> plugins = new LinkedHashMap<>();
                                        DefaultPackagingRegistry.parseLifecyclePhaseDefinitions(plugins, name, phase);
                                        return plugins.values().stream().toList();
                                    }
                                    return List.of();
                                }
                            })
                            .toList();
                }
            };
        }
    }

    static class WrappedLifecycle extends org.apache.maven.lifecycle.Lifecycle {
        WrappedLifecycle(Lifecycle lifecycle) {
            super(lifecycle);
        }
    }

    abstract static class BaseLifecycleProvider implements Provider<org.apache.maven.lifecycle.Lifecycle> {
        @Inject
        private PlexusContainer lookup;

        private final String name;

        BaseLifecycleProvider(String name) {
            this.name = name;
        }

        @Override
        public org.apache.maven.lifecycle.Lifecycle get() {
            try {
                LifecycleRegistry registry = lookup.lookup(LifecycleRegistry.class);
                return new WrappedLifecycle(registry.require(name));
            } catch (ComponentLookupException e) {
                throw new LookupException(e);
            }
        }
    }

    @Singleton
    @Named(Lifecycle.CLEAN)
    static class CleanLifecycleProvider extends BaseLifecycleProvider {
        CleanLifecycleProvider() {
            super(Lifecycle.CLEAN);
        }
    }

    @Singleton
    @Named(Lifecycle.DEFAULT)
    static class DefaultLifecycleProvider extends BaseLifecycleProvider {
        DefaultLifecycleProvider() {
            super(Lifecycle.DEFAULT);
        }
    }

    @Singleton
    @Named(Lifecycle.SITE)
    static class SiteLifecycleProvider extends BaseLifecycleProvider {
        SiteLifecycleProvider() {
            super(Lifecycle.SITE);
        }
    }

    @Singleton
    @Named(Lifecycle.WRAPPER)
    static class WrapperLifecycleProvider extends BaseLifecycleProvider {
        WrapperLifecycleProvider() {
            super(Lifecycle.WRAPPER);
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
