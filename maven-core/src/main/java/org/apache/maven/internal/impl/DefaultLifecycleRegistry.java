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

import java.util.*;
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
import static java.util.Collections.*;
import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static org.apache.maven.internal.impl.Lifecycles.*;
import static org.apache.maven.internal.impl.Lifecycles.after;
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
        lifecycle.aliases().forEach(alias -> {
            String n = alias.v3Phase();
            String a = alias.v4Phase();
            String[] u = a.split(":");
            Graph.Vertex v = graph.addVertex(n);
            if (u.length > 1) {
                if ("pre".equals(u[0])) {
                    graph.addEdge(graph.addVertex("$" + u[1]), v);
                    graph.addEdge(v, graph.addVertex("$$" + u[1]));
                } else if ("post".equals(u[0])) {
                    graph.addEdge(graph.addVertex(u[1]), v);
                    graph.addEdge(v, graph.addVertex("$$$" + u[1]));
                }
            } else {
                graph.addEdge(graph.addVertex("$$" + u[0]), v);
                graph.addEdge(v, graph.addVertex(u[0]));
            }
        });
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
                throw new IllegalArgumentException(
                        "List of phases differ in size: expected " + computed.size() + " but received " + given.size()
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
            return singleton(phase(
                    "clean",
                    plugin(
                            "org.apache.maven.plugins:maven-clean-plugin:" + MAVEN_CLEAN_PLUGIN_VERSION + ":clean",
                            "clean")));
        }

        @Override
        public Collection<Alias> aliases() {
            return asList(alias("pre-clean", PRE + "clean"), alias("post-clean", POST + "clean"));
        }
    }

    static class DefaultLifecycle implements Lifecycle {
        @Override
        public String id() {
            return Lifecycle.DEFAULT;
        }

        @Override
        public Collection<Phase> phases() {
            return asList(phase(
                    "all",
                    phase(
                            "build",
                            phase("initialize", phase("validate")),
                            phase("sources", after("initialize")),
                            phase("resources", after("initialize")),
                            phase("compile", after("sources"), dependencies("compile", READY)),
                            phase(READY, after("compile"), after("resources")),
                            phase(PACKAGE, after(READY), dependencies("runtime", PACKAGE))),
                    phase(
                            "verify",
                            phase(
                                    "unit-test",
                                    phase("test-sources"),
                                    phase("test-resources"),
                                    phase(
                                            "test-compile",
                                            after("test-sources"),
                                            after(READY),
                                            dependencies("test-only", READY)),
                                    phase(
                                            "test",
                                            after("test-compile"),
                                            after("test-resources"),
                                            dependencies("test", READY))),
                            phase("integration-test", after(PACKAGE))),
                    phase("install", after("verify")), // TODO: this should be after("package")
                    phase("deploy", after("install")))); // TODO: this should be after("package")
        }

        @Override
        public Collection<Alias> aliases() {
            return asList(
                    alias("generate-sources", RUN + "sources"),
                    alias("process-sources", POST + "sources"),
                    alias("generate-resources", RUN + "resources"),
                    alias("process-resources", POST + "resources"),
                    alias("process-classes", POST + "compile"),
                    alias("generate-test-sources", RUN + "test-sources"),
                    alias("process-test-resources", POST + "test-resources"),
                    alias("generate-test-resources", "run:test-resources"),
                    alias("process-test-sources", POST + "test-sources"),
                    alias("process-test-classes", POST + "test-compile"),
                    alias("prepare-package", PRE + PACKAGE),
                    alias("pre-integration-test", PRE + "integration-test"),
                    alias("post-integration-test", POST + "integration-test"));
        }

        @Override
        public Optional<List<String>> orderedPhases() {
            return Optional.of(Arrays.asList(
                    "validate",
                    "initialize",
                    "generate-sources",
                    "process-sources",
                    "sources",
                    "generate-resources",
                    "process-resources",
                    "resources",
                    "compile",
                    "process-classes",
                    READY,
                    "generate-test-sources",
                    "process-test-sources",
                    "test-sources",
                    "generate-test-resources",
                    "process-test-resources",
                    "test-resources",
                    "test-compile",
                    "process-test-classes",
                    "test",
                    "unit-test",
                    "prepare-package",
                    PACKAGE,
                    "build",
                    "pre-integration-test",
                    "integration-test",
                    "post-integration-test",
                    "verify",
                    "install",
                    "deploy",
                    "all"));
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
                    phase(
                            "site",
                            plugin(
                                    "org.apache.maven.plugins:maven-site-plugin:" + MAVEN_SITE_PLUGIN_VERSION + ":site",
                                    "site")),
                    phase(
                            "site-deploy",
                            after("site"),
                            plugin(
                                    "org.apache.maven.plugins:maven-site-plugin:" + MAVEN_SITE_PLUGIN_VERSION
                                            + ":deploy",
                                    "site-deploy")));
        }

        @Override
        public Collection<Alias> aliases() {
            return asList(alias("pre-site", PRE + "site"), alias("post-site", POST + "site"));
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

        @Override
        public Collection<Alias> aliases() {
            return emptyList();
        }
    }
}
