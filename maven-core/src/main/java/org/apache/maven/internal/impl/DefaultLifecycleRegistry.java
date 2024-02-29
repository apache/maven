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

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.api.Lifecycle;
import org.apache.maven.api.services.LifecycleRegistry;
import org.apache.maven.api.spi.LifecycleProvider;

import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static java.util.Collections.emptyList;
import static org.apache.maven.internal.impl.Lifecycles.*;

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
        return values.values().iterator();
    }

    @Override
    public Stream<Lifecycle> stream() {
        return values.values().stream();
    }

    static <T> List<T> concat(List<T> l, T t) {
        List<T> nl = new ArrayList<>();
        nl.addAll(l);
        nl.add(t);
        return nl;
    }

    @Override
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
            if (link.pointer().type() == org.apache.maven.api.Lifecycle.Pointer.Type.Project) {
                if (link.kind() == org.apache.maven.api.Lifecycle.Link.Kind.After) {
                    graph.addEdge(graph.addVertex(link.pointer().phase()), ep0);
                } else {
                    graph.addEdge(ep3, graph.addVertex("$" + link.pointer().phase()));
                }
            }
        });
        phase.phases().forEach(child -> addPhase(graph, ep1, ep2, child));
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

                @Override
                public Collection<Alias> aliases() {
                    return Collections.emptyList();
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
                            after("pre-clean"),
                            plugin("org.apache.maven.plugins:maven-clean-plugin:" + MAVEN_CLEAN_PLUGIN_VERSION
                                    + ":clean")),
                    phase("post-clean", after("clean")));
        }

        @Override
        public Collection<Alias> aliases() {
            return emptyList();
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
                    phase("initialize", after("validate")),
                    phase("generate-sources", after("initialize")),
                    phase("process-sources", after("generate-sources")),
                    phase("generate-resources", after("process-sources")),
                    phase("process-resources", after("generate-resources")),
                    phase("compile", after("process-resources")),
                    phase("process-classes", after("compile")),
                    phase("generate-test-sources", after("process-classes")),
                    phase("process-test-sources", after("generate-test-sources")),
                    phase("generate-test-resources", after("process-test-sources")),
                    phase("process-test-resources", after("generate-test-resources")),
                    phase("test-compile", after("process-test-resources")),
                    phase("process-test-classes", after("test-compile")),
                    phase("test", after("process-test-classes")),
                    phase("prepare-package", after("test")),
                    phase("package", after("prepare-package")),
                    phase("pre-integration-test", after("package")),
                    phase("integration-test", after("pre-integration-test")),
                    phase("post-integration-test", after("integration-test")),
                    phase("verify", after("post-integration-test")),
                    phase("install", after("verify")),
                    phase("deploy", after("install")));
        }

        @Override
        public Collection<Alias> aliases() {
            return emptyList();
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
                            after("pre-site"),
                            plugin("org.apache.maven.plugins:maven-site-plugin:" + MAVEN_SITE_PLUGIN_VERSION
                                    + ":site")),
                    phase("post-site", after("site")),
                    phase(
                            "site-deploy",
                            after("post-site"),
                            plugin("org.apache.maven.plugins:maven-site-plugin:" + MAVEN_SITE_PLUGIN_VERSION
                                    + ":deploy")));
        }

        @Override
        public Collection<Alias> aliases() {
            return emptyList();
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
                    plugin("org.apache.maven.plugins:maven-wrapper-plugin:" + MAVEN_WRAPPER_PLUGIN_VERSION
                            + ":wrapper")));
        }

        @Override
        public Collection<Alias> aliases() {
            return emptyList();
        }
    }
}
