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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.maven.api.DependencyScope;
import org.apache.maven.api.Lifecycle;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.model.InputLocation;
import org.apache.maven.api.model.InputSource;
import org.apache.maven.api.model.Plugin;
import org.apache.maven.api.services.LifecycleRegistry;
import org.apache.maven.api.services.LookupException;
import org.apache.maven.api.spi.ExtensibleEnumProvider;
import org.apache.maven.api.spi.LifecycleProvider;
import org.apache.maven.lifecycle.mapping.LifecyclePhase;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;

import static org.apache.maven.api.Lifecycle.AFTER;
import static org.apache.maven.api.Lifecycle.BEFORE;
import static org.apache.maven.api.Lifecycle.Phase.ALL;
import static org.apache.maven.api.Lifecycle.Phase.BUILD;
import static org.apache.maven.api.Lifecycle.Phase.COMPILE;
import static org.apache.maven.api.Lifecycle.Phase.DEPLOY;
import static org.apache.maven.api.Lifecycle.Phase.EACH;
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
import static org.apache.maven.internal.impl.Lifecycles.children;
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

    public static final String DEFAULT_LIFECYCLE_MODELID = "org.apache.maven:maven-core:"
            + DefaultLifecycleRegistry.class.getPackage().getImplementationVersion()
            + ":default-lifecycle-bindings";

    public static final InputLocation DEFAULT_LIFECYCLE_INPUT_LOCATION =
            new InputLocation(new InputSource(DEFAULT_LIFECYCLE_MODELID, null));

    public static final String SCOPE_COMPILE = DependencyScope.COMPILE.id();
    public static final String SCOPE_RUNTIME = DependencyScope.RUNTIME.id();
    public static final String SCOPE_TEST_ONLY = DependencyScope.TEST_ONLY.id();
    public static final String SCOPE_TEST = DependencyScope.TEST.id();

    private final List<LifecycleProvider> providers;

    public DefaultLifecycleRegistry() {
        this(List.of());
    }

    @Inject
    public DefaultLifecycleRegistry(List<LifecycleProvider> providers) {
        List<LifecycleProvider> p = new ArrayList<>(providers);
        p.add(() -> List.of(new CleanLifecycle(), new DefaultLifecycle(), new SiteLifecycle()));
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
    @Nonnull
    public Iterator<Lifecycle> iterator() {
        return stream().toList().iterator();
    }

    @Override
    @Nonnull
    public Stream<Lifecycle> stream() {
        return providers.stream().map(ExtensibleEnumProvider::provides).flatMap(Collection::stream);
    }

    @Override
    @Nonnull
    public Optional<Lifecycle> lookup(@Nonnull String id) {
        return stream().filter(lf -> Objects.equals(id, lf.id())).findAny();
    }

    @Nonnull
    public List<String> computePhases(@Nonnull Lifecycle lifecycle) {
        Graph graph = new Graph();
        addPhases(graph, null, null, lifecycle.v3phases());
        List<String> allPhases = graph.visitAll();
        Collections.reverse(allPhases);
        return allPhases.stream().filter(s -> !s.startsWith("$")).toList();
    }

    private static void addPhase(
            Graph graph, Graph.Vertex before, Graph.Vertex after, org.apache.maven.api.Lifecycle.Phase phase) {
        Graph.Vertex ep0 = graph.addVertex(BEFORE + phase.name());
        Graph.Vertex ep1 = graph.addVertex("$$" + phase.name());
        Graph.Vertex ep2 = graph.addVertex(phase.name());
        Graph.Vertex ep3 = graph.addVertex(AFTER + phase.name());
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
                    graph.addEdge(ep3, graph.addVertex(BEFORE + link.pointer().phase()));
                }
            }
        });
        addPhases(graph, ep1, ep2, phase.phases());
    }

    private static void addPhases(
            Graph graph, Graph.Vertex before, Graph.Vertex after, Collection<Lifecycle.Phase> phases) {
        // We add ordering between internal phases.
        // This would be wrong at execution time, but we are here computing a list and not a graph,
        // so in order to obtain the expected order, we add these links between phases.
        Lifecycle.Phase prev = null;
        for (Lifecycle.Phase child : phases) {
            // add phase
            addPhase(graph, before, after, child);
            if (prev != null) {
                // add link between end of previous phase and beginning of this one
                graph.addEdge(graph.addVertex(AFTER + prev.name()), graph.addVertex(BEFORE + child.name()));
            }
            prev = child;
        }
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
        @Nonnull
        public Collection<Lifecycle> provides() {
            try {
                Map<String, org.apache.maven.lifecycle.Lifecycle> all =
                        container.lookupMap(org.apache.maven.lifecycle.Lifecycle.class);
                return all.keySet().stream()
                        .filter(id -> !Lifecycle.CLEAN.equals(id)
                                && !Lifecycle.DEFAULT.equals(id)
                                && !Lifecycle.SITE.equals(id))
                        .map(id -> (Lifecycle) new WrappedLifecycle(all.get(id)))
                        .toList();
            } catch (ComponentLookupException e) {
                throw new LookupException(e);
            }
        }
    }

    /**
     * Record implementation of Lifecycle.Phase for wrapped phases.
     *
     * @param name The name of the phase
     * @param prev The name of the previous phase (may be null)
     * @param lifecycle The original Maven 3 lifecycle
     */
    record WrappedPhase(String name, String prev, org.apache.maven.lifecycle.Lifecycle lifecycle)
            implements Lifecycle.Phase {
        /**
         * Compact constructor with null validation.
         */
        WrappedPhase {
            Objects.requireNonNull(name, "name cannot be null");
            Objects.requireNonNull(lifecycle, "lifecycle cannot be null");
            // prev can be null for the first phase
        }

        @Override
        @Nonnull
        public List<Lifecycle.Phase> phases() {
            return List.of();
        }

        @Override
        @Nonnull
        public List<Plugin> plugins() {
            Map<String, LifecyclePhase> lfPhases = lifecycle.getDefaultLifecyclePhases();
            return lfPhases != null
                    ? List.copyOf(DefaultPackagingRegistry.parseLifecyclePhaseDefinitions(lfPhases))
                    : List.of();
        }

        @Override
        @Nonnull
        public Collection<Lifecycle.Link> links() {
            if (prev == null) {
                return List.of();
            } else {
                return List.of(new Lifecycles.DefaultLink(
                        Lifecycle.Link.Kind.AFTER, new Lifecycles.DefaultPhasePointer(prev)));
            }
        }
    }

    /**
     * Record implementation of Lifecycle for wrapped lifecycles.
     *
     * @param lifecycle The original Maven 3 lifecycle
     */
    record WrappedLifecycle(org.apache.maven.lifecycle.Lifecycle lifecycle) implements Lifecycle {
        /**
         * Compact constructor with null validation.
         */
        WrappedLifecycle {
            Objects.requireNonNull(lifecycle, "lifecycle cannot be null");
        }

        @Override
        @Nonnull
        public String id() {
            return lifecycle.getId();
        }

        @Override
        @Nonnull
        public Collection<Phase> phases() {
            List<String> names = lifecycle.getPhases();
            List<Phase> phases = new ArrayList<>();
            for (int i = 0; i < names.size(); i++) {
                String name = names.get(i);
                String prev = i > 0 ? names.get(i - 1) : null;
                phases.add(new WrappedPhase(name, prev, lifecycle));
            }
            return phases;
        }

        @Override
        @Nonnull
        public Collection<Alias> aliases() {
            return List.of();
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
                return new org.apache.maven.lifecycle.Lifecycle(registry, registry.require(name));
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

    static class CleanLifecycle implements Lifecycle {

        private static final String MAVEN_CLEAN_PLUGIN_VERSION = "3.4.0";

        @Override
        @Nonnull
        public String id() {
            return Lifecycle.CLEAN;
        }

        @Override
        @Nonnull
        public Collection<Phase> phases() {
            // START SNIPPET: clean
            return List.of(phase(
                    Phase.CLEAN,
                    plugin(
                            MAVEN_PLUGINS + "maven-clean-plugin:" + MAVEN_CLEAN_PLUGIN_VERSION + ":clean",
                            Phase.CLEAN)));
            // END SNIPPET: clean
        }

        @Override
        @Nonnull
        public Collection<Alias> aliases() {
            return List.of(alias("pre-clean", BEFORE + Phase.CLEAN), alias("post-clean", AFTER + Phase.CLEAN));
        }
    }

    static class DefaultLifecycle implements Lifecycle {
        @Override
        @Nonnull
        public String id() {
            return Lifecycle.DEFAULT;
        }

        @Override
        @Nonnull
        public Collection<Phase> phases() {
            // START SNIPPET: default
            return List.of(phase(
                    ALL,
                    children(ALL),
                    phase(
                            EACH,
                            phase(VALIDATE, phase(INITIALIZE)),
                            phase(
                                    BUILD,
                                    after(VALIDATE),
                                    phase(SOURCES),
                                    phase(RESOURCES),
                                    phase(COMPILE, after(SOURCES), dependencies(SCOPE_COMPILE, READY)),
                                    phase(READY, after(COMPILE), after(RESOURCES)),
                                    phase(PACKAGE, after(READY), dependencies(SCOPE_RUNTIME, PACKAGE))),
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
                                                    dependencies(SCOPE_TEST_ONLY, READY)),
                                            phase(
                                                    TEST,
                                                    after(TEST_COMPILE),
                                                    after(TEST_RESOURCES),
                                                    dependencies(SCOPE_TEST, READY))),
                                    phase(INTEGRATION_TEST)),
                            phase(INSTALL, after(PACKAGE)),
                            phase(DEPLOY, after(PACKAGE)))));
            // END SNIPPET: default
        }

        @Override
        @Nonnull
        public Collection<Phase> v3phases() {
            return List.of(phase(
                    ALL,
                    phase(INITIALIZE, phase(VALIDATE)),
                    phase(
                            BUILD,
                            phase(SOURCES),
                            phase(RESOURCES),
                            phase(COMPILE),
                            phase(READY),
                            phase(TEST_SOURCES),
                            phase(TEST_RESOURCES),
                            phase(TEST_COMPILE),
                            phase(TEST),
                            phase(UNIT_TEST),
                            phase(PACKAGE)),
                    phase(VERIFY, phase(INTEGRATION_TEST)),
                    phase(INSTALL),
                    phase(DEPLOY)));
        }

        @Override
        @Nonnull
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
    }

    static class SiteLifecycle implements Lifecycle {

        private static final String MAVEN_SITE_PLUGIN_VERSION = "3.21.0";
        private static final String MAVEN_SITE_PLUGIN =
                MAVEN_PLUGINS + "maven-site-plugin:" + MAVEN_SITE_PLUGIN_VERSION + ":";
        private static final String PHASE_SITE = "site";
        private static final String PHASE_SITE_DEPLOY = "site-deploy";

        @Override
        @Nonnull
        public String id() {
            return Lifecycle.SITE;
        }

        @Override
        @Nonnull
        public Collection<Phase> phases() {
            // START SNIPPET: site
            return List.of(
                    phase(PHASE_SITE, plugin(MAVEN_SITE_PLUGIN + "site", PHASE_SITE)),
                    phase(
                            PHASE_SITE_DEPLOY,
                            after(PHASE_SITE),
                            plugin(MAVEN_SITE_PLUGIN + "deploy", PHASE_SITE_DEPLOY)));
            // END SNIPPET: site
        }

        @Override
        @Nonnull
        public Collection<Alias> aliases() {
            return List.of(alias("pre-site", BEFORE + PHASE_SITE), alias("post-site", AFTER + PHASE_SITE));
        }
    }
}
