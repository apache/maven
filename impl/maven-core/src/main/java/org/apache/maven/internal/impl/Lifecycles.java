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

import java.util.Collection;
import java.util.List;
import java.util.Objects;

import org.apache.maven.api.Lifecycle;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.model.Plugin;
import org.apache.maven.api.model.PluginExecution;

public class Lifecycles {

    static Lifecycle.Phase phase(String name) {
        return new DefaultPhase(name, List.of(), List.of(), List.of());
    }

    static Lifecycle.Phase phase(String name, Lifecycle.Phase... phases) {
        return new DefaultPhase(name, List.of(), List.of(), List.of(phases));
    }

    static Lifecycle.Phase phase(String name, Lifecycle.Link link, Lifecycle.Phase... phases) {
        return new DefaultPhase(name, List.of(), List.of(link), List.of(phases));
    }

    static Lifecycle.Phase phase(String name, Plugin plugin) {
        return new DefaultPhase(name, List.of(plugin), List.of(), List.of());
    }

    static Lifecycle.Phase phase(String name, Lifecycle.Link link, Plugin plugin) {
        return new DefaultPhase(name, List.of(plugin), List.of(link), List.of());
    }

    static Lifecycle.Phase phase(String name, Lifecycle.Link link1, Lifecycle.Link link2, Lifecycle.Phase... phases) {
        return new DefaultPhase(name, List.of(), List.of(link1, link2), List.of(phases));
    }

    static Lifecycle.Phase phase(
            String name, Lifecycle.Link link1, Lifecycle.Link link2, Lifecycle.Link link3, Lifecycle.Phase... phases) {
        return new DefaultPhase(name, List.of(), List.of(link1, link2, link3), List.of(phases));
    }

    static Lifecycle.Phase phase(String name, Collection<Lifecycle.Link> links, Lifecycle.Phase... phases) {
        return new DefaultPhase(name, List.of(), links, List.of(phases));
    }

    static Plugin plugin(String coords, String phase) {
        String[] c = coords.split(":");
        return Plugin.newBuilder()
                .groupId(c[0])
                .artifactId(c[1])
                .version(c[2])
                .executions(List.of(PluginExecution.newBuilder()
                        .id("default-" + c[3])
                        .phase(phase)
                        .goals(List.of(c[3]))
                        .location("", DefaultLifecycleRegistry.DEFAULT_LIFECYCLE_INPUT_LOCATION)
                        .location("id", DefaultLifecycleRegistry.DEFAULT_LIFECYCLE_INPUT_LOCATION)
                        .location("phase", DefaultLifecycleRegistry.DEFAULT_LIFECYCLE_INPUT_LOCATION)
                        .location("goals", DefaultLifecycleRegistry.DEFAULT_LIFECYCLE_INPUT_LOCATION)
                        .build()))
                .location("", DefaultLifecycleRegistry.DEFAULT_LIFECYCLE_INPUT_LOCATION)
                .location("groupId", DefaultLifecycleRegistry.DEFAULT_LIFECYCLE_INPUT_LOCATION)
                .location("artifactId", DefaultLifecycleRegistry.DEFAULT_LIFECYCLE_INPUT_LOCATION)
                .location("version", DefaultLifecycleRegistry.DEFAULT_LIFECYCLE_INPUT_LOCATION)
                .build();
    }

    /** Indicates the phase is after the phases given in arguments */
    static Lifecycle.Link after(String phase) {
        return new DefaultLink(Lifecycle.Link.Kind.AFTER, new DefaultPhasePointer(phase));
    }

    /** Indicates the phase is after the phases for the dependencies in the given scope */
    static Lifecycle.Link dependencies(String scope, String phase) {
        return new DefaultLink(Lifecycle.Link.Kind.AFTER, new DefaultDependenciesPointer(phase, scope));
    }

    static Lifecycle.Link children(String phase) {
        return new DefaultLink(Lifecycle.Link.Kind.AFTER, new DefaultChildrenPointer(phase));
    }

