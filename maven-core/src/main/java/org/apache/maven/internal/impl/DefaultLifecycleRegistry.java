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
import java.util.Arrays;
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

import static org.apache.maven.api.Lifecycle.Phase.BUILD;
import static org.apache.maven.api.Lifecycle.Phase.COMPILE;
import static org.apache.maven.api.Lifecycle.Phase.DEPLOY;
import static org.apache.maven.api.Lifecycle.Phase.INITIALIZE;
import static org.apache.maven.api.Lifecycle.Phase.INSTALL;
import static org.apache.maven.api.Lifecycle.Phase.INTEGRATION_TEST;
import static org.apache.maven.api.Lifecycle.Phase.PACKAGE;
import static org.apache.maven.api.Lifecycle.Phase.READY;
import static org.apache.maven.api.Lifecycle.Phase.RESOURCES;
import static org.apache.maven.api.Lifecycle.Phase.SOURCES;
import static org.apache.maven.api.Lifecycle.Phase.TEST;
import static org.apache.maven.api.Lifecycle.Phase.TEST_COMPILE;
import static org.apache.maven.api.Lifecycle.Phase.TEST_RESOURCES;
import static org.apache.maven.api.Lifecycle.Phase.TEST_SOURCES;
import static org.apache.maven.api.Lifecycle.Phase.UNIT_TEST;
import static org.apache.maven.api.Lifecycle.Phase.VALIDATE;
import static org.apache.maven.api.Lifecycle.Phase.VERIFY;
import static org.apache.maven.internal.impl.Lifecycles.after;
import static org.apache.maven.internal.impl.Lifecycles.alias;
import static org.apache.maven.internal.impl.Lifecycles.dependencies;
import static org.apache.maven.internal.impl.Lifecycles.phase;
import static org.apache.maven.internal.impl.Lifecycles.plugin;

/**
 * TODO: this is session scoped as SPI can contribute.
 */
@Named
@Singleton
public class DefaultLifecycleRegistry implements LifecycleRegistry {

