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
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.apache.maven.api.Lifecycle;
import org.apache.maven.api.model.Plugin;
import org.apache.maven.api.model.PluginExecution;

import static java.util.Arrays.asList;

public class Lifecycles {

    static Lifecycle.Phase phase(String name) {
        return new DefaultPhase(name, Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
    }

    static Lifecycle.Phase phase(String name, Lifecycle.Phase... phases) {
        return new DefaultPhase(name, Collections.emptyList(), Collections.emptyList(), asList(phases));
    }

    static Lifecycle.Phase phase(String name, Lifecycle.Link link, Lifecycle.Phase... phases) {
        return new DefaultPhase(name, Collections.emptyList(), Collections.singletonList(link), asList(phases));
    }

    static Lifecycle.Phase phase(String name, Plugin plugin) {
        return new DefaultPhase(
                name, Collections.singletonList(plugin), Collections.emptyList(), Collections.emptyList());
    }

    static Lifecycle.Phase phase(String name, Lifecycle.Link link, Plugin plugin) {
        return new DefaultPhase(
                name, Collections.singletonList(plugin), Collections.singletonList(link), Collections.emptyList());
    }

    static Lifecycle.Phase phase(String name, Lifecycle.Link link1, Lifecycle.Link link2, Lifecycle.Phase... phases) {
        return new DefaultPhase(name, Collections.emptyList(), asList(link1, link2), asList(phases));
    }

    static Lifecycle.Phase phase(
            String name, Lifecycle.Link link1, Lifecycle.Link link2, Lifecycle.Link link3, Lifecycle.Phase... phases) {
        return new DefaultPhase(name, Collections.emptyList(), asList(link1, link2, link3), asList(phases));
    }

    static Lifecycle.Phase phase(String name, Collection<Lifecycle.Link> links, Lifecycle.Phase... phases) {
        return new DefaultPhase(name, Collections.emptyList(), links, asList(phases));
    }

    static Plugin plugin(String coords, String phase) {
        String[] c = coords.split(":");
        return Plugin.newBuilder()
                .groupId(c[0])
                .artifactId(c[1])
                .version(c[2])
                .executions(Collections.singletonList(PluginExecution.newBuilder()
                        .id("default-" + c[3])
                        .phase(phase)
                        .goals(Collections.singletonList(c[3]))
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
        return new Lifecycle.Link() {
            @Override
            public Kind kind() {
                return Kind.AFTER;
            }

            @Override
            public Lifecycle.Pointer pointer() {
                return new Lifecycle.PhasePointer() {
                    @Override
                    public String phase() {
                        return phase;
                    }

                    @Override
                    public String toString() {
                        return "phase(" + phase + ")";
                    }
                };
            }

            @Override
            public String toString() {
                return "after(" + pointer() + ")";
            }
        };
    }

    /** Indicates the phase is after the phases for the dependencies in the given scope */
    static Lifecycle.Link dependencies(String scope, String phase) {
        return new Lifecycle.Link() {
            @Override
            public Kind kind() {
                return Kind.AFTER;
            }

            @Override
            public Lifecycle.Pointer pointer() {
                return new Lifecycle.DependenciesPointer() {
                    @Override
                    public String phase() {
                        return phase;
                    }

                    @Override
                    public String scope() {
                        return scope;
                    }

                    @Override
                    public String toString() {
                        return "dependencies(" + scope + ", " + phase + ")";
                    }
                };
            }

            @Override
            public String toString() {
                return "after(" + pointer() + ")";
            }
        };
    }

    static Lifecycle.Link children(String phase) {
        return new Lifecycle.Link() {
            @Override
            public Kind kind() {
                return Kind.AFTER;
            }

            @Override
            public Lifecycle.Pointer pointer() {
                return new Lifecycle.ChildrenPointer() {
                    @Override
                    public String phase() {
                        return phase;
                    }

                    @Override
                    public String toString() {
                        return "children(" + phase + ")";
                    }
                };
            }

            @Override
            public String toString() {
                return "after(" + pointer() + ")";
            }
        };
    }

    static Lifecycle.Alias alias(String v3Phase, String v4Phase) {
        return new DefaultAlias(v3Phase, v4Phase);
    }

    static class DefaultPhase implements Lifecycle.Phase {
        private final String name;
        private final List<Plugin> plugins;
        private final Collection<Lifecycle.Link> links;
        private final List<Lifecycle.Phase> phases;

        DefaultPhase(
                String name, List<Plugin> plugins, Collection<Lifecycle.Link> links, List<Lifecycle.Phase> phases) {
            this.name = name;
            this.plugins = plugins;
            this.links = links;
            this.phases = phases;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public List<Plugin> plugins() {
            return plugins;
        }

        @Override
        public Collection<Lifecycle.Link> links() {
            return links;
        }

        @Override
        public List<Lifecycle.Phase> phases() {
            return phases;
        }

        @Override
        public Stream<Lifecycle.Phase> allPhases() {
            return Stream.concat(Stream.of(this), phases().stream().flatMap(Lifecycle.Phase::allPhases));
        }
    }

    static class DefaultAlias implements Lifecycle.Alias {
        private final String v3Phase;
        private final String v4Phase;

        DefaultAlias(String v3Phase, String v4Phase) {
            this.v3Phase = v3Phase;
            this.v4Phase = v4Phase;
        }

        @Override
        public String v3Phase() {
            return v3Phase;
        }

        @Override
        public String v4Phase() {
            return v4Phase;
        }
    }
}