    static Lifecycle.Alias alias(String v3Phase, String v4Phase) {
        return new DefaultAlias(v3Phase, v4Phase);
    }

    /**
     * Record implementation of Link.
     *
     * @param kind The kind of link (BEFORE or AFTER)
     * @param pointer The pointer to the target phase
     */
    record DefaultLink(Lifecycle.Link.Kind kind, Lifecycle.Pointer pointer) implements Lifecycle.Link {
        /**
         * Compact constructor with null validation.
         */
        DefaultLink {
            Objects.requireNonNull(kind, "kind cannot be null");
            Objects.requireNonNull(pointer, "pointer cannot be null");
        }

        @Override
        @Nonnull
        public String toString() {
            return kind.name().toLowerCase() + "(" + pointer + ")";
        }
    }

    /**
     * Record implementation of PhasePointer.
     *
     * @param phase The name of the target phase
     */
    record DefaultPhasePointer(String phase) implements Lifecycle.PhasePointer {
        /**
         * Compact constructor with null validation.
         */
        DefaultPhasePointer {
            Objects.requireNonNull(phase, "phase cannot be null");
        }

        @Override
        @Nonnull
        public String toString() {
            return "phase(" + phase + ")";
        }
    }

    /**
     * Record implementation of DependenciesPointer.
     *
     * @param phase The name of the target phase
     * @param scope The dependency scope
     */
    record DefaultDependenciesPointer(String phase, String scope) implements Lifecycle.DependenciesPointer {
        /**
         * Compact constructor with null validation.
         */
        DefaultDependenciesPointer {
            Objects.requireNonNull(phase, "phase cannot be null");
            Objects.requireNonNull(scope, "scope cannot be null");
        }

        @Override
        @Nonnull
        public String toString() {
            return "dependencies(" + scope + ", " + phase + ")";
        }
    }

    /**
     * Record implementation of ChildrenPointer.
     *
     * @param phase The name of the target phase
     */
    record DefaultChildrenPointer(String phase) implements Lifecycle.ChildrenPointer {
        /**
         * Compact constructor with null validation.
         */
        DefaultChildrenPointer {
            Objects.requireNonNull(phase, "phase cannot be null");
        }

        @Override
        @Nonnull
        public String toString() {
            return "children(" + phase + ")";
        }
    }

    /**
     * Record implementation of Lifecycle.Phase.
     *
     * @param name The name of the phase
     * @param plugins The list of plugins bound to this phase
     * @param links The collection of links from this phase to other phases
     * @param phases The list of sub-phases
     */
    record DefaultPhase(
            String name, List<Plugin> plugins, Collection<Lifecycle.Link> links, List<Lifecycle.Phase> phases)
            implements Lifecycle.Phase {

        /**
         * Canonical constructor with null validation and defensive copying.
         *
         * @param name The name of the phase
         * @param plugins The list of plugins bound to this phase
         * @param links The collection of links from this phase to other phases
         * @param phases The list of sub-phases
         */
        DefaultPhase(
                String name, List<Plugin> plugins, Collection<Lifecycle.Link> links, List<Lifecycle.Phase> phases) {
            this.name = Objects.requireNonNull(name, "name cannot be null");
            this.plugins = List.copyOf(Objects.requireNonNull(plugins, "plugins cannot be null"));
            this.links = List.copyOf(Objects.requireNonNull(links, "links cannot be null"));
            this.phases = List.copyOf(Objects.requireNonNull(phases, "phases cannot be null"));
        }
    }

    /**
     * Record implementation of Lifecycle.Alias.
     *
     * @param v3Phase The Maven 3 phase name
     * @param v4Phase The Maven 4 phase name
     */
    record DefaultAlias(String v3Phase, String v4Phase) implements Lifecycle.Alias {
        /**
         * Compact constructor with null validation.
         */
        DefaultAlias {
            Objects.requireNonNull(v3Phase, "v3Phase cannot be null");
            Objects.requireNonNull(v4Phase, "v4Phase cannot be null");
        }
    }
}