    private static final String MAVEN_PLUGINS = "org.apache.maven.plugins:";

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
            lifecycle.allPhases().forEach(phase -> {
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

    public List<String> computePhases(Lifecycle lifecycle) {
        Graph graph = new Graph();
        lifecycle.phases().forEach(phase -> addPhase(graph, null, null, phase));
        List<String> allPhases = graph.visitAll();
        Collections.reverse(allPhases);
        List<String> computed =
                allPhases.stream().filter(s -> !s.startsWith("$")).collect(Collectors.toList());
        List<String> given = lifecycle.orderedPhases().orElse(null);
        if (given != null) {
            if (given.size() != computed.size()) {
                Set<String> s1 =
                        given.stream().filter(s -> !computed.contains(s)).collect(Collectors.toSet());
                Set<String> s2 =
                        computed.stream().filter(s -> !given.contains(s)).collect(Collectors.toSet());
                throw new IllegalStateException(
                        "List of phases differ in size: expected " + computed.size() + ", but received " + given.size()
                                + (s1.isEmpty() ? "" : ", missing " + s1)
                                + (s2.isEmpty() ? "" : ", unexpected " + s2));
            }
            return given;
        }
        return computed;
    }

    private static void addPhase(
            Graph graph, Graph.Vertex before, Graph.Vertex after, org.apache.maven.api.Lifecycle.Phase phase) {
        Graph.Vertex ep0 = graph.addVertex("$" + phase.name());
        Graph.Vertex ep1 = graph.addVertex("$$" + phase.name());
        Graph.Vertex ep2 = graph.addVertex(phase.name());
        Graph.Vertex ep3 = graph.addVertex("$$$" + phase.name());
        graph.addEdge(ep0, ep1);
        graph.addEdge(ep1, ep2);
        graph.addEdge(ep2, ep3);
        if (before != null) {
            graph.addEdge(before, ep0);
        }
        if (after != null) {
            graph.addEdge(ep3, after);
        }
        phase.links().forEach(link -> {
            if (link.pointer().type() == Lifecycle.Pointer.Type.PROJECT) {
                if (link.kind() == Lifecycle.Link.Kind.AFTER) {
                    graph.addEdge(graph.addVertex(link.pointer().phase()), ep0);
                } else {
                    graph.addEdge(ep3, graph.addVertex("$" + link.pointer().phase()));
                }
            }
        });
        phase.phases().forEach(child -> addPhase(graph, ep1, ep2, child));
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
                                public List<Phase> phases() {
                                    return List.of();
                                }

                                @Override
                                public Stream<Phase> allPhases() {
                                    return Stream.concat(
                                            Stream.of(this), phases().stream().flatMap(Lifecycle.Phase::allPhases));
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

                                @Override
                                public Collection<Link> links() {
                                    return List.of();
                                }
                            })
                            .toList();
                }

                @Override
                public Collection<Alias> aliases() {
                    return Collections.emptyList();
                }
            };
        }
    }

    static class WrappedLifecycle extends org.apache.maven.lifecycle.Lifecycle {
        WrappedLifecycle(LifecycleRegistry registry, Lifecycle lifecycle) {
            super(registry, lifecycle);
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
                return new WrappedLifecycle(registry, registry.require(name));
            } catch (ComponentLookupException e) {
                throw new LookupException(e);
            }
        }
    }

    @Singleton
    @Named(Lifecycle.CLEAN)
    @SuppressWarnings("unused")
    static class CleanLifecycleProvider extends BaseLifecycleProvider {
        CleanLifecycleProvider() {
            super(Lifecycle.CLEAN);
        }
    }

    @Singleton
    @Named(Lifecycle.DEFAULT)
    @SuppressWarnings("unused")
    static class DefaultLifecycleProvider extends BaseLifecycleProvider {
        DefaultLifecycleProvider() {
            super(Lifecycle.DEFAULT);
        }
    }

    @Singleton
    @Named(Lifecycle.SITE)
    @SuppressWarnings("unused")
    static class SiteLifecycleProvider extends BaseLifecycleProvider {
        SiteLifecycleProvider() {
            super(Lifecycle.SITE);
        }
    }

    @Singleton
    @Named(Lifecycle.WRAPPER)
    @SuppressWarnings("unused")
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
            return List.of(phase(
                    Phase.CLEAN,
                    plugin(
                            MAVEN_PLUGINS + "maven-clean-plugin:" + MAVEN_CLEAN_PLUGIN_VERSION + ":clean",
                            Phase.CLEAN)));
        }

        @Override
        public Collection<Alias> aliases() {
            return List.of(alias("pre-clean", BEFORE + Phase.CLEAN), alias("post-clean", AFTER + Phase.CLEAN));
        }
    }

    static class DefaultLifecycle implements Lifecycle {
        @Override
        public String id() {
            return Lifecycle.DEFAULT;
        }

        @Override
        public Collection<Phase> phases() {
            return List.of(phase(
                    "all",
                    phase(INITIALIZE, phase(VALIDATE)),
                    phase(
                            BUILD,
                            after(VALIDATE),
                            phase(SOURCES),
                            phase(RESOURCES),
                            phase(COMPILE, after(SOURCES), dependencies(COMPILE, READY)),
                            phase(READY, after(COMPILE), after(RESOURCES)),
                            phase(PACKAGE, after(READY), dependencies("runtime", PACKAGE))),
                    phase(
                            VERIFY,
                            after(VALIDATE),
                            phase(
                                    UNIT_TEST,
                                    phase(TEST_SOURCES),
                                    phase(TEST_RESOURCES),
                                    phase(
                                            TEST_COMPILE,
                                            after(TEST_SOURCES),
                                            after(READY),
                                            dependencies("test-only", READY)),
                                    phase(
                                            TEST,
                                            after(TEST_COMPILE),
                                            after(TEST_RESOURCES),
                                            dependencies("test", READY))),
                            phase(INTEGRATION_TEST)),
                    phase(INSTALL, after(PACKAGE)),
                    phase(DEPLOY, after(PACKAGE))));
        }

        @Override
        public Collection<Alias> aliases() {
            return List.of(
                    alias("generate-sources", SOURCES),
                    alias("process-sources", AFTER + SOURCES),
                    alias("generate-resources", RESOURCES),
                    alias("process-resources", AFTER + RESOURCES),
                    alias("process-classes", AFTER + COMPILE),
                    alias("generate-test-sources", TEST_SOURCES),
                    alias("process-test-sources", AFTER + TEST_SOURCES),
                    alias("generate-test-resources", TEST_RESOURCES),
                    alias("process-test-resources", AFTER + TEST_RESOURCES),
                    alias("process-test-classes", AFTER + TEST_COMPILE),
                    alias("prepare-package", BEFORE + PACKAGE),
                    alias("pre-integration-test", BEFORE + INTEGRATION_TEST),
                    alias("post-integration-test", AFTER + INTEGRATION_TEST));
        }

        @Override
        public Optional<List<String>> orderedPhases() {
            return Optional.of(Arrays.asList(
                    VALIDATE,
                    INITIALIZE,
                    // "generate-sources",
                    SOURCES,
                    // "process-sources",
                    // "generate-resources",
                    RESOURCES,
                    // "process-resources",
                    COMPILE,
                    // "process-classes",
                    READY,
                    // "generate-test-sources",
                    TEST_SOURCES,
                    // "process-test-sources",
                    // "generate-test-resources",
                    TEST_RESOURCES,
                    // "process-test-resources",
                    TEST_COMPILE,
                    // "process-test-classes",
                    TEST,
                    UNIT_TEST,
                    // "prepare-package",
                    PACKAGE,
                    BUILD,
                    // "pre-integration-test",
                    INTEGRATION_TEST,
                    // "post-integration-test",
                    VERIFY,
                    INSTALL,
                    DEPLOY,
                    "all"));
        }
    }

    static class SiteLifecycle implements Lifecycle {

        private static final String MAVEN_SITE_PLUGIN_VERSION = "3.12.1";
        private static final String MAVEN_SITE_PLUGIN =
                MAVEN_PLUGINS + "maven-site-plugin:" + MAVEN_SITE_PLUGIN_VERSION + ":";
        private static final String PHASE_SITE = "site";
        private static final String PHASE_SITE_DEPLOY = "site-deploy";

        @Override
        public String id() {
            return Lifecycle.SITE;
        }

        @Override
        public Collection<Phase> phases() {
            return List.of(
                    phase(PHASE_SITE, plugin(MAVEN_SITE_PLUGIN + "site", PHASE_SITE)),
                    phase(
                            PHASE_SITE_DEPLOY,
                            after(PHASE_SITE),
                            plugin(MAVEN_SITE_PLUGIN + "deploy", PHASE_SITE_DEPLOY)));
        }

        @Override
        public Collection<Alias> aliases() {
            return List.of(alias("pre-site", BEFORE + PHASE_SITE), alias("post-site", AFTER + PHASE_SITE));
        }
    }

    static class WrapperLifecycle implements Lifecycle {

        private static final String MAVEN_WRAPPER_PLUGIN_VERSION = "3.2.0";
        private static final String PHASE_WRAPPER = "wrapper";

        @Override
        public String id() {
            return WRAPPER;
        }

        @Override
        public Collection<Phase> phases() {
            return List.of(phase(
                    PHASE_WRAPPER,
                    plugin(
                            MAVEN_PLUGINS + "maven-wrapper-plugin:" + MAVEN_WRAPPER_PLUGIN_VERSION + ":wrapper",
                            PHASE_WRAPPER)));
        }

        @Override
        public Collection<Alias> aliases() {
            return List.of();
        }
    }
}
